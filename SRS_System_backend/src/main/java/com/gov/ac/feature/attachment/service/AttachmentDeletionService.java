package com.gov.ac.feature.attachment.service;

import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.service.CorrespondenceMutationGuards;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentDeletionService {

  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final CorrespondenceActionAudit correspondenceActionAudit;

  @Transactional
  public void softDelete(Long attachmentId, UUID actorUserId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot delete this attachment"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You cannot delete this attachment");
    }

    AttachmentEntity attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("AttachmentEntity not found"));

    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("AttachmentEntity not found");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);
    CorrespondenceMutationGuards.assertCorrespondenceMutable(correspondence);

    Long verId = attachment.getCurrentVersionId();
    if (verId == null) {
      throw new NotFoundException("AttachmentEntity not found");
    }
    AttachmentVersionEntity version =
        attachmentVersionRepository
            .findByIdAndDeletedAtIsNullWithAttachment(verId)
            .orElseThrow(() -> new NotFoundException("AttachmentEntity not found"));

    long subtract = version.getByteSize() != null ? version.getByteSize() : 0L;
    Instant now = Instant.now();

    version.setDeletedAt(now);
    version.setDeletedBy(actorUserId);
    version.setUpdatedBy(actorUserId);
    attachmentVersionRepository.save(version);

    attachment.setDeletedAt(now);
    attachment.setDeletedBy(actorUserId);
    attachment.setUpdatedBy(actorUserId);
    attachment.setActive(false);
    attachmentRepository.save(attachment);

    long total =
        correspondence.getTotalAttachmentBytes() != null ? correspondence.getTotalAttachmentBytes() : 0L;
    correspondence.setTotalAttachmentBytes(Math.max(0L, total - subtract));
    correspondence.setUpdatedBy(actorUserId);
    correspondenceRepository.save(correspondence);

    Map<String, Object> audit = new HashMap<>();
    audit.put("attachmentId", attachmentId);
    audit.put("bytesRemoved", subtract);
    correspondenceActionAudit.log(
        actorUserId,
        CorrespondenceActionAudit.ACTION_ATTACHMENT_DELETE,
        correspondence.getId(),
        audit);

    log.info("AttachmentEntity soft-deleted id={} correspondence={}", attachmentId, correspondence.getId());
  }
}
