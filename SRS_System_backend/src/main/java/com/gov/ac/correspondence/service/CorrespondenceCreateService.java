package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.dto.CorrespondenceAttachmentForm;
import com.gov.ac.correspondence.dto.CorrespondenceCreateForm;
import com.gov.ac.correspondence.dto.CorrespondenceCreatedResponse;
import com.gov.ac.correspondence.mapper.CorrespondenceCreateMapper;
import com.gov.ac.correspondence.reference.ReferenceNumberGenerator;
import com.gov.ac.correspondence.workflow.CamundaCorrespondenceWorkflowService;
import com.gov.ac.correspondence.workflow.CamundaCorrespondenceWorkflowService.StartedProcess;
import com.gov.ac.correspondence.workflow.CorrespondenceProcessDefinitionKeys;
import com.gov.ac.domain.correspondence.Attachment;
import com.gov.ac.domain.correspondence.AttachmentVersion;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.correspondence.CorrespondenceComment;
import com.gov.ac.domain.lookup.AttachmentContentType;
import com.gov.ac.domain.lookup.WorkflowInstanceStatus;
import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.org.Organization;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.workflow.WorkflowHistory;
import com.gov.ac.domain.workflow.WorkflowInstance;
import com.gov.ac.lookup.LookupResolutionService;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.AttachmentRepository;
import com.gov.ac.persistence.AttachmentVersionRepository;
import com.gov.ac.persistence.CorrespondenceCommentRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.persistence.DepartmentRepository;
import com.gov.ac.persistence.OrganizationRepository;
import com.gov.ac.persistence.WorkflowHistoryRepository;
import com.gov.ac.persistence.WorkflowInstanceRepository;
import com.gov.ac.persistence.WorkflowInstanceStatusRepository;
import com.gov.ac.web.BadRequestException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceCreateService {

  /** SRS §14.1 aggregate cap (application-enforced). */
  private static final long MAX_TOTAL_ATTACHMENT_BYTES = 200L * 1024 * 1024;

  private static final String INITIAL_STATUS_CODE = "NEW";
  private static final String WORKFLOW_INSTANCE_RUNNING = "RUNNING";
  private static final String HISTORY_EVENT_CREATE = "CREATE";
  private static final String HISTORY_ACTION_CREATE = "CREATE";

  private final LookupResolutionService lookups;
  private final ReferenceNumberGenerator referenceNumberGenerator;
  private final CamundaCorrespondenceWorkflowService camundaWorkflow;
  private final CorrespondenceCreateMapper createMapper;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final OrganizationRepository organizationRepository;
  private final DepartmentRepository departmentRepository;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final CorrespondenceCommentRepository correspondenceCommentRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final WorkflowInstanceStatusRepository workflowInstanceStatusRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;

  @Transactional(rollbackFor = Exception.class)
  public CorrespondenceCreatedResponse create(UUID actorUserId, CorrespondenceCreateForm form) {
    AppUser actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new BadRequestException("Unknown or deleted user: " + actorUserId));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new BadRequestException("Inactive user cannot create correspondence");
    }

    var type = lookups.requireActiveCorrespondenceType(form.getCorrespondenceTypeCode());
    var initialStatus = lookups.requireActiveCorrespondenceStatus(INITIAL_STATUS_CODE);
    var priority = lookups.requireActivePriority(form.getPriorityCode());
    var confidentiality = lookups.requireActiveConfidentiality(form.getConfidentialityCode());
    var classification = lookups.requireActiveClassification(form.getClassificationCode());

    Organization sender = resolveOrganization(form.getSenderOrganizationId());
    Organization recipient = resolveOrganization(form.getRecipientOrganizationId());
    Department ownerDept = resolveDepartment(form.getOwnerDepartmentId());

    long attachmentTotal = sumAttachmentBytes(form.getAttachments());
    if (attachmentTotal > MAX_TOTAL_ATTACHMENT_BYTES) {
      throw new BadRequestException(
          "Total attachment size exceeds limit of " + MAX_TOTAL_ATTACHMENT_BYTES + " bytes");
    }

    String referenceNumber = referenceNumberGenerator.nextReferenceNumber();
    String processKey = CorrespondenceProcessDefinitionKeys.forCorrespondenceTypeCode(type.getCode());

    Correspondence correspondence = new Correspondence();
    correspondence.setReferenceNumber(referenceNumber);
    correspondence.setCorrespondenceType(type);
    correspondence.setCorrespondenceStatus(initialStatus);
    correspondence.setPriority(priority);
    correspondence.setConfidentiality(confidentiality);
    correspondence.setClassification(classification);
    correspondence.setSubject(form.getSubject().trim());
    correspondence.setDescription(trimToNull(form.getDescription()));
    correspondence.setBodyHtml(trimToNull(form.getBodyHtml()));
    correspondence.setSenderOrganization(sender);
    correspondence.setRecipientOrganization(recipient);
    correspondence.setExternalReferenceNumber(trimToNull(form.getExternalReferenceNumber()));
    correspondence.setExternalReferenceDate(form.getExternalReferenceDate());
    correspondence.setOwnerDepartment(ownerDept);
    correspondence.setDueDate(form.getDueDate());
    correspondence.setBarcodeValue(trimToNull(form.getBarcodeValue()));
    correspondence.setTotalAttachmentBytes(attachmentTotal);
    correspondence.setCreatedBy(actorUserId);
    correspondence.setUpdatedBy(actorUserId);

    correspondence = correspondenceRepository.saveAndFlush(correspondence);
    log.debug("Saved correspondence id={} reference={}", correspondence.getId(), referenceNumber);

    persistAttachments(actorUserId, correspondence, form.getAttachments());

    if (StringUtils.hasText(form.getPrimaryComment())) {
      CorrespondenceComment comment = new CorrespondenceComment();
      comment.setCorrespondence(correspondence);
      comment.setAuthor(actor);
      comment.setBody(form.getPrimaryComment().trim());
      comment.setCreatedBy(actorUserId);
      comment.setUpdatedBy(actorUserId);
      correspondenceCommentRepository.save(comment);
    }

    StartedProcess started =
        camundaWorkflow.startCorrespondenceProcess(
            processKey, referenceNumber, actorUserId, correspondence.getId());

    WorkflowInstanceStatus running =
        workflowInstanceStatusRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(WORKFLOW_INSTANCE_RUNNING)
            .orElseThrow(
                () -> new IllegalStateException("Missing workflow_instance_status: RUNNING"));

    WorkflowInstance instance = new WorkflowInstance();
    instance.setCorrespondence(correspondence);
    instance.setProcessDefinitionKey(started.processDefinitionKey());
    instance.setProcessInstanceId(started.processInstanceId());
    instance.setStatus(running);
    instance.setStartedAt(Instant.now());
    instance.setBusinessKey(referenceNumber);
    instance.setCreatedBy(actorUserId);
    instance.setUpdatedBy(actorUserId);
    instance = workflowInstanceRepository.save(instance);

    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;
    var eventType = lookups.requireActiveHistoryEventType(HISTORY_EVENT_CREATE);
    var actionType = lookups.requireActiveWorkflowActionType(HISTORY_ACTION_CREATE);

    WorkflowHistory history = new WorkflowHistory();
    history.setCorrespondence(correspondence);
    history.setWorkflowInstance(instance);
    history.setEventType(eventType);
    history.setWorkflowActionType(actionType);
    history.setActor(actor);
    history.setOccurredAt(Instant.now());
    history.setSequenceNo(nextSeq);
    history.setPrimaryCommentText(trimToNull(form.getPrimaryComment()));
    history.setNewCorrespondenceStatus(initialStatus);
    history.setPriorityAtEvent(priority);
    Map<String, Object> detail = new HashMap<>();
    detail.put("referenceNumber", referenceNumber);
    detail.put("correspondenceTypeCode", type.getCode());
    detail.put("subject", correspondence.getSubject());
    history.setDetail(detail);
    history.setCreatedBy(actorUserId);
    history.setUpdatedBy(actorUserId);
    workflowHistoryRepository.save(history);

    log.info(
        "Created correspondence id={} reference={} processKey={} camundaInstance={} user={}",
        correspondence.getId(),
        referenceNumber,
        processKey,
        started.processInstanceId(),
        actorUserId);

    return createMapper.toCreatedResponse(
        correspondence, instance, started.processInstanceId());
  }

  private long sumAttachmentBytes(List<CorrespondenceAttachmentForm> attachments) {
    if (CollectionUtils.isEmpty(attachments)) {
      return 0L;
    }
    long sum = 0L;
    for (CorrespondenceAttachmentForm a : attachments) {
      sum += a.getByteSize();
    }
    return sum;
  }

  private void persistAttachments(
      UUID actorUserId, Correspondence correspondence, List<CorrespondenceAttachmentForm> items) {
    if (CollectionUtils.isEmpty(items)) {
      return;
    }
    for (CorrespondenceAttachmentForm item : items) {
      AttachmentContentType contentType = null;
      if (StringUtils.hasText(item.getContentTypeCode())) {
        contentType = lookups.requireActiveAttachmentContentType(item.getContentTypeCode());
      }
      Attachment attachment = new Attachment();
      attachment.setCorrespondence(correspondence);
      attachment.setContentType(contentType);
      attachment.setDisplayName(item.getDisplayName().trim());
      attachment.setCreatedBy(actorUserId);
      attachment.setUpdatedBy(actorUserId);
      attachment = attachmentRepository.saveAndFlush(attachment);

      AttachmentVersion version = new AttachmentVersion();
      version.setAttachment(attachment);
      version.setVersionNumber(1);
      version.setStorageKey(item.getStorageKey().trim());
      version.setByteSize(item.getByteSize());
      version.setMimeType(trimToNull(item.getMimeType()));
      version.setChecksumSha256(trimToNull(item.getChecksumSha256()));
      version.setCreatedBy(actorUserId);
      version.setUpdatedBy(actorUserId);
      version = attachmentVersionRepository.saveAndFlush(version);

      attachment.setCurrentVersionId(version.getId());
      attachmentRepository.save(attachment);
    }
  }

  private Organization resolveOrganization(Long id) {
    if (id == null) {
      return null;
    }
    return organizationRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown or deleted organization: " + id));
  }

  private Department resolveDepartment(Long id) {
    if (id == null) {
      return null;
    }
    return departmentRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown or deleted department: " + id));
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
