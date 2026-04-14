package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persists {@code workflow_history} and updates {@link CorrespondenceEntity#getCorrespondenceStatus()}
 * when a Camunda user task completes. Expects process variables {@code correspondenceId}, optional
 * {@code wfDecision} (must match {@code workflow_action_type.code}), and optional {@code
 * actionComment}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceWorkflowTaskPersistenceService {

  public static final String VAR_CORRESPONDENCE_ID = "correspondenceId";
  public static final String VAR_WF_DECISION = "wfDecision";
  public static final String VAR_ACTION_COMMENT = "actionComment";

  private static final String EVENT_TASK_COMPLETED = "TASK_COMPLETED";

  private final CorrespondenceRepository correspondenceRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;
  private final LookupResolutionService lookups;
  private final AppUserRepository appUserRepository;

  @Transactional
  public void recordUserTaskCompleted(DelegateTask task) {
    String corrIdStr = (String) task.getVariable(VAR_CORRESPONDENCE_ID);
    if (!StringUtils.hasText(corrIdStr)) {
      log.warn("Workflow persistence skipped: missing correspondenceId on task {}", task.getId());
      return;
    }
    UUID correspondenceId;
    try {
      correspondenceId = UUID.fromString(corrIdStr.trim());
    } catch (IllegalArgumentException ex) {
      log.warn("Workflow persistence skipped: invalid correspondenceId={}", corrIdStr);
      return;
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findById(correspondenceId)
            .filter(c -> c.getDeletedAt() == null)
            .orElse(null);
    if (correspondence == null) {
      log.warn("Workflow persistence skipped: correspondence not found id={}", correspondenceId);
      return;
    }

    String decisionRaw = (String) task.getVariable(VAR_WF_DECISION);
    if (!StringUtils.hasText(decisionRaw)) {
      log.warn(
          "Workflow persistence skipped: missing {} on task {}",
          VAR_WF_DECISION,
          task.getId());
      return;
    }
    String decision = decisionRaw.trim().toUpperCase();

    String comment = (String) task.getVariable(VAR_ACTION_COMMENT);
    String commentTrimmed = StringUtils.hasText(comment) ? comment.trim() : null;

    CorrespondenceStatusEntity previous = correspondence.getCorrespondenceStatus();
    if (previous == null) {
      log.warn("Workflow persistence skipped: correspondence has no status id={}", correspondenceId);
      return;
    }

    WorkflowActionTypeEntity rule;
    try {
      rule = lookups.requireWorkflowActionForTransition(decision, previous.getId());
    } catch (Exception ex) {
      log.warn(
          "Workflow persistence: no DB rule for wfDecision={} fromStatusId={}: {}",
          decision,
          previous.getId(),
          ex.getMessage());
      return;
    }

    CorrespondenceStatusEntity newStatus = rule.getNextCorrespondenceStatus();
    if (newStatus != null && !newStatus.getId().equals(previous.getId())) {
      correspondence.setCorrespondenceStatus(newStatus);
    }

    UUID actorId = null;
    if (StringUtils.hasText(task.getAssignee())) {
      try {
        actorId = UUID.fromString(task.getAssignee().trim());
      } catch (IllegalArgumentException ignored) {
        // non-UUID assignee
      }
    }
    if (actorId != null) {
      correspondence.setUpdatedBy(actorId);
    }
    correspondenceRepository.save(correspondence);

    AppUserEntity actor =
        actorId != null
            ? appUserRepository.findByIdAndDeletedAtIsNull(actorId).orElse(null)
            : null;

    WorkflowHistoryEventTypeEntity eventType = lookups.requireActiveHistoryEventType(EVENT_TASK_COMPLETED);

    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;
    Instant now = Instant.now();

    WorkflowHistoryEntity history = new WorkflowHistoryEntity();
    history.setCorrespondence(correspondence);
    history.setEventType(eventType);
    history.setWorkflowActionType(rule);
    history.setActor(actor);
    history.setOccurredAt(now);
    history.setSequenceNo(nextSeq);
    history.setPrimaryCommentText(commentTrimmed);
    history.setPreviousCorrespondenceStatus(previous);
    history.setNewCorrespondenceStatus(correspondence.getCorrespondenceStatus());
    history.setPriorityAtEvent(correspondence.getPriority());
    history.setCamundaTaskId(task.getId());
    history.setCamundaActivityId(task.getTaskDefinitionKey());
    Map<String, Object> detail = new HashMap<>();
    detail.put("taskDefinitionKey", task.getTaskDefinitionKey());
    detail.put("taskName", task.getName());
    detail.put("wfDecision", decision);
    history.setDetail(detail);
    if (actorId != null) {
      history.setCreatedBy(actorId);
      history.setUpdatedBy(actorId);
    }
    workflowHistoryRepository.save(history);

    log.info(
        "Recorded workflow task completion correspondenceId={} taskId={} decision={} seq={}",
        correspondenceId,
        task.getId(),
        decision,
        nextSeq);
  }
}
