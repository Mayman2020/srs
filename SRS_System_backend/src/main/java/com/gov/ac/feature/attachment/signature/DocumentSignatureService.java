package com.gov.ac.feature.attachment.signature;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.attachment.crypto.AttachmentDecryptionService;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.attachment.service.AttachmentContentStore;
import com.gov.ac.feature.attachment.signature.dto.AttachmentVerificationDto;
import com.gov.ac.feature.attachment.signature.dto.DocumentSignatureDto;
import com.gov.ac.feature.attachment.signature.entity.DocumentSignatureEntity;
import com.gov.ac.feature.attachment.signature.repository.DocumentSignatureRepository;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates / verifies / revokes per-attachment-version digital signatures.
 *
 * <p>Signing re-streams the plaintext (decrypting on the fly when needed), recomputes its SHA-256
 * digest, asserts it matches the stored {@code plaintext_sha256} (tamper guard), then delegates
 * to the {@link SigningKeyProvider} SPI. Verification re-runs the same digest + SPI verify and
 * flips {@code verification_status}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSignatureService {

  private static final String AUDIT_SIGN_CREATED = "ATTACHMENT_SIGNATURE_CREATED";
  private static final String AUDIT_SIGN_VERIFIED = "ATTACHMENT_SIGNATURE_VERIFIED";
  private static final String AUDIT_SIGN_REVOKED = "ATTACHMENT_SIGNATURE_REVOKED";
  private static final String RESOURCE_TYPE = "ATTACHMENT_SIGNATURE";

  private final DocumentSignatureRepository signatureRepository;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AttachmentDecryptionService decryptionService;
  private final AttachmentContentStore contentStore;
  private final SigningKeyProvider signingKeyProvider;
  private final AuditTrailService auditTrailService;

  @Transactional
  public DocumentSignatureDto create(UUID actorUserId, Long attachmentId) {
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot sign this attachment"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot sign this attachment");
    }

    AttachmentEntity attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence == null || correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Attachment not found");
    }
    correspondenceViewAuthorization.assertCanView(actor, correspondence);

    Long currentVersionId = attachment.getCurrentVersionId();
    if (currentVersionId == null) {
      throw new NotFoundException("Attachment not found");
    }
    AttachmentVersionEntity version =
        attachmentVersionRepository
            .findByIdAndDeletedAtIsNullWithAttachment(currentVersionId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));

    String plaintextHash = computeAndVerifyPlaintextHash(version);

    if (signatureRepository
        .findActiveByVersionAndSigner(version.getId(), actorUserId)
        .isPresent()) {
      throw new BadRequestException("Attachment is already signed by you");
    }

    SigningKeyProvider.SignatureResult result = signingKeyProvider.sign(hexToBytes(plaintextHash));

    DocumentSignatureEntity row = new DocumentSignatureEntity();
    row.setAttachmentVersion(version);
    row.setSigner(actor);
    row.setAlgorithm(result.algorithm());
    row.setCanonicalHashSha256(plaintextHash);
    row.setSignatureBytes(result.signatureBytes());
    row.setKeyRef(result.keyRef());
    row.setCertificatePem(result.certificatePem());
    row.setSignedAt(Instant.now());
    row.setStatus(DocumentSignatureEntity.STATUS_VALID);
    row.setVerificationStatus(DocumentSignatureEntity.VERIFICATION_VERIFIED);
    row.setVerificationAt(row.getSignedAt());
    row.setCreatedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    DocumentSignatureEntity saved = signatureRepository.save(row);

    audit(actorUserId, AUDIT_SIGN_CREATED, saved.getId(), version.getId());
    return toDto(saved);
  }

  @Transactional(readOnly = true)
  public List<DocumentSignatureDto> listForAttachment(UUID actorUserId, Long attachmentId) {
    AttachmentEntity attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence == null || correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Attachment not found");
    }
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot view this attachment"));
    correspondenceViewAuthorization.assertCanView(actor, correspondence);

    Long currentVersionId = attachment.getCurrentVersionId();
    if (currentVersionId == null) {
      return List.of();
    }
    return signatureRepository.findByAttachmentVersionId(currentVersionId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public DocumentSignatureDto verify(UUID actorUserId, UUID signatureId) {
    DocumentSignatureEntity row =
        signatureRepository
            .findByIdLoaded(signatureId)
            .orElseThrow(() -> new NotFoundException("Signature not found"));
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot verify this signature"));
    AttachmentVersionEntity version = row.getAttachmentVersion();
    AttachmentEntity attachment = version.getAttachment();
    correspondenceViewAuthorization.assertCanView(actor, attachment.getCorrespondence());

    String currentHash = computePlaintextHashRaw(version);
    boolean hashMatches = currentHash.equalsIgnoreCase(row.getCanonicalHashSha256());
    boolean spiOk =
        hashMatches
            && signingKeyProvider.verify(
                hexToBytes(row.getCanonicalHashSha256()),
                row.getSignatureBytes(),
                row.getKeyRef(),
                row.getAlgorithm());

    row.setVerificationStatus(
        spiOk ? DocumentSignatureEntity.VERIFICATION_VERIFIED : DocumentSignatureEntity.VERIFICATION_FAILED);
    row.setVerificationAt(Instant.now());
    row.setVerificationDetail(
        spiOk
            ? null
            : (!hashMatches ? "Plaintext hash drift detected" : "Signature failed cryptographic verification"));
    row.setUpdatedBy(actorUserId);
    signatureRepository.save(row);

    audit(actorUserId, AUDIT_SIGN_VERIFIED, row.getId(), version.getId());
    return toDto(row);
  }

  @Transactional
  public DocumentSignatureDto revoke(UUID actorUserId, UUID signatureId) {
    DocumentSignatureEntity row =
        signatureRepository
            .findByIdLoaded(signatureId)
            .orElseThrow(() -> new NotFoundException("Signature not found"));
    if (DocumentSignatureEntity.STATUS_REVOKED.equals(row.getStatus())) {
      return toDto(row);
    }
    row.setStatus(DocumentSignatureEntity.STATUS_REVOKED);
    row.setRevokedAt(Instant.now());
    row.setRevokedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    signatureRepository.save(row);

    audit(actorUserId, AUDIT_SIGN_REVOKED, row.getId(), row.getAttachmentVersion().getId());
    return toDto(row);
  }

  /** True iff the user has a VALID + VERIFIED signature on the latest version of the attachment. */
  @Transactional(readOnly = true)
  public boolean hasValidSignatureByUser(Long attachmentVersionId, UUID userId) {
    return signatureRepository
        .findActiveByVersionAndSigner(attachmentVersionId, userId)
        .filter(
            s ->
                DocumentSignatureEntity.STATUS_VALID.equals(s.getStatus())
                    && DocumentSignatureEntity.VERIFICATION_VERIFIED.equals(s.getVerificationStatus()))
        .isPresent();
  }

  @Transactional(readOnly = true)
  public AttachmentVerificationDto verifierProjection(Long attachmentVersionId) {
    AttachmentVersionEntity version =
        attachmentVersionRepository
            .findByIdAndDeletedAtIsNullWithAttachment(attachmentVersionId)
            .orElseThrow(() -> new NotFoundException("Attachment version not found"));
    List<DocumentSignatureDto> signatures =
        signatureRepository.findByAttachmentVersionId(version.getId()).stream()
            .map(this::toDto)
            .toList();
    return new AttachmentVerificationDto(
        version.getId(),
        version.getPlaintextSha256(),
        version.getEncryptionAlgo(),
        signatures);
  }

  private String computeAndVerifyPlaintextHash(AttachmentVersionEntity version) {
    String hex = computePlaintextHashRaw(version);
    String expected = version.getPlaintextSha256();
    if (expected != null && !expected.isBlank() && !expected.equalsIgnoreCase(hex)) {
      throw new BadRequestException(
          "Plaintext hash does not match stored attachment_version.plaintext_sha256 — refusing to sign tampered data");
    }
    if (expected == null || expected.isBlank()) {
      version.setPlaintextSha256(hex);
      attachmentVersionRepository.save(version);
    }
    return hex;
  }

  private String computePlaintextHashRaw(AttachmentVersionEntity version) {
    try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
      if (AttachmentDecryptionService.isEncrypted(version)) {
        decryptionService.streamPlaintext(version, sink);
      } else {
        contentStore.copyToOutputStream(version.getStorageKey(), sink);
      }
      byte[] plaintext = sink.toByteArray();
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(plaintext);
      java.util.Arrays.fill(plaintext, (byte) 0);
      return toHex(hash);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to compute plaintext hash", e);
    }
  }

  private DocumentSignatureDto toDto(DocumentSignatureEntity row) {
    AppUserEntity signer = row.getSigner();
    AttachmentVersionEntity version = row.getAttachmentVersion();
    Long attachmentId =
        version != null && version.getAttachment() != null ? version.getAttachment().getId() : null;
    return new DocumentSignatureDto(
        row.getId(),
        attachmentId,
        version != null ? version.getId() : null,
        signer != null ? signer.getId() : null,
        signer != null ? signer.getUsername() : null,
        signer != null ? signer.getFullNameAr() : null,
        signer != null ? signer.getFullNameEn() : null,
        row.getAlgorithm(),
        row.getCanonicalHashSha256(),
        row.getKeyRef(),
        row.getSignedAt(),
        row.getStatus(),
        row.getVerificationStatus(),
        row.getVerificationAt(),
        row.getVerificationDetail());
  }

  private void audit(UUID actorUserId, String actionCode, UUID signatureId, Long versionId) {
    try {
      auditTrailService.append(
          new CreateAuditEventRequestDto(
              actorUserId != null ? actorUserId.toString() : null,
              actionCode,
              RESOURCE_TYPE,
              signatureId.toString(),
              "{\"attachmentVersionId\":" + versionId + "}",
              null,
              null,
              Instant.now()));
    } catch (RuntimeException ex) {
      log.warn("Audit append failed for {} signatureId={}: {}", actionCode, signatureId, ex.getMessage());
    }
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) | Character.digit(hex.charAt(i + 1), 16));
    }
    return out;
  }
}
