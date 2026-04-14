package com.gov.ac.feature.workflow.execution.job;

import com.gov.ac.feature.audit.entity.AuditEventEntity;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import com.gov.ac.feature.audit.repository.AuditEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flags long-running unassigned Camunda user tasks and records {@link AuditEventEntity} rows (integrate
 * email/SMS via {@link com.gov.ac.feature.notification.dispatch} as a follow-up).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEscalationScheduler {

  private final TaskService taskService;
  private final WorkflowService workflowService;
  private final AuditEventRepository auditEventRepository;

  @Value("${ac.workflow.escalation.unassigned-after-minutes:120}")
  private int unassignedAfterMinutes;

  @Scheduled(fixedDelayString = "${ac.workflow.escalation.poll-ms:120000}")
  @Transactional
  public void scanStaleUnassignedTasks() {
    Instant cutoff = Instant.now().minus(unassignedAfterMinutes, ChronoUnit.MINUTES);
    Date before = Date.from(cutoff);
    List<Task> tasks =
        taskService.createTaskQuery().taskUnassigned().taskCreatedBefore(before).active().list();
    for (Task t : tasks) {
      String pi = t.getProcessInstanceId();
      Boolean sent =
          (Boolean)
              workflowService.getProcessVariable(pi, "escalationAuditLogged").orElse(null);
      if (Boolean.TRUE.equals(sent)) {
        continue;
      }
      String businessKey = workflowService.findBusinessKey(pi).orElse(null);
      AuditEventEntity e = new AuditEventEntity();
      e.setActorUserId("SYSTEM");
      e.setActionCode("WF_TASK_STALE_UNASSIGNED");
      e.setResourceType("CAMUNDA_TASK");
      e.setResourceId(t.getId());
      e.setDetailJson(
          "{\"processInstanceId\":\""
              + pi
              + "\",\"taskName\":\""
              + escape(t.getName())
              + "\",\"businessKey\":\""
              + escape(businessKey)
              + "\"}");
      auditEventRepository.save(e);
      workflowService.setProcessVariable(pi, "escalationAuditLogged", true);
      log.warn(
          "Workflow escalation audit: taskId={} processInstanceId={} businessKey={}",
          t.getId(),
          pi,
          businessKey);
    }
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
