package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceCamundaTaskSupport;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowTaskPersistenceService;
import com.gov.ac.feature.correspondence.workflow.WorkflowActionResolutionService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final WorkflowService workflowService;
  private final CorrespondenceCamundaTaskSupport camundaTaskSupport;
  private final WorkflowActionResolutionService workflowActionResolution;

  @Transactional
  public void completeActiveAssigneeTask(
      UUID correspondenceId, UUID viewerId, String action, String comment) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(() -> new ForbiddenException("You cannot perform this action"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot perform this action");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("CorrespondenceEntity not found");
    }
    if (correspondence.getCorrespondenceStatus() != null
        && Boolean.TRUE.equals(correspondence.getCorrespondenceStatus().getTerminal())) {
      throw new BadRequestException("CorrespondenceEntity is not open for workflow actions");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    if (correspondence.getCorrespondenceStatus() == null) {
      throw new BadRequestException("CorrespondenceEntity has no status");
    }

    String businessKey = correspondence.getReferenceNumber();
    if (!StringUtils.hasText(businessKey)) {
      throw new BadRequestException("CorrespondenceEntity has no reference number");
    }

    String assignee = viewerId.toString();
    List<Task> tasks = camundaTaskSupport.findActiveTasksForUser(businessKey, viewerId);

    if (tasks.isEmpty()) {
      throw new BadRequestException(
          "No active workflow task for you on this correspondence (assignee or candidate)");
    }

    if (!StringUtils.hasText(action)) {
      throw new BadRequestException("Workflow action is required");
    }
    String decision = action.trim().toUpperCase();

    WorkflowActionTypeEntity rule =
        workflowActionResolution
            .resolveTransition(decision, correspondence.getCorrespondenceStatus().getId())
            .orElseThrow(() -> new BadRequestException("Invalid workflow action"));

    if (Boolean.TRUE.equals(rule.getRequiresComment()) && !StringUtils.hasText(comment)) {
      throw new BadRequestException("Comment is required for this workflow action");
    }

    Task task = tasks.get(0);
    if (tasks.size() > 1) {
      log.warn(
          "Multiple active tasks for user on businessKey={}; completing taskId={}",
          businessKey,
          task.getId());
    }

    if (task.getAssignee() == null) {
      workflowService.claimTask(task.getId(), assignee);
    }

    HashMap<String, Object> vars = new HashMap<>();
    vars.put(CorrespondenceWorkflowTaskPersistenceService.VAR_WF_DECISION, rule.getCode().toUpperCase());
    if (StringUtils.hasText(comment)) {
      vars.put(CorrespondenceWorkflowTaskPersistenceService.VAR_ACTION_COMMENT, comment.trim());
    }
    workflowService.completeTask(task.getId(), vars);
  }

  @Transactional
  public void delegateActiveAssigneeTask(
      UUID correspondenceId, UUID viewerId, UUID delegateeUserId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(() -> new ForbiddenException("You cannot perform this action"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot perform this action");
    }
    AppUserEntity delegatee =
        appUserRepository
            .findByIdAndDeletedAtIsNull(delegateeUserId)
            .orElseThrow(() -> new BadRequestException("Delegatee user not found"));
    if (!Boolean.TRUE.equals(delegatee.getActive())) {
      throw new BadRequestException("Delegatee user is not active");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("CorrespondenceEntity not found");
    }
    if (correspondence.getCorrespondenceStatus() != null
        && Boolean.TRUE.equals(correspondence.getCorrespondenceStatus().getTerminal())) {
      throw new BadRequestException("CorrespondenceEntity is not open for workflow actions");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    String businessKey = correspondence.getReferenceNumber();
    if (!StringUtils.hasText(businessKey)) {
      throw new BadRequestException("CorrespondenceEntity has no reference number");
    }

    String assignee = viewerId.toString();
    List<Task> tasks = camundaTaskSupport.findActiveTasksForUser(businessKey, viewerId);

    if (tasks.isEmpty()) {
      throw new BadRequestException(
          "No active workflow task for you on this correspondence (assignee or candidate)");
    }
    Task task = tasks.get(0);
    if (task.getAssignee() == null) {
      workflowService.claimTask(task.getId(), assignee);
    }
    workflowService.delegateTask(task.getId(), delegateeUserId.toString());
  }
}
