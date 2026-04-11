package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.dto.CorrespondenceDetailResponse;
import com.gov.ac.correspondence.dto.WorkflowActionAvailableDto;
import com.gov.ac.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.domain.correspondence.Attachment;
import com.gov.ac.domain.correspondence.AttachmentVersion;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.correspondence.CorrespondenceComment;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.workflow.WorkflowHistory;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.AttachmentRepository;
import com.gov.ac.persistence.AttachmentVersionRepository;
import com.gov.ac.persistence.CorrespondenceCommentRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.persistence.WorkflowHistoryRepository;
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

  @Transactional(readOnly = true)
  public CorrespondenceDetailResponse getById(UUID correspondenceId, UUID viewerId) {
    Correspondence correspondence =
        correspondenceRepository
            .findDetailGraphByIdAndDeletedAtIsNull(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));

    AppUser viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "Correspondence view denied: unknown or deleted viewer userId={} correspondenceId={}",
                      viewerId,
                      correspondenceId);
                  return new ForbiddenException("You do not have access to this correspondence");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      log.warn(
          "Correspondence view denied: inactive viewer userId={} correspondenceId={}",
          viewerId,
          correspondenceId);
      throw new ForbiddenException("You do not have access to this correspondence");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    List<Attachment> attachments =
        attachmentRepository.findAllForDetailByCorrespondenceId(correspondenceId);
    List<Long> attachmentIds = attachments.stream().map(Attachment::getId).toList();
    List<AttachmentVersion> versions =
        attachmentIds.isEmpty()
            ? List.of()
            : attachmentVersionRepository.findAllForDetailByAttachmentIdIn(attachmentIds);
    Map<Long, List<AttachmentVersion>> versionsByAttachmentId =
        versions.stream()
            .collect(Collectors.groupingBy(v -> v.getAttachment().getId()));

    List<CorrespondenceComment> comments =
        correspondenceCommentRepository.findAllForDetailByCorrespondenceId(correspondenceId);
    List<WorkflowHistory> history =
        workflowHistoryRepository.findByCorrespondence_IdOrderBySequenceNoAsc(correspondenceId);

    List<WorkflowActionAvailableDto> actions =
        correspondenceWorkflowAvailabilityService.listAvailableWorkflowActions(
            correspondenceId, viewerId);

    boolean cancelAllowed = correspondenceCancelService.isUserCancelAllowed(correspondence);

    return correspondenceDetailMapper.toResponse(
        correspondence, attachments, versionsByAttachmentId, comments, history, actions, cancelAllowed);
  }
}
