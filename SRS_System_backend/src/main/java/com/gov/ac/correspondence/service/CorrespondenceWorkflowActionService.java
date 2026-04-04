package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.correspondence.workflow.CorrespondenceWorkflowTaskPersistenceService;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceWorkflowActionService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final TaskService taskService;

  @Transactional
  public void completeActiveAssigneeTask(
      UUID correspondenceId, UUID viewerId, String action, String comment) {
    AppUser viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(() -> new ForbiddenException("You cannot perform this action"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot perform this action");
    }

    Correspondence correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Correspondence not found");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    String businessKey = correspondence.getReferenceNumber();
    if (!StringUtils.hasText(businessKey)) {
      throw new BadRequestException("Correspondence has no reference number");
    }

    String assignee = viewerId.toString();
    List<Task> tasks =
        taskService
            .createTaskQuery()
            .processInstanceBusinessKey(businessKey)
            .taskAssignee(assignee)
            .active()
            .orderByTaskCreateTime()
            .asc()
            .list();

    if (tasks.isEmpty()) {
      throw new BadRequestException("No active workflow task assigned to you for this correspondence");
    }

    String decision =
        StringUtils.hasText(action) ? action.trim().toUpperCase() : "APPROVE";
    if (!decision.equals("APPROVE")
        && !decision.equals("REJECT")
        && !decision.equals("RETURN")) {
      throw new BadRequestException("Invalid workflow action");
    }
    if ((decision.equals("REJECT") || decision.equals("RETURN"))
        && !StringUtils.hasText(comment)) {
      throw new BadRequestException("Comment is required for this workflow action");
    }

    Task task = tasks.get(0);
    if (tasks.size() > 1) {
      log.warn(
          "Multiple active tasks for user on businessKey={}; completing taskId={}",
          businessKey,
          task.getId());
    }

    HashMap<String, Object> vars = new HashMap<>();
    vars.put(CorrespondenceWorkflowTaskPersistenceService.VAR_WF_DECISION, decision);
    if (StringUtils.hasText(comment)) {
      vars.put(CorrespondenceWorkflowTaskPersistenceService.VAR_ACTION_COMMENT, comment.trim());
    }
    taskService.complete(task.getId(), vars);
  }
}
