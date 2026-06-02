package com.gov.ac.feature.workflow.execution.job;

import com.gov.ac.feature.audit.entity.AuditEventEntity;
import com.gov.ac.feature.audit.repository.AuditEventRepository;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flags long-running stale Camunda user tasks. For unassigned tasks, records an audit event so
 * supervisors can intervene. For assigned tasks whose assignee has an active authority
 * delegation in place, automatically reassigns to the delegate, increments the
 * {@code workflow_instance.escalation_count}, and audits the change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEscalationScheduler {

  private static final String METRIC_SLA_BREACH = "correspondence_sla_breach_total";

  private final TaskService taskService;
  private final WorkflowService workflowService;
  private final AuditEventRepository auditEventRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final AuthorityDelegationRepository authorityDelegationRepository;
  private final MeterRegistry meterRegistry;

  @Value("${ac.workflow.escalation.unassigned-after-minutes:120}")
  private int unassignedAfterMinutes;

  @Scheduled(fixedDelayString = "${ac.workflow.escalation.poll-ms:120000}")
  @Transactional
  public void scanStaleTasks() {
    Instant cutoff = Instant.now().minus(unassignedAfterMinutes, ChronoUnit.MINUTES);
    Date before = Date.from(cutoff);
    scanStaleUnassigned(before);
    scanStaleAssigned(before);
  }

  private void scanStaleUnassigned(Date before) {
    List<Task> tasks =
        taskService.createTaskQuery().taskUnassigned().taskCreatedBefore(before).active().list();
    for (Task t : tasks) {
      String pi = t.getProcessInstanceId();
      Boolean sent =
          (Boolean) workflowService.getProcessVariable(pi, "escalationAuditLogged").orElse(null);
      if (Boolean.TRUE.equals(sent)) {
        continue;
      }
      auditStaleTask(t, pi, "WF_TASK_STALE_UNASSIGNED");
      workflowService.setProcessVariable(pi, "escalationAuditLogged", true);
      bumpEscalationCount(pi);
      slaBreachCounter("unassigned").increment();
    }
  }

  private void scanStaleAssigned(Date before) {
    List<Task> tasks =
        taskService.createTaskQuery().taskAssigned().taskCreatedBefore(before).active().list();
    for (Task t : tasks) {
      String pi = t.getProcessInstanceId();
      Boolean reassigned =
          (Boolean) workflowService.getProcessVariable(pi, "escalationDelegateReassigned").orElse(null);
      if (Boolean.TRUE.equals(reassigned)) {
        continue;
      }
      Optional<UUID> assigneeId = parseUuid(t.getAssignee());
      if (assigneeId.isEmpty()) {
        continue;
      }
      AuthorityDelegationEntity delegation =
          authorityDelegationRepository
              .findFirstActiveByDelegator(assigneeId.get(), Instant.now())
              .orElse(null);
      if (delegation == null || delegation.getDelegateUser() == null) {
        continue;
      }
      UUID delegateId = delegation.getDelegateUser().getId();
      taskService.setAssignee(t.getId(), delegateId.toString());
      workflowService.setProcessVariable(pi, "escalationDelegateReassigned", true);
      bumpEscalationCount(pi);
      slaBreachCounter("reassigned_to_delegate").increment();
      auditStaleTask(t, pi, "WF_TASK_REASSIGNED_TO_DELEGATE");
      log.info(
          "Workflow escalation: reassigned task {} from {} to delegate {}",
          t.getId(),
          assigneeId.get(),
          delegateId);
    }
  }

  private void auditStaleTask(Task t, String processInstanceId, String actionCode) {
    String businessKey = workflowService.findBusinessKey(processInstanceId).orElse(null);
    AuditEventEntity e = new AuditEventEntity();
    e.setActorUserId("SYSTEM");
    e.setActionCode(actionCode);
    e.setResourceType("CAMUNDA_TASK");
    e.setResourceId(t.getId());
    e.setDetailJson(
        "{\"processInstanceId\":\""
            + processInstanceId
            + "\",\"taskName\":\""
            + escape(t.getName())
            + "\",\"businessKey\":\""
            + escape(businessKey)
            + "\"}");
    auditEventRepository.save(e);
    log.warn(
        "Workflow escalation [{}]: taskId={} processInstanceId={} businessKey={}",
        actionCode,
        t.getId(),
        processInstanceId,
        businessKey);
  }

  private void bumpEscalationCount(String processInstanceId) {
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(processInstanceId)
        .ifPresent(
            (WorkflowInstanceEntity wi) -> {
              int current = wi.getEscalationCount() == null ? 0 : wi.getEscalationCount();
              wi.setEscalationCount(current + 1);
              workflowInstanceRepository.save(wi);
            });
  }

  private Counter slaBreachCounter(String outcome) {
    return Counter.builder(METRIC_SLA_BREACH)
        .description("Correspondence workflow SLA breaches detected by the escalation scheduler")
        .tag("outcome", outcome)
        .register(meterRegistry);
  }

  private static Optional<UUID> parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value.trim()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
