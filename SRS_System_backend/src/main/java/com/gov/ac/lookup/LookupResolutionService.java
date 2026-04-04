package com.gov.ac.lookup;

import com.gov.ac.domain.lookup.AttachmentContentType;
import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.domain.org.Classification;
import com.gov.ac.persistence.AttachmentContentTypeRepository;
import com.gov.ac.persistence.ClassificationRepository;
import com.gov.ac.persistence.ConfidentialityRepository;
import com.gov.ac.persistence.CorrespondenceStatusRepository;
import com.gov.ac.persistence.CorrespondenceTypeRepository;
import com.gov.ac.persistence.PriorityRepository;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
import com.gov.ac.persistence.WorkflowHistoryEventTypeRepository;
import com.gov.ac.web.BadRequestException;
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
  private final WorkflowActionTypeRepository workflowActionTypeRepository;

  public CorrespondenceType requireActiveCorrespondenceType(String code) {
    return correspondenceTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive correspondence type: " + code));
  }

  public CorrespondenceStatus requireActiveCorrespondenceStatus(String code) {
    return correspondenceStatusRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive correspondence status: " + code));
  }

  public Priority requireActivePriority(String code) {
    return priorityRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive priority: " + code));
  }

  public Confidentiality requireActiveConfidentiality(String code) {
    return confidentialityRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive confidentiality: " + code));
  }

  public Classification requireActiveClassification(String code) {
    return classificationRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive classification: " + code));
  }

  public AttachmentContentType requireActiveAttachmentContentType(String code) {
    return attachmentContentTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown or inactive attachment content type: " + code));
  }

  public WorkflowHistoryEventType requireActiveHistoryEventType(String code) {
    return workflowHistoryEventTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown workflow history event type: " + code));
  }

  public WorkflowActionType requireActiveWorkflowActionType(String code) {
    return workflowActionTypeRepository
        .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(code)
        .orElseThrow(() -> new BadRequestException("Unknown workflow action type: " + code));
  }
}
