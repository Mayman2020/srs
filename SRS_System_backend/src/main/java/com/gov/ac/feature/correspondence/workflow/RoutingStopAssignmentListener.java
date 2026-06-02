package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.delegation.task.workflow.TaskDelegationAssignmentResolver;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * On user-task {@code create} inside the multi-instance routing subprocess: reads the current
 * {@code routingStop} map and assigns the task to a user holding the stop's {@code roleCode}
 * within the stop's {@code departmentId}, falling back to a department-scoped candidate group
 * if no specific user exists.
 *
 * <p>Bean name {@code routingStopAssignmentListener} so BPMN can reference it as
 * {@code camunda:delegateExpression="${routingStopAssignmentListener}"}.
 */
@Component("routingStopAssignmentListener")
@RequiredArgsConstructor
@Slf4j
public class RoutingStopAssignmentListener implements TaskListener {

  private final UserRoleRepository userRoleRepository;
  private final AppUserRepository appUserRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final TaskDelegationAssignmentResolver taskDelegationAssignmentResolver;

  @Override
  public void notify(DelegateTask task) {
    if (!TaskListener.EVENTNAME_CREATE.equals(task.getEventName())) {
      return;
    }

    Object stopRaw = task.getVariable(CorrespondenceWorkflowVariables.ROUTING_STOP);
    if (!(stopRaw instanceof Map<?, ?> stopMap)) {
      log.debug(
          "routingStopAssignmentListener: no routingStop on task {}; leaving assignment to fallback listener",
          task.getId());
      return;
    }

    Long departmentId = readLong(stopMap.get("departmentId"));
    String roleCode = readString(stopMap.get("roleCode"));
    String levelCode = readString(stopMap.get("levelCode"));

    if (departmentId == null) {
      log.warn("routingStopAssignmentListener: stop missing departmentId on task {}", task.getId());
      return;
    }

    // Update workflow_instance pointer so reports/dashboard see live progress.
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(task.getProcessInstanceId())
        .ifPresent(
            instance -> {
              instance.setCurrentDepartmentId(departmentId);
              if (StringUtils.hasText(levelCode)) {
                instance.setCurrentLevelCode(levelCode);
              }
              workflowInstanceRepository.save(instance);
            });

    // Try to resolve a specific user with the requested role in this department.
    AppUserEntity assignee = null;
    if (StringUtils.hasText(roleCode)) {
      assignee = pickUserByRoleAndDepartment(roleCode, departmentId);
    }
    if (assignee == null) {
      // Fall back to any active user in the department.
      assignee = pickAnyActiveUserInDepartment(departmentId);
    }

    if (assignee != null) {
      String uid = assignee.getId().toString();
      task.setAssignee(uid);
      log.debug(
          "routingStopAssignmentListener: task {} -> user {} (dept {} role {} level {})",
          task.getId(),
          assignee.getId(),
          departmentId,
          roleCode,
          levelCode);
      taskDelegationAssignmentResolver.resolveAndApply(task, uid);
      return;
    }

    // Last resort: department-scoped candidate group so anyone in the dept can claim.
    String groupCode = "DEPT_" + departmentId;
    task.setAssignee(null);
    task.addCandidateGroup(groupCode);
    log.warn(
        "routingStopAssignmentListener: no concrete assignee for task {}; left as candidate group {}",
        task.getId(),
        groupCode);
  }

  private AppUserEntity pickUserByRoleAndDepartment(String roleCode, Long departmentId) {
    return userRoleRepository
        .findActiveUserIdsByRoleCodeAndDepartmentId(roleCode, departmentId)
        .stream()
        .findFirst()
        .flatMap(appUserRepository::findByIdAndDeletedAtIsNull)
        .orElse(null);
  }

  private AppUserEntity pickAnyActiveUserInDepartment(Long departmentId) {
    return appUserRepository
        .findFirstActiveByDepartmentId(departmentId)
        .orElse(null);
  }

  private static Long readLong(Object raw) {
    if (raw == null) {
      return null;
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

  private static String readString(Object raw) {
    return raw == null ? null : raw.toString();
  }
}
