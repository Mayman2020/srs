package com.gov.ac.feature.correspondence.service;

import com.gov.ac.common.audit.UserAuditRefDto;
import com.gov.ac.common.audit.UserAuditResolutionService;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDetailResponseDto;
import com.gov.ac.feature.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.feature.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
import com.gov.ac.feature.correspondence.readtracking.service.CorrespondenceReadTrackingService;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceCommentEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceCommentRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceDetailService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final CorrespondenceCommentRepository correspondenceCommentRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;
  private final CorrespondenceDetailMapper correspondenceDetailMapper;
  private final CorrespondenceWorkflowAvailabilityService correspondenceWorkflowAvailabilityService;
  private final CorrespondenceCancelService correspondenceCancelService;
  private final CorrespondenceReadTrackingService correspondenceReadTrackingService;
  private final UserAuditResolutionService userAuditResolutionService;

  @Transactional(readOnly = true)
  public CorrespondenceDetailResponseDto getByBarcode(String barcode, UUID viewerId) {
    if (!StringUtils.hasText(barcode)) {
      throw new NotFoundException("CorrespondenceEntity not found");
    }
    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByBarcodeValueIgnoreCaseAndDeletedAtIsNull(barcode.trim())
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    return getById(correspondence.getId(), viewerId);
  }

  @Transactional(readOnly = true)
  public CorrespondenceDetailResponseDto getById(UUID correspondenceId, UUID viewerId) {
    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findDetailGraphByIdAndDeletedAtIsNull(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));

    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "CorrespondenceEntity view denied: unknown or deleted viewer userId={} correspondenceId={}",
                      viewerId,
                      correspondenceId);
                  return new ForbiddenException("You do not have access to this correspondence");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      log.warn(
          "CorrespondenceEntity view denied: inactive viewer userId={} correspondenceId={}",
          viewerId,
          correspondenceId);
      throw new ForbiddenException("You do not have access to this correspondence");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    List<AttachmentEntity> attachments =
        attachmentRepository.findAllForDetailByCorrespondenceId(correspondenceId);
    List<Long> attachmentIds = attachments.stream().map(AttachmentEntity::getId).toList();
    List<AttachmentVersionEntity> versions =
        attachmentIds.isEmpty()
            ? List.of()
            : attachmentVersionRepository.findAllForDetailByAttachmentIdIn(attachmentIds);
    Map<Long, List<AttachmentVersionEntity>> versionsByAttachmentId =
        versions.stream()
            .collect(Collectors.groupingBy(v -> v.getAttachment().getId()));

    List<CorrespondenceCommentEntity> comments =
        correspondenceCommentRepository.findAllForDetailByCorrespondenceId(correspondenceId);
    List<WorkflowHistoryEntity> history =
        workflowHistoryRepository.findByCorrespondence_IdOrderBySequenceNoAsc(correspondenceId);

    List<WorkflowActionAvailableDto> actions =
        correspondenceWorkflowAvailabilityService.listAvailableWorkflowActions(
            correspondenceId, viewerId);

    boolean cancelAllowed = correspondenceCancelService.isUserCancelAllowed(correspondence);

    try {
      correspondenceReadTrackingService.recordOpen(correspondenceId, viewerId);
    } catch (RuntimeException ex) {
      log.warn(
          "Read tracking failed (non-fatal) for correspondenceId={} viewerId={}: {}",
          correspondenceId,
          viewerId,
          ex.getMessage());
    }

    CorrespondenceReadReceiptDto myReceipt = null;
    try {
      myReceipt =
          correspondenceReadTrackingService.getOwnReceipt(correspondenceId, viewerId).orElse(null);
    } catch (RuntimeException ex) {
      log.warn(
          "Read tracking lookup failed (non-fatal) for correspondenceId={} viewerId={}: {}",
          correspondenceId,
          viewerId,
          ex.getMessage());
    }

    UserAuditRefDto createdByUser =
        userAuditResolutionService.toRef(correspondence.getCreatedBy()).orElse(null);
    UserAuditRefDto updatedByUser =
        userAuditResolutionService.toRef(correspondence.getUpdatedBy()).orElse(null);

    return correspondenceDetailMapper.toResponse(
        correspondence,
        attachments,
        versionsByAttachmentId,
        comments,
        history,
        actions,
        cancelAllowed,
        myReceipt,
        true,
        createdByUser,
        updatedByUser);
  }
}
