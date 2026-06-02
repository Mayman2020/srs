package com.gov.ac.feature.acting.job;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowVariables;
import com.gov.ac.feature.delegation.task.workflow.TaskDelegationAssignmentResolver;
import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import com.gov.ac.feature.acting.repository.ActingAssignmentRepository;
import java.time.LocalDate;
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
import org.springframework.util.StringUtils;

/**
 * Heals open Camunda tasks that still carry {@code actingAssignmentId} locals after the underlying
 * {@code acting_assignment} row was revoked or fell outside its validity window. Resets the
 * assignee to the absent user, clears acting locals, and re-applies task delegation (if any) via
 * {@link TaskDelegationAssignmentResolver#applyTaskDelegationOverlayWithTaskService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActingAssignmentReconciliationJob {

  private final TaskService taskService;
  private final ActingAssignmentRepository actingAssignmentRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final TaskDelegationAssignmentResolver taskDelegationAssignmentResolver;

  @Value("${ac.acting-assignment.reconciliation-max-tasks:150}")
  private int maxTasksPerRun;

  @Scheduled(fixedDelayString = "${ac.acting-assignment.reconciliation-poll-ms:180000}")
  public void run() {
    LocalDate today = LocalDate.now();
    List<Task> tasks =
        taskService.createTaskQuery().active().orderByTaskCreateTime().asc().listPage(0, maxTasksPerRun);
    int healed = 0;
    for (Task t : tasks) {
      String aidStr = readLocal(t.getId(), CorrespondenceWorkflowVariables.ACTING_ASSIGNMENT_ID);
      if (!StringUtils.hasText(aidStr)) {
        continue;
      }
      UUID aid;
      try {
        aid = UUID.fromString(aidStr.trim());
      } catch (IllegalArgumentException ex) {
        continue;
      }
      Optional<ActingAssignmentEntity> rowOpt = actingAssignmentRepository.findById(aid);
      ActingAssignmentEntity row = rowOpt.orElse(null);
      if (row != null && row.getRevokedAt() == null && row.isActiveOn(today)) {
        continue;
      }
      String absentStr = readLocal(t.getId(), CorrespondenceWorkflowVariables.ACTING_FOR_ABSENT_USER_ID);
      if (!StringUtils.hasText(absentStr)) {
        continue;
      }
      UUID absentId;
      try {
        absentId = UUID.fromString(absentStr.trim());
      } catch (IllegalArgumentException ex) {
        continue;
      }
      UUID correspondenceId = readCorrespondenceId(t.getId());
      CorrespondenceEntity correspondence =
          correspondenceId != null
              ? correspondenceRepository.findById(correspondenceId).orElse(null)
              : null;

      taskService.setAssignee(t.getId(), absentId.toString());
      removeLocal(t.getId(), CorrespondenceWorkflowVariables.ACTING_ASSIGNMENT_ID);
      removeLocal(t.getId(), CorrespondenceWorkflowVariables.ACTING_FOR_ABSENT_USER_ID);
      removeLocal(t.getId(), CorrespondenceWorkflowVariables.ACTING_MANAGER_USER_ID);

      taskDelegationAssignmentResolver.applyTaskDelegationOverlayWithTaskService(
          t.getId(), absentId, correspondenceId, correspondence);
      healed++;
    }
    if (healed > 0) {
      log.info("ActingAssignmentReconciliationJob healed {} open task(s)", healed);
    }
  }

  private String readLocal(String taskId, String name) {
    Object v = taskService.getVariableLocal(taskId, name);
    return v == null ? null : v.toString();
  }

  private void removeLocal(String taskId, String name) {
    try {
      taskService.removeVariableLocal(taskId, name);
    } catch (RuntimeException ex) {
      log.debug("removeVariableLocal {} on task {}: {}", name, taskId, ex.getMessage());
    }
  }

  private UUID readCorrespondenceId(String taskId) {
    Object raw = taskService.getVariable(taskId, CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw.toString().trim());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
