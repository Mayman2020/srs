package com.gov.ac.feature.workflow.execution.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowVariables;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.workflow.execution.dto.WorkflowTaskInboxRowDto;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Builds the workflow task inbox for the current user: active Camunda user tasks where the user
 * is the assignee, a candidate user, or a member of a candidate role group. Each row is enriched
 * with the originating correspondence so the UI can present an actionable list.
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskInboxService {

  private final TaskService taskService;
  private final RoleRepository roleRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final CorrespondenceRepository correspondenceRepository;

  /**
   * Active user tasks visible to the caller, ordered by creation time ascending. The result is
   * capped by the engine query but we do not paginate — front-end keeps it as a focused worklist.
   */
  @Transactional(readOnly = true)
  public List<WorkflowTaskInboxRowDto> listMyOpenTasks(UUID userId, int limit) {
    String uid = userId.toString();
    TaskQuery q =
        taskService
            .createTaskQuery()
            .or()
            .taskAssignee(uid)
            .taskCandidateUser(uid);
    for (String role : roleRepository.findActiveRoleCodesByUserId(userId)) {
      if (StringUtils.hasText(role)) {
        q = q.taskCandidateGroup(role);
      }
    }
    List<Task> tasks =
        q.endOr().active().orderByTaskCreateTime().asc().listPage(0, Math.max(1, Math.min(limit, 500)));
    if (tasks.isEmpty()) {
      return List.of();
    }

    // Resolve correspondences via workflow_instance.process_instance_id.
    Map<String, WorkflowInstanceEntity> byPi = new HashMap<>();
    for (Task t : tasks) {
      workflowInstanceRepository
          .findByProcessInstanceIdAndDeletedAtIsNull(t.getProcessInstanceId())
          .ifPresent(wi -> byPi.put(t.getProcessInstanceId(), wi));
    }
    Map<UUID, CorrespondenceEntity> byCorrId = new LinkedHashMap<>();
    for (WorkflowInstanceEntity wi : byPi.values()) {
      UUID cid = wi.getCorrespondence().getId();
      if (!byCorrId.containsKey(cid)) {
        correspondenceRepository.findById(cid).ifPresent(c -> byCorrId.put(cid, c));
      }
    }

    List<WorkflowTaskInboxRowDto> rows = new ArrayList<>(tasks.size());
    for (Task t : tasks) {
      WorkflowInstanceEntity wi = byPi.get(t.getProcessInstanceId());
      CorrespondenceEntity c = wi != null ? byCorrId.get(wi.getCorrespondence().getId()) : null;
      Instant created = t.getCreateTime() != null ? t.getCreateTime().toInstant() : null;
      Instant due = t.getDueDate() != null ? t.getDueDate().toInstant() : null;

      // Delegation overlay: local task variables written by the assignment listener tell us
      // whether this task is currently routed through a delegation. Both reads are best-effort —
      // missing variables simply mean "not a delegated task" and we report the canonical state.
      String originalAssignee = readLocal(t, CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID);
      String actingDelegate = readLocal(t, CorrespondenceWorkflowVariables.ACTING_DELEGATE_USER_ID);
      String delegationId = readLocal(t, CorrespondenceWorkflowVariables.TASK_DELEGATION_ID);
      boolean actingAsDelegate =
          StringUtils.hasText(actingDelegate) && actingDelegate.equals(uid);

      String workflowDirect = readLocal(t, CorrespondenceWorkflowVariables.WORKFLOW_DIRECT_ASSIGNEE_USER_ID);
      String actingForAbsent = readLocal(t, CorrespondenceWorkflowVariables.ACTING_FOR_ABSENT_USER_ID);
      String actingAssignmentId = readLocal(t, CorrespondenceWorkflowVariables.ACTING_ASSIGNMENT_ID);
      boolean actingAsManager =
          StringUtils.hasText(actingAssignmentId)
              && uid.equals(t.getAssignee())
              && !actingAsDelegate;

      rows.add(
          new WorkflowTaskInboxRowDto(
              t.getId(),
              t.getName(),
              t.getTaskDefinitionKey(),
              t.getAssignee(),
              t.getProcessInstanceId(),
              created,
              due,
              c != null ? c.getId() : null,
              c != null ? c.getReferenceNumber() : null,
              c != null ? c.getSubject() : null,
              c != null && c.getCorrespondenceType() != null ? c.getCorrespondenceType().getCode() : null,
              c != null && c.getCorrespondenceStatus() != null ? c.getCorrespondenceStatus().getCode() : null,
              c != null && c.getPriority() != null ? c.getPriority().getCode() : null,
              wi != null ? wi.getCurrentLevelCode() : null,
              wi != null ? wi.getCurrentDepartmentId() : null,
              originalAssignee,
              actingAsDelegate,
              delegationId,
              workflowDirect,
              actingForAbsent,
              actingAssignmentId,
              actingAsManager));
    }
    return rows;
  }

  private String readLocal(Task task, String name) {
    Object value = taskService.getVariableLocal(task.getId(), name);
    return value == null ? null : value.toString();
  }
}
