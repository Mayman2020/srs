package com.gov.ac.feature.attachment.verification;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.GoneException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.attachment.signature.entity.DocumentSignatureEntity;
import com.gov.ac.feature.attachment.signature.repository.DocumentSignatureRepository;
import com.gov.ac.feature.attachment.verification.dto.AttachmentPublicVerificationDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentPublicVerificationDto.PublicSignatureDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenIssueRequestDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenIssuedDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenSummaryDto;
import com.gov.ac.feature.attachment.verification.entity.AttachmentVerificationAccessLogEntity;
import com.gov.ac.feature.attachment.verification.entity.AttachmentVerificationTokenEntity;
import com.gov.ac.feature.attachment.verification.repository.AttachmentVerificationAccessLogRepository;
import com.gov.ac.feature.attachment.verification.repository.AttachmentVerificationTokenRepository;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues / revokes / lists long-lived verification tokens and serves the scrubbed public projection
 * for QR scans.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentVerificationTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final String AUDIT_ISSUED = "ATTACHMENT_VERIFICATION_TOKEN_ISSUED";
  private static final String AUDIT_REVOKED = "ATTACHMENT_VERIFICATION_TOKEN_REVOKED";
  private static final String RESOURCE_TYPE = "ATTACHMENT_VERIFICATION_TOKEN";

  private final AttachmentVerificationTokenRepository tokenRepository;
  private final AttachmentVerificationAccessLogRepository accessLogRepository;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final DocumentSignatureRepository documentSignatureRepository;
  private final AuditTrailService auditTrailService;
  private final AttachmentVerificationProperties verificationProperties;
  private final SecureRandom random = new SecureRandom();

  @Transactional
  public AttachmentVerificationTokenIssuedDto issue(
      Long attachmentId,
      UUID actorUserId,
      HttpServletRequest request,
      AttachmentVerificationTokenIssueRequestDto body) {
    AppUserEntity actor = requireActiveUser(actorUserId);

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
    if (!version.getAttachment().getId().equals(attachment.getId())) {
      throw new NotFoundException("Attachment not found");
    }

    boolean permanent = body != null && Boolean.TRUE.equals(body.permanent());
    Integer ttlDays = body != null ? body.ttlDays() : null;
    Instant issuedAt = Instant.now();
    Instant expiresAt;
    if (permanent) {
      expiresAt = null;
    } else if (ttlDays != null) {
      if (ttlDays <= 0) {
        throw new BadRequestException("ttlDays must be positive when permanent=false");
      }
      expiresAt = issuedAt.plus(ttlDays.longValue(), ChronoUnit.DAYS);
    } else {
      expiresAt = issuedAt.plus(verificationProperties.defaultTtlDays().longValue(), ChronoUnit.DAYS);
    }

    byte[] raw = new byte[TOKEN_BYTES];
    random.nextBytes(raw);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    String hash = sha256Hex(token);

    AttachmentVerificationTokenEntity row = new AttachmentVerificationTokenEntity();
    row.setTokenHash(hash);
    row.setAttachment(attachment);
    row.setAttachmentVersion(version);
    row.setIssuedBy(actorUserId);
    row.setIssuedAt(issuedAt);
    row.setExpiresAt(expiresAt);
    row.setAccessCount(0);
    row.setCreatedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    AttachmentVerificationTokenEntity saved = tokenRepository.save(row);

    audit(actorUserId, AUDIT_ISSUED, saved.getId(), version.getId());
    return new AttachmentVerificationTokenIssuedDto(
        saved.getId(), version.getId(), token, saved.getIssuedAt(), saved.getExpiresAt());
  }

  @Transactional(readOnly = true)
  public List<AttachmentVerificationTokenSummaryDto> listForAttachment(Long attachmentId, UUID actorUserId) {
    AppUserEntity actor = requireActiveUser(actorUserId);
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
      return List.of();
    }
    return tokenRepository.findByAttachmentVersion_IdOrderByIssuedAtDesc(currentVersionId).stream()
        .map(this::toSummary)
        .toList();
  }

  @Transactional
  public void revoke(Long attachmentId, UUID tokenId, UUID actorUserId) {
    AppUserEntity actor = requireActiveUser(actorUserId);
    AttachmentEntity attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence == null || correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Attachment not found");
    }
    correspondenceViewAuthorization.assertCanView(actor, correspondence);

    AttachmentVerificationTokenEntity row =
        tokenRepository
            .findById(tokenId)
            .orElseThrow(() -> new NotFoundException("Verification token not found"));
    if (!row.getAttachment().getId().equals(attachmentId)) {
      throw new NotFoundException("Verification token not found");
    }
    if (row.getRevokedAt() != null) {
      return;
    }
    row.setRevokedAt(Instant.now());
    row.setRevokedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    tokenRepository.save(row);

    audit(actorUserId, AUDIT_REVOKED, row.getId(), row.getAttachmentVersion().getId());
  }

  /**
   * Called by the public controller when the in-memory rate limiter fires, so the forensic log
   * still captures the attempt.
   */
  @Transactional
  public void recordRateLimited(String rawToken, HttpServletRequest request) {
    String hash = rawToken == null || rawToken.isBlank() ? sha256Hex("") : sha256Hex(rawToken);
    appendAccessLog(null, hash, null, request, false, AttachmentVerificationAccessLogEntity.REASON_RATE_LIMITED);
  }

  /**
   * Public path: validates the raw token, increments {@code access_count}, appends an access-log
   * row, and returns the scrubbed projection. Caller is responsible for rate limiting before this
   * method runs.
   */
  @Transactional
  public AttachmentPublicVerificationDto verifyPublic(String rawToken, HttpServletRequest request) {
    if (rawToken == null || rawToken.isBlank()) {
      appendAccessLog(null, sha256Hex(rawToken), null, request, false, AttachmentVerificationAccessLogEntity.REASON_UNKNOWN);
      throw new BadRequestException("Token is required");
    }
    String hash = sha256Hex(rawToken);
    AttachmentVerificationTokenEntity row =
        tokenRepository.findByTokenHash(hash).orElse(null);
    if (row == null) {
      appendAccessLog(null, hash, null, request, false, AttachmentVerificationAccessLogEntity.REASON_UNKNOWN);
      throw new NotFoundException("Unknown verification token");
    }
    Long versionId = row.getAttachmentVersion().getId();
    if (row.getRevokedAt() != null) {
      appendAccessLog(versionId, hash, row, request, false, AttachmentVerificationAccessLogEntity.REASON_REVOKED);
      throw new GoneException("Verification token is revoked");
    }
    Instant now = Instant.now();
    if (row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now)) {
      appendAccessLog(versionId, hash, row, request, false, AttachmentVerificationAccessLogEntity.REASON_EXPIRED);
      throw new GoneException("Verification token expired");
    }

    row.setAccessCount(row.getAccessCount() == null ? 1 : row.getAccessCount() + 1);
    row.setLastAccessedAt(now);
    tokenRepository.save(row);

    appendAccessLog(versionId, hash, row, request, true, AttachmentVerificationAccessLogEntity.REASON_OK);

    AttachmentVersionEntity version = row.getAttachmentVersion();
    CorrespondenceEntity corr = version.getAttachment().getCorrespondence();
    List<DocumentSignatureEntity> sigs =
        documentSignatureRepository.findByAttachmentVersionId(version.getId());

    List<PublicSignatureDto> publicSigs =
        sigs.stream()
            .map(
                s -> {
                  AppUserEntity signer = s.getSigner();
                  String display =
                      signer != null && signer.getFullNameEn() != null && !signer.getFullNameEn().isBlank()
                          ? signer.getFullNameEn()
                          : (signer != null ? signer.getUsername() : "?");
                  return new PublicSignatureDto(
                      display,
                      s.getAlgorithm(),
                      s.getSignedAt(),
                      s.getStatus(),
                      s.getVerificationStatus());
                })
            .toList();

    return new AttachmentPublicVerificationDto(
        version.getId(),
        version.getPlaintextSha256(),
        version.getEncryptionAlgo(),
        row.getIssuedAt(),
        corr != null ? corr.getReferenceNumber() : null,
        resolveOrganizationLabel(corr),
        publicSigs);
  }

  private AttachmentVerificationTokenSummaryDto toSummary(AttachmentVerificationTokenEntity e) {
    return new AttachmentVerificationTokenSummaryDto(
        e.getId(),
        e.getAttachmentVersion().getId(),
        e.getIssuedBy(),
        e.getIssuedAt(),
        e.getExpiresAt(),
        e.getRevokedAt(),
        e.getRevokedBy(),
        e.getAccessCount(),
        e.getLastAccessedAt());
  }

  private static String resolveOrganizationLabel(CorrespondenceEntity corr) {
    if (corr == null) {
      return null;
    }
    OrganizationEntity rec = corr.getRecipientOrganization();
    if (rec != null && rec.getNameEn() != null && !rec.getNameEn().isBlank()) {
      return rec.getNameEn();
    }
    OrganizationEntity snd = corr.getSenderOrganization();
    if (snd != null && snd.getNameEn() != null && !snd.getNameEn().isBlank()) {
      return snd.getNameEn();
    }
    DepartmentEntity dept = corr.getOwnerDepartment();
    if (dept != null && dept.getNameEn() != null && !dept.getNameEn().isBlank()) {
      return dept.getNameEn();
    }
    return null;
  }

  private void appendAccessLog(
      Long versionId,
      String tokenHash,
      AttachmentVerificationTokenEntity row,
      HttpServletRequest request,
      boolean success,
      String failureReason) {
    try {
      AttachmentVerificationAccessLogEntity logRow = new AttachmentVerificationAccessLogEntity();
      logRow.setTokenHash(tokenHash);
      logRow.setAttachmentVersionId(versionId);
      logRow.setAccessedAt(Instant.now());
      logRow.setIpAddress(truncate(extractClientIp(request), 64));
      logRow.setUserAgent(truncate(extractUserAgent(request), 512));
      logRow.setSuccess(success);
      logRow.setFailureReason(success ? null : failureReason);
      accessLogRepository.save(logRow);
    } catch (RuntimeException ex) {
      log.warn("attachment_verification_access_log write failed: {}", ex.getMessage());
    }
  }

  private AppUserEntity requireActiveUser(UUID userId) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ForbiddenException("You do not have access"));
    if (!Boolean.TRUE.equals(u.getActive())) {
      throw new ForbiddenException("You do not have access");
    }
    return u;
  }

  private void audit(UUID actorUserId, String actionCode, UUID tokenId, Long versionId) {
    try {
      auditTrailService.append(
          new CreateAuditEventRequestDto(
              actorUserId != null ? actorUserId.toString() : null,
              actionCode,
              RESOURCE_TYPE,
              tokenId.toString(),
              "{\"attachmentVersionId\":" + versionId + "}",
              null,
              null,
              Instant.now()));
    } catch (RuntimeException ex) {
      log.warn("Audit append failed for {} tokenId={}: {}", actionCode, tokenId, ex.getMessage());
    }
  }

  public static String sha256Hex(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(out.length * 2);
      for (byte b : out) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private static String extractClientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }

  private static String extractUserAgent(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    return request.getHeader("User-Agent");
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
