package com.gov.ac.feature.shared.lookup.service;

import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.lookups.repository.AttachmentContentTypeRepository;
import com.gov.ac.feature.lookups.repository.ClassificationRepository;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.lookups.repository.PriorityRepository;
import com.gov.ac.feature.correspondence.workflow.WorkflowActionResolutionService;
import com.gov.ac.feature.lookups.repository.WorkflowHistoryEventTypeRepository;
import com.gov.ac.common.api.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves lookup rows by stable {@code code} for all write paths — avoids duplicated validation
 * logic per feature.
 */
@Service
@RequiredArgsConstructor
public class LookupResolutionService {

  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final ClassificationRepository classificationRepository;
  private final AttachmentContentTypeRepository attachmentContentTypeRepository;
  private final WorkflowHistoryEventTypeRepository workflowHistoryEventTypeRepository;
  private final WorkflowActionResolutionService workflowActionResolution;

  public CorrespondenceTypeEntity requireActiveCorrespondenceType(String code) {
    return correspondenceTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive correspondence type: " + code));
  }

  public CorrespondenceStatusEntity requireActiveCorrespondenceStatus(String code) {
    return correspondenceStatusRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive correspondence status: " + code));
  }

  public PriorityEntity requireActivePriority(String code) {
    return priorityRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive priority: " + code));
  }

  public ConfidentialityEntity requireActiveConfidentiality(String code) {
    return confidentialityRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive confidentiality: " + code));
  }

  public ClassificationEntity requireActiveClassification(String code) {
    return classificationRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive classification: " + code));
  }

  public AttachmentContentTypeEntity requireActiveAttachmentContentType(String code) {
    return attachmentContentTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive attachment content type: " + code));
  }

  public WorkflowHistoryEventTypeEntity requireActiveHistoryEventType(String code) {
    return workflowHistoryEventTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown workflow history event type: " + code));
  }

  /** Wildcard {@code workflow_action_type} rows only ({@code allowed_from} is null). */
  public WorkflowActionTypeEntity requireActiveWorkflowActionType(String code) {
    return workflowActionResolution
        .resolveWildcardOnly(code)
        .orElseThrow(() -> new BadRequestException("Unknown workflow action type: " + code));
  }

  /** Resolves the row for a Camunda {@code wfDecision} and current correspondence status. */
  public WorkflowActionTypeEntity requireWorkflowActionForTransition(
      String code, Long fromCorrespondenceStatusId) {
    return workflowActionResolution
        .resolveTransition(code, fromCorrespondenceStatusId)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "Unknown workflow action for current status: " + code));
  }
}
