package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.CorrespondenceAggregateLimits;
import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentDetailDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceAttachmentFormDto;
import com.gov.ac.feature.correspondence.mapper.CorrespondenceDetailMapper;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import java.util.Base64;
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
      UUID correspondenceId, UUID actorUserId, CorrespondenceAttachmentFormDto form) {
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot modify this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot modify this correspondence");
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));

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

    AttachmentContentTypeEntity contentType = null;
    if (StringUtils.hasText(form.getContentTypeCode())) {
      contentType = lookups.requireActiveAttachmentContentType(form.getContentTypeCode());
    }

    AttachmentEntity attachment = new AttachmentEntity();
    attachment.setCorrespondence(correspondence);
    attachment.setContentType(contentType);
    attachment.setDisplayName(form.getDisplayName().trim());
    attachment.setCreatedBy(actorUserId);
    attachment.setUpdatedBy(actorUserId);
    attachment = attachmentRepository.saveAndFlush(attachment);

    AttachmentVersionEntity version = new AttachmentVersionEntity();
    version.setAttachment(attachment);
    version.setVersionNumber(1);
    version.setStorageKey(form.getStorageKey().trim());
    version.setByteSize(form.getByteSize());
    version.setMimeType(trimToNull(form.getMimeType()));
    version.setChecksumSha256(trimToNull(form.getChecksumSha256()));
    applyEncryptionMetadata(version, form);
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

  private void validateForm(CorrespondenceAttachmentFormDto form) {
    if (form.getByteSize() == null || form.getByteSize() < 0) {
      throw new BadRequestException("Invalid attachment size");
    }
    if (!StringUtils.hasText(form.getContentTypeCode())) {
      return;
    }
    AttachmentContentTypeEntity ct = lookups.requireActiveAttachmentContentType(form.getContentTypeCode());
    if (ct.getMaxBytes() != null && form.getByteSize() > ct.getMaxBytes()) {
      throw new BadRequestException("AttachmentEntity exceeds max size for content type " + ct.getCode());
    }
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    return s.trim();
  }

  /**
   * Slice 5: copy the at-rest encryption envelope (algo + wrapped DEK + IV + digests) from the
   * upload response onto the freshly created {@link AttachmentVersionEntity}. The fields are
   * optional so legacy clients that skip the upload encryption path still work — but if any one
   * of {@code encryptionAlgo} / {@code encryptionWrappedDekB64} / {@code encryptionIvB64} is
   * provided, all three must be present and well-formed.
   */
  private static void applyEncryptionMetadata(
      AttachmentVersionEntity version, CorrespondenceAttachmentFormDto form) {
    boolean any =
        StringUtils.hasText(form.getEncryptionAlgo())
            || StringUtils.hasText(form.getEncryptionWrappedDekB64())
            || StringUtils.hasText(form.getEncryptionIvB64());
    if (!any) {
      if (StringUtils.hasText(form.getPlaintextSha256())) {
        version.setPlaintextSha256(form.getPlaintextSha256().trim());
      }
      return;
    }
    if (!StringUtils.hasText(form.getEncryptionAlgo())
        || !StringUtils.hasText(form.getEncryptionWrappedDekB64())
        || !StringUtils.hasText(form.getEncryptionIvB64())) {
      throw new BadRequestException(
          "encryptionAlgo, encryptionWrappedDekB64 and encryptionIvB64 must all be provided");
    }
    Base64.Decoder b64 = Base64.getDecoder();
    byte[] wrapped;
    byte[] iv;
    try {
      wrapped = b64.decode(form.getEncryptionWrappedDekB64());
      iv = b64.decode(form.getEncryptionIvB64());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid Base64 in encryption envelope");
    }
    version.setEncryptionAlgo(form.getEncryptionAlgo().trim());
    version.setEncryptionKeyRef(trimToNull(form.getEncryptionKeyRef()));
    version.setEncryptionWrappedDek(wrapped);
    version.setEncryptionIv(iv);
    version.setCiphertextSha256(trimToNull(form.getCiphertextSha256()));
    String plaintextHash =
        StringUtils.hasText(form.getPlaintextSha256())
            ? form.getPlaintextSha256().trim()
            : trimToNull(form.getChecksumSha256());
    version.setPlaintextSha256(plaintextHash);
  }
}
