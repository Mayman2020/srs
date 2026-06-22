package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Records SLA boundary timer firings on routing-stop user tasks in workflow_history. */
@Component("workflowTimerEscalationDelegate")
@RequiredArgsConstructor
@Slf4j
public class WorkflowTimerEscalationDelegate implements JavaDelegate {

  private static final String EVENT_SLA_BREACH = "SLA_BREACH";

  private final CorrespondenceRepository correspondenceRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final LookupResolutionService lookups;

  @Override
  public void execute(DelegateExecution execution) {
    Object stopRaw = execution.getVariable(CorrespondenceWorkflowVariables.ROUTING_STOP);
    String level =
        stopRaw instanceof Map<?, ?> m ? String.valueOf(m.get("levelCode")) : "?";
    UUID correspondenceId = readCorrespondenceId(execution);
    log.warn(
        "SLA timer escalation: processInstanceId={} correspondenceId={} level={}",
        execution.getProcessInstanceId(),
        correspondenceId,
        level);
    execution.setVariable("slaEscalated", Boolean.TRUE);

    if (correspondenceId == null) {
      return;
    }

    correspondenceRepository
        .findById(correspondenceId)
        .filter(c -> c.getDeletedAt() == null)
        .ifPresent(c -> appendSlaHistory(execution, c, level, stopRaw));
  }

  private void appendSlaHistory(
      DelegateExecution execution,
      CorrespondenceEntity correspondence,
      String level,
      Object stopRaw) {
    WorkflowHistoryEventTypeEntity eventType = lookups.requireActiveHistoryEventType(EVENT_SLA_BREACH);
    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;

    WorkflowHistoryEntity history = new WorkflowHistoryEntity();
    history.setCorrespondence(correspondence);
    history.setEventType(eventType);
    history.setOccurredAt(Instant.now());
    history.setSequenceNo(nextSeq);
    history.setPrimaryCommentText("SLA timer fired at routing level " + level);
    history.setPreviousCorrespondenceStatus(correspondence.getCorrespondenceStatus());
    history.setNewCorrespondenceStatus(correspondence.getCorrespondenceStatus());
    history.setPriorityAtEvent(correspondence.getPriority());
    history.setCamundaActivityId(execution.getCurrentActivityId());

    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(execution.getProcessInstanceId())
        .ifPresent(history::setWorkflowInstance);

    Map<String, Object> detail = new HashMap<>();
    detail.put("levelCode", level);
    detail.put("slaEscalated", true);
    if (stopRaw instanceof Map<?, ?> stop) {
      detail.put("routingStop", stop);
    }
    history.setDetail(detail);
    workflowHistoryRepository.save(history);
  }

  private static UUID readCorrespondenceId(DelegateExecution execution) {
    Object raw = execution.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw.toString().trim());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
