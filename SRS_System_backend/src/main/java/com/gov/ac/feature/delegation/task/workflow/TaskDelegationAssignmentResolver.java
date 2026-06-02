package com.gov.ac.feature.delegation.task.workflow;

import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import com.gov.ac.feature.acting.service.ActingAssignmentService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowVariables;
import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import com.gov.ac.feature.delegation.task.service.TaskDelegationService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Bridges Camunda task assignment listeners with acting-manager coverage (Slice 4) and task
 * delegation (Slice 2).
 *
 * <p><b>Deterministic precedence</b>:
 *
 * <ol>
 *   <li><b>Direct assignee</b> — UUID from the workflow listener.
 *   <li><b>Acting manager</b> — matching {@code acting_assignment} with clearance check on the
 *       correspondence.
 *   <li><b>Task delegation</b> — evaluated against the assignee after step 2.
 *   <li><b>Escalation / SLA / audit reassignment</b> — separate schedulers; not part of this
 *       listener chain.
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDelegationAssignmentResolver {

  private final TaskDelegationService taskDelegationService;
  private final ActingAssignmentService actingAssignmentService;
  private final CorrespondenceRepository correspondenceRepository;
  private final TaskService taskService;

  public void resolveAndApply(DelegateTask task, String directAssigneeUserId) {
    if (task == null || directAssigneeUserId == null || directAssigneeUserId.isBlank()) {
      return;
    }
    UUID directId;
    try {
      directId = UUID.fromString(directAssigneeUserId.trim());
    } catch (IllegalArgumentException ex) {
      return;
    }

    task.setVariableLocal(
        CorrespondenceWorkflowVariables.WORKFLOW_DIRECT_ASSIGNEE_USER_ID, directId.toString());

    UUID correspondenceId = readCorrespondenceId(task);
    CorrespondenceEntity correspondence =
        correspondenceId != null
            ? correspondenceRepository.findById(correspondenceId).orElse(null)
            : null;

    String processKey = processDefinitionKey(task.getProcessDefinitionId());
    Long wfActionTypeId = readLongVariable(task, "wfWorkflowActionTypeId");

    tryApplyActingOverlay(task, directId, correspondence, processKey, wfActionTypeId);

    UUID delegatorForTaskDelegation;
    try {
      delegatorForTaskDelegation = UUID.fromString(task.getAssignee().trim());
    } catch (Exception ex) {
      return;
    }

    applyTaskDelegationOverlayOnDelegateTask(
        task, delegatorForTaskDelegation, correspondenceId, correspondence);
  }

  /** Re-applies task delegation using {@link TaskService} (reconciliation after acting expiry). */
  public void applyTaskDelegationOverlayWithTaskService(
      String taskId,
      UUID delegatorUserId,
      UUID correspondenceId,
      CorrespondenceEntity correspondence) {
    if (taskId == null || delegatorUserId == null) {
      return;
    }
    String typeCode =
        correspondence != null && correspondence.getCorrespondenceType() != null
            ? correspondence.getCorrespondenceType().getCode()
            : null;
    String confCode =
        correspondence != null && correspondence.getConfidentiality() != null
            ? correspondence.getConfidentiality().getCode()
            : null;
    Optional<TaskDelegationEntity> match;
    try {
      match =
          taskDelegationService.findEffectiveDelegationForTask(
              delegatorUserId, taskId, correspondenceId, typeCode, confCode);
    } catch (RuntimeException ex) {
      log.warn(
          "Task delegation lookup failed (taskService path) for task {} assignee {}: {}",
          taskId,
          delegatorUserId,
          ex.getMessage());
      return;
    }
    if (match.isEmpty()) {
      return;
    }
    TaskDelegationEntity delegation = match.get();
    UUID delegateId = delegation.getDelegateUser().getId();
    if (delegateId.equals(delegatorUserId)) {
      return;
    }
    taskService.setAssignee(taskId, delegateId.toString());
    taskService.setVariableLocal(
        taskId, CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID, delegatorUserId.toString());
    taskService.setVariableLocal(
        taskId, CorrespondenceWorkflowVariables.ACTING_DELEGATE_USER_ID, delegateId.toString());
    taskService.setVariableLocal(
        taskId, CorrespondenceWorkflowVariables.TASK_DELEGATION_ID, delegation.getId().toString());
    try {
      taskDelegationService.recordTaskRoutedToDelegate(delegation, taskId, correspondenceId);
    } catch (RuntimeException ex) {
      log.warn(
          "Failed to record TASK_ACTED_UNDER_DELEGATION audit for delegation {}: {}",
          delegation.getId(),
          ex.getMessage());
    }
  }

  private void tryApplyActingOverlay(
      DelegateTask task,
      UUID directAssigneeId,
      CorrespondenceEntity correspondence,
      String processDefinitionKey,
      Long workflowActionTypeId) {
    Optional<ActingAssignmentEntity> acting;
    try {
      acting =
          actingAssignmentService.findBestMatchForTask(
              directAssigneeId,
              correspondence,
              processDefinitionKey,
              task.getTaskDefinitionKey(),
              workflowActionTypeId);
    } catch (RuntimeException ex) {
      log.warn(
          "Acting assignment lookup failed for task {} assignee {}: {}",
          task.getId(),
          directAssigneeId,
          ex.getMessage());
      return;
    }
    if (acting.isEmpty()) {
      return;
    }
    ActingAssignmentEntity row = acting.get();
    UUID actingUserId = row.getActingUser().getId();
    if (actingUserId.equals(directAssigneeId)) {
      return;
    }
    if (correspondence != null
        && !actingAssignmentService.isActingClearedForCorrespondence(actingUserId, correspondence)) {
      log.warn(
          "[Acting] refused task {} — acting user {} not cleared for correspondence {}",
          task.getId(),
          actingUserId,
          correspondence.getId());
      return;
    }
    task.setAssignee(actingUserId.toString());
    task.setVariableLocal(CorrespondenceWorkflowVariables.ACTING_ASSIGNMENT_ID, row.getId().toString());
    task.setVariableLocal(
        CorrespondenceWorkflowVariables.ACTING_FOR_ABSENT_USER_ID, directAssigneeId.toString());
    task.setVariableLocal(CorrespondenceWorkflowVariables.ACTING_MANAGER_USER_ID, actingUserId.toString());
    log.info(
        "Task {} routed to acting manager {} for absent {} (actingAssignment {})",
        task.getId(),
        actingUserId,
        directAssigneeId,
        row.getId());
    try {
      actingAssignmentService.recordAssignmentUsed(row, task.getId(), readCorrespondenceId(task));
    } catch (RuntimeException ex) {
      log.warn(
          "Failed to record ACTING_ASSIGNMENT_USED audit for assignment {}: {}",
          row.getId(),
          ex.getMessage());
    }
  }

  private void applyTaskDelegationOverlayOnDelegateTask(
      DelegateTask task,
      UUID delegatorUserId,
      UUID correspondenceId,
      CorrespondenceEntity correspondence) {
    String correspondenceTypeCode =
        correspondence != null && correspondence.getCorrespondenceType() != null
            ? correspondence.getCorrespondenceType().getCode()
            : null;
    String confidentialityCode =
        correspondence != null && correspondence.getConfidentiality() != null
            ? correspondence.getConfidentiality().getCode()
            : null;

    Optional<TaskDelegationEntity> match;
    try {
      match =
          taskDelegationService.findEffectiveDelegationForTask(
              delegatorUserId,
              task.getId(),
              correspondenceId,
              correspondenceTypeCode,
              confidentialityCode);
    } catch (RuntimeException ex) {
      log.warn(
          "Task delegation lookup failed for task {} assignee {}: {}. Leaving assignee after acting overlay.",
          task.getId(),
          delegatorUserId,
          ex.getMessage());
      return;
    }
    if (match.isEmpty()) {
      return;
    }

    TaskDelegationEntity delegation = match.get();
    UUID delegateId = delegation.getDelegateUser().getId();
    if (delegateId.equals(delegatorUserId)) {
      return;
    }

    task.setAssignee(delegateId.toString());
    task.setVariableLocal(
        CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID, delegatorUserId.toString());
    task.setVariableLocal(
        CorrespondenceWorkflowVariables.ACTING_DELEGATE_USER_ID, delegateId.toString());
    task.setVariableLocal(
        CorrespondenceWorkflowVariables.TASK_DELEGATION_ID, delegation.getId().toString());

    log.info(
        "Task {} reassigned from {} to delegate {} (delegation {})",
        task.getId(),
        delegatorUserId,
        delegateId,
        delegation.getId());

    try {
      taskDelegationService.recordTaskRoutedToDelegate(delegation, task.getId(), correspondenceId);
    } catch (RuntimeException ex) {
      log.warn(
          "Failed to record TASK_ACTED_UNDER_DELEGATION audit for delegation {}: {}",
          delegation.getId(),
          ex.getMessage());
    }
  }

  private static UUID readCorrespondenceId(DelegateTask task) {
    Object raw = task.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw.toString().trim());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static Long readLongVariable(DelegateTask task, String name) {
    Object raw = task.getVariable(name);
    if (raw == null) {
      raw = task.getVariableLocal(name);
    }
    if (raw == null) {
      return null;
    }
    if (raw instanceof Long l) {
      return l;
    }
    if (raw instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(raw.toString().trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  static String processDefinitionKey(String processDefinitionId) {
    if (!StringUtils.hasText(processDefinitionId)) {
      return null;
    }
    int colon = processDefinitionId.indexOf(':');
    return colon > 0 ? processDefinitionId.substring(0, colon) : processDefinitionId.trim();
  }
}
