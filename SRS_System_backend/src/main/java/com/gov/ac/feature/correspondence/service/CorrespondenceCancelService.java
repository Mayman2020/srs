package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCancelRequestDto;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.CorrespondenceLookupCodes;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.retention.LegalHoldService;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import com.gov.ac.feature.lookups.repository.WorkflowInstanceStatusRepository;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceCancelService {

  private final CorrespondenceRepository correspondenceRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final WorkflowInstanceStatusRepository workflowInstanceStatusRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;
  private final LookupResolutionService lookups;
  private final WorkflowService workflowService;
  private final CorrespondenceActionAudit correspondenceActionAudit;
  private final EffectiveUserPermissionService effectiveUserPermissionService;
  private final LegalHoldService legalHoldService;

  @Transactional
  public void cancel(UUID correspondenceId, UUID actorUserId, CorrespondenceCancelRequestDto body) {
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot cancel this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot cancel this correspondence");
    }
    if (!effectiveUserPermissionService.hasActivePermission(actorUserId, "CORRESPONDENCE_DELETE")) {
      throw new ForbiddenException("Missing CORRESPONDENCE_DELETE permission");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));

    correspondenceViewAuthorization.assertCanView(actor, correspondence);
    legalHoldService.assertNotHeld(correspondenceId);

    CorrespondenceStatusEntity previous = correspondence.getCorrespondenceStatus();
    if (!isUserCancelAllowed(correspondence)) {
      String currentCode =
          previous != null && previous.getCode() != null ? previous.getCode().trim() : "?";
      throw new BadRequestException("CorrespondenceEntity cannot be cancelled in status: " + currentCode);
    }

    CorrespondenceStatusEntity cancelled =
        correspondenceStatusRepository
            .findByCancelOutcomeTrueAndActiveTrueAndDeletedAtIsNull()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No correspondence_status is marked as cancel outcome (cancel_outcome)."));
    correspondence.setCorrespondenceStatus(cancelled);
    correspondence.setUpdatedBy(actorUserId);
    correspondenceRepository.save(correspondence);

    List<WorkflowInstanceEntity> instances =
        workflowInstanceRepository.findByCorrespondence_IdAndDeletedAtIsNullOrderByStartedAtDesc(
            correspondenceId);
    WorkflowInstanceEntity primary = instances.isEmpty() ? null : instances.get(0);

    if (primary != null && StringUtils.hasText(primary.getProcessInstanceId())) {
      if (workflowService.hasActiveProcessInstance(primary.getProcessInstanceId())) {
        try {
          workflowService.deleteProcessInstance(
              primary.getProcessInstanceId(), "CORRESPONDENCE_CANCELLED");
        } catch (Exception e) {
          log.warn(
              "Camunda deleteProcessInstance failed for {}: {}",
              primary.getProcessInstanceId(),
              e.getMessage());
        }
      }
      var terminated =
          workflowInstanceStatusRepository
              .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
                  CorrespondenceLookupCodes.WORKFLOW_INSTANCE_TERMINATED)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Missing workflow_instance_status TERMINATED"));
      primary.setStatus(terminated);
      primary.setEndedAt(Instant.now());
      primary.setUpdatedBy(actorUserId);
      workflowInstanceRepository.save(primary);
    }

    WorkflowHistoryEventTypeEntity eventType = lookups.requireActiveHistoryEventType("STATUS_CHANGE");
    WorkflowActionTypeEntity closeAction = lookups.requireActiveWorkflowActionType("CLOSE");
    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;
    WorkflowHistoryEntity history = new WorkflowHistoryEntity();
    history.setCorrespondence(correspondence);
    history.setWorkflowInstance(primary);
    history.setEventType(eventType);
    history.setWorkflowActionType(closeAction);
    history.setActor(actor);
    history.setOccurredAt(Instant.now());
    history.setSequenceNo(nextSeq);
    history.setPrimaryCommentText(
        body != null && StringUtils.hasText(body.reason()) ? body.reason().trim() : null);
    history.setPreviousCorrespondenceStatus(previous);
    history.setNewCorrespondenceStatus(cancelled);
    history.setPriorityAtEvent(correspondence.getPriority());
    Map<String, Object> detail = new HashMap<>();
    detail.put("action", "CANCEL");
    history.setDetail(detail);
    history.setCreatedBy(actorUserId);
    history.setUpdatedBy(actorUserId);
    workflowHistoryRepository.save(history);

    Map<String, Object> auditDetail = new HashMap<>();
    auditDetail.put("previousStatus", previous.getCode());
    if (body != null && StringUtils.hasText(body.reason())) {
      auditDetail.put("reason", body.reason().trim());
    }
    correspondenceActionAudit.log(
        actorUserId, CorrespondenceActionAudit.ACTION_CANCEL, correspondenceId, auditDetail);

    log.info("CorrespondenceEntity cancelled id={} by user={}", correspondenceId, actorUserId);
  }

  /**
   * User cancel is allowed when the row is non-terminal, {@link CorrespondenceStatusEntity#getAllowsCancel()}
   * is not false, and a cancel-outcome status exists in the database.
   */
  public boolean isUserCancelAllowed(CorrespondenceEntity correspondence) {
    CorrespondenceStatusEntity s = correspondence.getCorrespondenceStatus();
    if (s == null) {
      return false;
    }
    if (Boolean.TRUE.equals(s.getTerminal())) {
      return false;
    }
    if (Boolean.FALSE.equals(s.getAllowsCancel())) {
      return false;
    }
    return correspondenceStatusRepository
        .findByCancelOutcomeTrueAndActiveTrueAndDeletedAtIsNull()
        .isPresent();
  }
}
