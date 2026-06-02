package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowActionEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowActionRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
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
  private final WorkflowActionRepository workflowActionRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
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

    // Preserve the delegation chain (original assignee / acting delegate) on the immutable
    // workflow_history row so audit consumers can clearly distinguish who held the task vs who
    // actually completed it. Slice 2 — Task Delegation.
    String originalAssignee =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID);
    String actingDelegate =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.ACTING_DELEGATE_USER_ID);
    String taskDelegationId =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.TASK_DELEGATION_ID);
    if (StringUtils.hasText(originalAssignee)) {
      detail.put("originalAssigneeUserId", originalAssignee);
    }
    if (StringUtils.hasText(actingDelegate)) {
      detail.put("actingDelegateUserId", actingDelegate);
    }
    if (StringUtils.hasText(taskDelegationId)) {
      detail.put("taskDelegationId", taskDelegationId);
    }

    String workflowDirect =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.WORKFLOW_DIRECT_ASSIGNEE_USER_ID);
    String actingAssignmentId =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.ACTING_ASSIGNMENT_ID);
    String actingForAbsent =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.ACTING_FOR_ABSENT_USER_ID);
    String actingManagerUserId =
        (String) task.getVariableLocal(CorrespondenceWorkflowVariables.ACTING_MANAGER_USER_ID);
    if (StringUtils.hasText(workflowDirect)) {
      detail.put("workflowDirectAssigneeUserId", workflowDirect);
    }
    if (StringUtils.hasText(actingAssignmentId)) {
      detail.put("actingAssignmentId", actingAssignmentId);
    }
    if (StringUtils.hasText(actingForAbsent)) {
      detail.put("actingForAbsentUserId", actingForAbsent);
    }
    if (StringUtils.hasText(actingManagerUserId)) {
      detail.put("actingManagerUserId", actingManagerUserId);
    }
    if (StringUtils.hasText(task.getAssignee())) {
      detail.put("effectiveActorUserId", task.getAssignee().trim());
    }

    history.setDetail(detail);
    if (actorId != null) {
      history.setCreatedBy(actorId);
      history.setUpdatedBy(actorId);
    }
    workflowHistoryRepository.save(history);

    // Immutable workflow_action row for the timeline (was previously skipped — orphan table).
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(task.getProcessInstanceId())
        .ifPresentOrElse(
            instance -> persistWorkflowAction(instance, correspondence, rule, actor, commentTrimmed, task, decision),
            () ->
                log.warn(
                    "workflow_action skipped: no workflow_instance for processInstanceId={} correspondenceId={}",
                    task.getProcessInstanceId(),
                    correspondenceId));

    log.info(
        "Recorded workflow task completion correspondenceId={} taskId={} decision={} seq={}",
        correspondenceId,
        task.getId(),
        decision,
        nextSeq);
  }

  /** Persist the immutable workflow_action row that the timeline UI reads. */
  private void persistWorkflowAction(
      WorkflowInstanceEntity instance,
      CorrespondenceEntity correspondence,
      WorkflowActionTypeEntity rule,
      AppUserEntity actor,
      String commentTrimmed,
      DelegateTask task,
      String decision) {
    WorkflowActionEntity action = new WorkflowActionEntity();
    action.setWorkflowInstance(instance);
    action.setCorrespondence(correspondence);
    action.setActionType(rule);
    action.setActor(actor);
    action.setCommentText(commentTrimmed);
    action.setCamundaTaskId(task.getId());
    action.setCamundaActivityId(task.getTaskDefinitionKey());
    action.setPayload(toJsonPayload(task, decision));
    if (actor != null) {
      action.setCreatedBy(actor.getId());
      action.setUpdatedBy(actor.getId());
    }
    workflowActionRepository.save(action);
  }

  private static String toJsonPayload(DelegateTask task, String decision) {
    String safeName = task.getName() == null ? "" : task.getName().replace("\"", "\\\"");
    String key = task.getTaskDefinitionKey() == null ? "" : task.getTaskDefinitionKey();
    return "{\"taskDefinitionKey\":\"" + key + "\",\"taskName\":\""
        + safeName + "\",\"wfDecision\":\"" + decision + "\"}";
  }
}
