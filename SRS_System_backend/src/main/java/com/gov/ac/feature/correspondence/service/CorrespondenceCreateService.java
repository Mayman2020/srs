package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreateFormDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceCreatedResponseDto;
import com.gov.ac.feature.correspondence.mapper.CorrespondenceCreateMapper;
import com.gov.ac.feature.correspondence.reference.ReferenceNumberGenerator;
import com.gov.ac.feature.correspondence.workflow.CamundaCorrespondenceWorkflowService;
import com.gov.ac.feature.correspondence.workflow.CamundaCorrespondenceWorkflowService.StartedProcess;
import com.gov.ac.feature.correspondence.CorrespondenceAggregateLimits;
import com.gov.ac.feature.correspondence.CorrespondenceLookupCodes;
import com.gov.ac.feature.correspondence.workflow.WorkflowInstanceRoutingSyncService;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceCommentEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;
import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowInstanceStatusEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.shared.notification.service.NotificationService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceCommentRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organizations.repository.OrganizationRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.workflow.routes.repository.ServiceWorkflowRouteRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.feature.lookups.repository.WorkflowInstanceStatusRepository;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.SystemConfigurationException;
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
  private final NotificationService notificationService;
  private final RoleRepository roleRepository;
  private final ServiceWorkflowRouteRepository serviceWorkflowRouteRepository;
  private final EffectiveUserPermissionService effectiveUserPermissionService;
  private final WorkflowInstanceRoutingSyncService routingSyncService;
  private final CorrespondenceCreateRecipientSupport createRecipientSupport;

  /**
   * Creates correspondence, attachments, Camunda process, {@code workflow_instance}, and first
   * {@code workflow_history} in one transaction. Rolls back on any failure, including Camunda
   * startup errors, when the process engine shares the Spring-managed transaction (default for
   * Camunda Spring Boot).
   */
  @Transactional(rollbackFor = Exception.class)
  public CorrespondenceCreatedResponseDto create(UUID actorUserId, CorrespondenceCreateFormDto form) {
    Instant now = Instant.now();

    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new BadRequestException("Unknown or deleted user"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new BadRequestException("Inactive user cannot create correspondence");
    }
    if (!effectiveUserPermissionService.hasActivePermission(actorUserId, "CORRESPONDENCE_CREATE")) {
      throw new ForbiddenException("Missing CORRESPONDENCE_CREATE permission");
    }

    var type = lookups.requireActiveCorrespondenceType(form.getCorrespondenceTypeCode());
    var initialStatus =
        lookups.requireActiveCorrespondenceStatus(
            CorrespondenceLookupCodes.INITIAL_CORRESPONDENCE_STATUS);
    var priority = lookups.requireActivePriority(form.getPriorityCode());
    var confidentiality = lookups.requireActiveConfidentiality(form.getConfidentialityCode());
    var classification = lookups.requireActiveClassification(form.getClassificationCode());

    OrganizationEntity sender = resolveOrganization(form.getSenderOrganizationId());
    OrganizationEntity recipient = resolveOrganization(form.getRecipientOrganizationId());
    DepartmentEntity ownerDept = resolveDepartment(form.getOwnerDepartmentId());

    if (!CollectionUtils.isEmpty(form.getAttachments())
        && form.getAttachments().size() > CorrespondenceAggregateLimits.MAX_ATTACHMENTS_COUNT) {
      throw new BadRequestException(
          "Too many attachments (max "
              + CorrespondenceAggregateLimits.MAX_ATTACHMENTS_COUNT
              + " per correspondence)");
    }

    long attachmentTotal = sumAttachmentBytes(form.getAttachments());
    if (attachmentTotal > CorrespondenceAggregateLimits.MAX_TOTAL_ATTACHMENT_BYTES) {
      throw new BadRequestException(
          "Total attachment size exceeds limit of "
              + CorrespondenceAggregateLimits.MAX_TOTAL_ATTACHMENT_BYTES
              + " bytes");
    }

    validateAttachmentSizes(form.getAttachments());

    validateWorkflowFirstAssignment(form);

    String referenceNumber = referenceNumberGenerator.nextReferenceNumber();
    ResolvedWorkflow resolved = resolveWorkflow(form, type);
    String processKey = resolved.processKey();

    CorrespondenceEntity correspondence = new CorrespondenceEntity();
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
    correspondence.setWorkflowRouteMode(resolved.modeStored());
    correspondence.setServiceWorkflowRoute(resolved.routeEntity());
    correspondence.setSupplyTransaction(Boolean.TRUE.equals(form.getSupplyTransaction()));
    correspondence.setBeneficiaryName(trimToNull(form.getBeneficiaryName()));
    correspondence.setBeneficiaryOrganization(trimToNull(form.getBeneficiaryOrganization()));
    correspondence.setBeneficiaryIdentifier(trimToNull(form.getBeneficiaryIdentifier()));
    correspondence.setCreatedBy(actorUserId);
    correspondence.setUpdatedBy(actorUserId);

    correspondence = correspondenceRepository.saveAndFlush(correspondence);
    log.debug("Saved correspondence id={} reference={}", correspondence.getId(), referenceNumber);

    if (!StringUtils.hasText(correspondence.getBarcodeValue())) {
      correspondence.setBarcodeValue(referenceNumber);
      correspondence = correspondenceRepository.saveAndFlush(correspondence);
    }

    persistAttachments(actorUserId, correspondence, form.getAttachments());

    if (StringUtils.hasText(form.getPrimaryComment())) {
      CorrespondenceCommentEntity comment = new CorrespondenceCommentEntity();
      comment.setCorrespondence(correspondence);
      comment.setAuthor(actor);
      comment.setBody(form.getPrimaryComment().trim());
      comment.setCreatedBy(actorUserId);
      comment.setUpdatedBy(actorUserId);
      correspondenceCommentRepository.save(comment);
    }

    UUID wfAssignee = form.getWorkflowFirstAssigneeUserId();
    String wfGroup =
        StringUtils.hasText(form.getWorkflowFirstCandidateGroup())
            ? form.getWorkflowFirstCandidateGroup().trim()
            : null;

    Long originatorDepartmentId =
        actor != null && actor.getDepartment() != null ? actor.getDepartment().getId() : null;
    Long targetDepartmentId =
        correspondence.getOwnerDepartment() != null
            ? correspondence.getOwnerDepartment().getId()
            : null;

    StartedProcess started =
        camundaWorkflow.startCorrespondenceProcess(
            processKey,
            referenceNumber,
            actorUserId,
            correspondence.getId(),
            wfAssignee,
            wfGroup,
            originatorDepartmentId,
            targetDepartmentId);

    WorkflowInstanceStatusEntity running =
        workflowInstanceStatusRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
                CorrespondenceLookupCodes.WORKFLOW_INSTANCE_RUNNING)
            .orElseThrow(
                () ->
                    new SystemConfigurationException(
                        "Missing active workflow_instance_status lookup: "
                            + CorrespondenceLookupCodes.WORKFLOW_INSTANCE_RUNNING));

    WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
    instance.setCorrespondence(correspondence);
    instance.setProcessDefinitionKey(started.processDefinitionKey());
    instance.setProcessInstanceId(started.processInstanceId());
    instance.setStatus(running);
    instance.setStartedAt(now);
    instance.setBusinessKey(referenceNumber);
    instance.setOriginatorDepartmentId(originatorDepartmentId);
    instance.setTargetDepartmentId(targetDepartmentId);
    instance.setCreatedBy(actorUserId);
    instance.setUpdatedBy(actorUserId);
    instance = workflowInstanceRepository.save(instance);

    routingSyncService.syncFromEngine(started.processInstanceId());

    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;
    var eventType =
        lookups.requireActiveHistoryEventType(CorrespondenceLookupCodes.WORKFLOW_HISTORY_CREATE);
    var actionType =
        lookups.requireActiveWorkflowActionType(CorrespondenceLookupCodes.WORKFLOW_HISTORY_CREATE);

    WorkflowHistoryEntity history = new WorkflowHistoryEntity();
    history.setCorrespondence(correspondence);
    history.setWorkflowInstance(instance);
    history.setEventType(eventType);
    history.setWorkflowActionType(actionType);
    history.setActor(actor);
    history.setOccurredAt(now);
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
        resolved.processKey(),
        started.processInstanceId(),
        actorUserId);

    notificationService.notifyCorrespondenceCreated(correspondence, actor);
    if (StringUtils.hasText(form.getPrimaryComment())) {
      notificationService.notifyCommentAdded(correspondence, actor);
    }

    createRecipientSupport.persistAfterCreate(actorUserId, correspondence, form);

    return createMapper.toCreatedResponse(
        correspondence, instance, started.processInstanceId());
  }

  private void validateAttachmentSizes(List<CorrespondenceAttachmentFormDto> attachments) {
    if (CollectionUtils.isEmpty(attachments)) {
      return;
    }
    for (CorrespondenceAttachmentFormDto a : attachments) {
      if (a.getByteSize() == null || a.getByteSize() < 0) {
        throw new BadRequestException("Invalid attachment size");
      }
      if (!StringUtils.hasText(a.getContentTypeCode())) {
        continue;
      }
      AttachmentContentTypeEntity ct = lookups.requireActiveAttachmentContentType(a.getContentTypeCode());
      if (ct.getMaxBytes() != null && a.getByteSize() > ct.getMaxBytes()) {
        throw new BadRequestException(
            "AttachmentEntity exceeds max size for content type " + ct.getCode());
      }
    }
  }

  private long sumAttachmentBytes(List<CorrespondenceAttachmentFormDto> attachments) {
    if (CollectionUtils.isEmpty(attachments)) {
      return 0L;
    }
    long sum = 0L;
    for (CorrespondenceAttachmentFormDto a : attachments) {
      sum += a.getByteSize();
    }
    return sum;
  }

  private void persistAttachments(
      UUID actorUserId, CorrespondenceEntity correspondence, List<CorrespondenceAttachmentFormDto> items) {
    if (CollectionUtils.isEmpty(items)) {
      return;
    }
    for (CorrespondenceAttachmentFormDto item : items) {
      AttachmentContentTypeEntity contentType = null;
      if (StringUtils.hasText(item.getContentTypeCode())) {
        contentType = lookups.requireActiveAttachmentContentType(item.getContentTypeCode());
      }
      AttachmentEntity attachment = new AttachmentEntity();
      attachment.setCorrespondence(correspondence);
      attachment.setContentType(contentType);
      attachment.setDisplayName(item.getDisplayName().trim());
      attachment.setCreatedBy(actorUserId);
      attachment.setUpdatedBy(actorUserId);
      attachment = attachmentRepository.saveAndFlush(attachment);

      AttachmentVersionEntity version = new AttachmentVersionEntity();
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

  private OrganizationEntity resolveOrganization(Long id) {
    if (id == null) {
      return null;
    }
    return organizationRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> {
              log.debug("Reject create: unknown organization id={}", id);
              return new BadRequestException("Unknown or deleted organization");
            });
  }

  private DepartmentEntity resolveDepartment(Long id) {
    if (id == null) {
      return null;
    }
    return departmentRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () -> {
              log.debug("Reject create: unknown department id={}", id);
              return new BadRequestException("Unknown or deleted department");
            });
  }

  private void validateWorkflowFirstAssignment(CorrespondenceCreateFormDto form) {
    UUID assignee = form.getWorkflowFirstAssigneeUserId();
    boolean hasGroup = StringUtils.hasText(form.getWorkflowFirstCandidateGroup());
    if (assignee != null && hasGroup) {
      throw new BadRequestException(
          "Use either workflowFirstAssigneeUserId or workflowFirstCandidateGroup, not both");
    }
    if (assignee != null) {
      appUserRepository
          .findByIdAndDeletedAtIsNull(assignee)
          .filter(u -> Boolean.TRUE.equals(u.getActive()))
          .orElseThrow(
              () -> new BadRequestException("workflowFirstAssigneeUserId: unknown or inactive user"));
    }
    if (hasGroup) {
      String code = form.getWorkflowFirstCandidateGroup().trim();
      roleRepository
          .findByCodeIgnoreCaseAndDeletedAtIsNullAndActiveTrue(code)
          .orElseThrow(
              () ->
                  new BadRequestException(
                      "workflowFirstCandidateGroup: unknown or inactive role code: " + code));
    }
  }

  private ResolvedWorkflow resolveWorkflow(CorrespondenceCreateFormDto form, CorrespondenceTypeEntity type) {
    String mode = form.getWorkflowRouteMode() != null ? form.getWorkflowRouteMode().trim() : "AUTO";
    boolean manual = "MANUAL".equalsIgnoreCase(mode);
    if (manual) {
      if (form.getServiceWorkflowRouteId() == null) {
        throw new BadRequestException(
            "serviceWorkflowRouteId is required when workflowRouteMode is MANUAL");
      }
      ServiceWorkflowRouteEntity r =
          serviceWorkflowRouteRepository
              .findByIdAndDeletedAtIsNull(form.getServiceWorkflowRouteId())
              .orElseThrow(() -> new BadRequestException("Unknown workflow route"));
      if (!Boolean.TRUE.equals(r.getActive())) {
        throw new BadRequestException("Workflow route is inactive");
      }
      if (!r.getCorrespondenceType().getId().equals(type.getId())) {
        throw new BadRequestException("Workflow route does not match correspondence type");
      }
      return new ResolvedWorkflow(r.getProcessDefinitionKey(), r, "MANUAL");
    }
    ServiceWorkflowRouteEntity def =
        serviceWorkflowRouteRepository
            .findFirstByCorrespondenceTypeIdAndDefaultRouteIsTrueAndActiveIsTrueAndDeletedAtIsNull(
                type.getId())
            .orElse(null);
    if (def != null) {
      return new ResolvedWorkflow(def.getProcessDefinitionKey(), def, "AUTO");
    }
    throw new BadRequestException(
        "No default workflow route configured for correspondence type: " + type.getCode());
  }

  private record ResolvedWorkflow(
      String processKey, ServiceWorkflowRouteEntity routeEntity, String modeStored) {}

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
