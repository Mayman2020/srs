package com.gov.ac.correspondence.service;

import com.gov.ac.correspondence.CorrespondenceAggregateLimits;
import com.gov.ac.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.correspondence.dto.CorrespondenceAttachmentForm;
import com.gov.ac.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.domain.correspondence.Attachment;
import com.gov.ac.domain.correspondence.AttachmentVersion;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.AttachmentContentType;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.lookup.LookupResolutionService;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.AttachmentRepository;
import com.gov.ac.persistence.AttachmentVersionRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CorrespondenceAttachmentMutationService {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final LookupResolutionService lookups;
  private final CorrespondenceDetailMapper correspondenceDetailMapper;
  private final CorrespondenceActionAudit correspondenceActionAudit;

  @Transactional
  public CorrespondenceAttachmentDetailDto addAttachment(
      UUID correspondenceId, UUID actorUserId, CorrespondenceAttachmentForm form) {
    AppUser actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot modify this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot modify this correspondence");
    }

    Correspondence correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("Correspondence not found"));

    correspondenceViewAuthorization.assertCanView(actor, correspondence);
    CorrespondenceMutationGuards.assertCorrespondenceMutable(correspondence);

    validateForm(form);

    long count = attachmentRepository.countByCorrespondence_IdAndDeletedAtIsNull(correspondenceId);
    if (count >= CorrespondenceAggregateLimits.MAX_ATTACHMENTS_COUNT) {
      throw new BadRequestException(
          "Too many attachments (max "
              + CorrespondenceAggregateLimits.MAX_ATTACHMENTS_COUNT
              + " per correspondence)");
    }

    long currentTotal =
        correspondence.getTotalAttachmentBytes() != null ? correspondence.getTotalAttachmentBytes() : 0L;
    if (currentTotal + form.getByteSize() > CorrespondenceAggregateLimits.MAX_TOTAL_ATTACHMENT_BYTES) {
      throw new BadRequestException(
          "Total attachment size would exceed limit of "
              + CorrespondenceAggregateLimits.MAX_TOTAL_ATTACHMENT_BYTES
              + " bytes");
    }

    AttachmentContentType contentType = null;
    if (StringUtils.hasText(form.getContentTypeCode())) {
      contentType = lookups.requireActiveAttachmentContentType(form.getContentTypeCode());
    }

    Attachment attachment = new Attachment();
    attachment.setCorrespondence(correspondence);
    attachment.setContentType(contentType);
    attachment.setDisplayName(form.getDisplayName().trim());
    attachment.setCreatedBy(actorUserId);
    attachment.setUpdatedBy(actorUserId);
    attachment = attachmentRepository.saveAndFlush(attachment);

    AttachmentVersion version = new AttachmentVersion();
    version.setAttachment(attachment);
    version.setVersionNumber(1);
    version.setStorageKey(form.getStorageKey().trim());
    version.setByteSize(form.getByteSize());
    version.setMimeType(trimToNull(form.getMimeType()));
    version.setChecksumSha256(trimToNull(form.getChecksumSha256()));
    version.setCreatedBy(actorUserId);
    version.setUpdatedBy(actorUserId);
    version = attachmentVersionRepository.saveAndFlush(version);

    attachment.setCurrentVersionId(version.getId());
    attachmentRepository.save(attachment);

    correspondence.setTotalAttachmentBytes(currentTotal + form.getByteSize());
    correspondence.setUpdatedBy(actorUserId);
    correspondenceRepository.save(correspondence);

    Map<String, Object> audit = new HashMap<>();
    audit.put("attachmentId", attachment.getId());
    audit.put("displayName", attachment.getDisplayName());
    audit.put("byteSize", form.getByteSize());
    correspondenceActionAudit.log(
        actorUserId, CorrespondenceActionAudit.ACTION_ATTACHMENT_ADD, correspondenceId, audit);

    return correspondenceDetailMapper.toAttachmentDetail(attachment, List.of(version));
  }

  private void validateForm(CorrespondenceAttachmentForm form) {
    if (form.getByteSize() == null || form.getByteSize() < 0) {
      throw new BadRequestException("Invalid attachment size");
    }
    if (!StringUtils.hasText(form.getContentTypeCode())) {
      return;
    }
    AttachmentContentType ct = lookups.requireActiveAttachmentContentType(form.getContentTypeCode());
    if (ct.getMaxBytes() != null && form.getByteSize() > ct.getMaxBytes()) {
      throw new BadRequestException("Attachment exceeds max size for content type " + ct.getCode());
    }
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    return s.trim();
  }
}
