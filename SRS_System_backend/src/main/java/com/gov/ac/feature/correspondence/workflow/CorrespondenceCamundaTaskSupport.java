package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Locates active Camunda user tasks for a correspondence business key (reference number) and
 * viewer, matching assignee / candidate user / candidate group rules.
 */
@Component
@RequiredArgsConstructor
public class CorrespondenceCamundaTaskSupport {

  private final TaskService taskService;
  private final RoleRepository roleRepository;
  private final AppUserRepository appUserRepository;

  public List<Task> findActiveTasksForUser(String businessKey, UUID viewerId) {
    String userId = viewerId.toString();
    var q =
        taskService
            .createTaskQuery()
            .processInstanceBusinessKey(businessKey)
            .or()
            .taskAssignee(userId)
            .taskCandidateUser(userId);
    for (String role : roleRepository.findActiveRoleCodesByUserId(viewerId)) {
      if (StringUtils.hasText(role)) {
        q = q.taskCandidateGroup(role);
      }
    }
    String deptGroup =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .map(u -> u.getDepartment())
            .filter(dept -> dept != null && dept.getId() != null)
            .map(dept -> "DEPT_" + dept.getId())
            .orElse(null);
    if (StringUtils.hasText(deptGroup)) {
      q = q.taskCandidateGroup(deptGroup);
    }
    return q.endOr().active().orderByTaskCreateTime().asc().list();
  }
}
