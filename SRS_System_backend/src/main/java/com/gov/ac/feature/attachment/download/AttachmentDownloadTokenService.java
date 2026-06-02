package com.gov.ac.feature.attachment.download;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.service.AttachmentAccessLogService;
import com.gov.ac.feature.attachment.download.dto.AttachmentDownloadIntentDto;
import com.gov.ac.feature.attachment.download.entity.AttachmentDownloadTokenEntity;
import com.gov.ac.feature.attachment.download.repository.AttachmentDownloadTokenRepository;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and consumes single-use, short-TTL download tokens.
 *
 * <p>The raw token (32 random bytes, URL-safe Base64) is returned exactly once at issuance; the
 * server stores only its SHA-256 hash. {@link #consume(String, UUID, HttpServletRequest)} runs
 * the same clearance check as the legacy GET endpoint, asserts the row is unconsumed and unexpired,
 * and stamps {@code consumed_at} so a replayed token fails atomically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentDownloadTokenService {

  private static final int TOKEN_BYTES = 32;

  private final AttachmentDownloadTokenRepository tokenRepository;
  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AttachmentAccessLogService accessLogService;
  private final AttachmentDownloadTokenProperties properties;
  private final SecureRandom random = new SecureRandom();

  /** Issues a fresh single-use token for the given attachment, on behalf of {@code userId}. */
  @Transactional
  public AttachmentDownloadIntentDto issue(Long attachmentId, UUID userId, HttpServletRequest request) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ForbiddenException("You do not have access to this attachment"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this attachment");
    }

    AttachmentEntity attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));
    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence == null || correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Attachment not found");
    }
    // Clearance / department-scope check identical to the legacy GET.
    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

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

    byte[] raw = new byte[TOKEN_BYTES];
    random.nextBytes(raw);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    String hash = sha256Hex(token);

    Instant now = Instant.now();
    Instant expires = now.plus(Duration.ofSeconds(properties.ttlSeconds()));

    AttachmentDownloadTokenEntity row = new AttachmentDownloadTokenEntity();
    row.setTokenHash(hash);
    row.setAttachment(attachment);
    row.setAttachmentVersion(version);
    row.setUserId(userId);
    row.setIssuedAt(now);
    row.setExpiresAt(expires);
    row.setIpAddress(truncate(extractClientIp(request), 64));
    row.setUserAgent(truncate(extractUserAgent(request), 512));
    row.setCreatedBy(userId);
    row.setUpdatedBy(userId);
    tokenRepository.save(row);

    return new AttachmentDownloadIntentDto(token, expires);
  }

  /**
   * Validates a presented token, flips {@code consumed_at}, and returns the resolved version.
   *
   * <p>Every failure (unknown / expired / consumed / user mismatch / revoked) writes a
   * {@code success=false} row to {@code attachment_access_log} when enough context is known.
   */
  @Transactional
  public AttachmentVersionEntity consume(String token, UUID userId, HttpServletRequest request) {
    if (token == null || token.isBlank()) {
      throw new BadRequestException("Token is required");
    }
    String hash = sha256Hex(token);
    AttachmentDownloadTokenEntity row =
        tokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> new NotFoundException("Unknown or expired download token"));

    AttachmentVersionEntity version = row.getAttachmentVersion();
    try {
      if (row.getRevokedAt() != null) {
        throw new ForbiddenException("Download token is revoked");
      }
      if (row.getConsumedAt() != null) {
        throw new ForbiddenException("Download token already used");
      }
      Instant now = Instant.now();
      if (row.getExpiresAt() == null || row.getExpiresAt().isBefore(now)) {
        throw new ForbiddenException("Download token expired");
      }
      if (!row.getUserId().equals(userId)) {
        throw new ForbiddenException("Download token does not belong to the caller");
      }
      row.setConsumedAt(now);
      row.setIpAddress(truncate(extractClientIp(request), 64));
      row.setUserAgent(truncate(extractUserAgent(request), 512));
      row.setUpdatedBy(userId);
      tokenRepository.save(row);
      return version;
    } catch (ForbiddenException | NotFoundException ex) {
      recordFailure(version, userId, request, ex.getMessage());
      throw ex;
    }
  }

  /** Cleanup job hook: deletes tokens whose {@code expires_at + 1h} is in the past. */
  @Transactional
  public int purgeExpired() {
    Instant cutoff = Instant.now().minus(Duration.ofHours(1));
    int n = tokenRepository.deleteExpiredOlderThan(cutoff);
    if (n > 0) {
      log.info("AttachmentDownloadTokenService purged {} expired token row(s)", n);
    }
    return n;
  }

  private void recordFailure(
      AttachmentVersionEntity version, UUID userId, HttpServletRequest request, String reason) {
    if (version == null || userId == null) {
      return;
    }
    try {
      accessLogService.record(
          version, userId, AttachmentAccessLogEntity.ACTION_DOWNLOAD, false, request);
    } catch (RuntimeException ex) {
      log.warn("Attachment access log (failure path) write failed: {}", ex.getMessage());
    }
    log.warn(
        "Attachment download rejected userId={} versionId={} reason={}",
        userId,
        version.getId(),
        reason);
  }

  private static String sha256Hex(String value) {
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
