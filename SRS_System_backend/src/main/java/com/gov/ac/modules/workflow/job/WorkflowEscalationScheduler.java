package com.gov.ac.modules.workflow.job;

import com.gov.ac.domain.audit.AuditEvent;
import com.gov.ac.persistence.AuditEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flags long-running unassigned Camunda user tasks and records {@link AuditEvent} rows (integrate
 * email/SMS via {@link com.gov.ac.modules.notification} as a follow-up).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEscalationScheduler {

  private final TaskService taskService;
  private final RuntimeService runtimeService;
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
      Boolean sent = (Boolean) runtimeService.getVariable(pi, "escalationAuditLogged");
      if (Boolean.TRUE.equals(sent)) {
        continue;
      }
      ProcessInstance pinst =
          runtimeService.createProcessInstanceQuery().processInstanceId(pi).singleResult();
      String businessKey = pinst != null ? pinst.getBusinessKey() : null;
      AuditEvent e = new AuditEvent();
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
      runtimeService.setVariable(pi, "escalationAuditLogged", true);
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
