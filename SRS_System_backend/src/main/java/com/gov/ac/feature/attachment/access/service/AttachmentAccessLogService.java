package com.gov.ac.feature.attachment.access.service;

import com.gov.ac.feature.attachment.access.dto.AttachmentAccessLogDto;
import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.repository.AttachmentAccessLogRepository;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only access log for attachments. Writes use {@link Propagation#REQUIRES_NEW} so that a
 * logging failure cannot poison the outer streaming-download transaction. Callers wrap
 * invocations in a try/catch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentAccessLogService {

  static final String AUDIT_ACTION_DOWNLOADED = "ATTACHMENT_DOWNLOADED";

  private static final int MAX_IP_LENGTH = 64;
  private static final int MAX_UA_LENGTH = 512;

  private final AttachmentAccessLogRepository accessLogRepository;
  private final AppUserRepository appUserRepository;
  private final AuditTrailService auditTrailService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      AttachmentVersionEntity version,
      UUID userId,
      String actionCode,
      boolean success,
      HttpServletRequest request) {
    if (version == null || userId == null || actionCode == null || actionCode.isBlank()) {
      return;
    }
    AttachmentEntity attachment = version.getAttachment();
    if (attachment == null) {
      return;
    }
    CorrespondenceEntity correspondence = attachment.getCorrespondence();
    if (correspondence == null) {
      return;
    }
    AppUserEntity user = appUserRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
    if (user == null) {
      return;
    }

    Instant now = Instant.now();
    String ip = truncate(extractClientIp(request), MAX_IP_LENGTH);
    String ua = truncate(extractUserAgent(request), MAX_UA_LENGTH);

    AttachmentAccessLogEntity row = new AttachmentAccessLogEntity();
    row.setAttachmentVersion(version);
    row.setAttachment(attachment);
    row.setCorrespondence(correspondence);
    row.setUser(user);
    row.setActionCode(actionCode);
    row.setOccurredAt(now);
    row.setSuccess(success);
    row.setIpAddress(ip);
    row.setUserAgent(ua);
    accessLogRepository.save(row);

    if (success && AttachmentAccessLogEntity.ACTION_DOWNLOAD.equals(actionCode)) {
      auditTrailService.append(
          new CreateAuditEventRequestDto(
              userId.toString(),
              AUDIT_ACTION_DOWNLOADED,
              "ATTACHMENT",
              version.getId().toString(),
              null,
              ip,
              ua,
              now));
    }
  }

  @Transactional(readOnly = true)
  public List<AttachmentAccessLogDto> listForAttachment(Long attachmentId) {
    return accessLogRepository.findRecentByAttachmentId(attachmentId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AttachmentAccessLogDto> listForCorrespondence(UUID correspondenceId) {
    return accessLogRepository.findRecentByCorrespondenceId(correspondenceId).stream()
        .map(this::toDto)
        .toList();
  }

  private AttachmentAccessLogDto toDto(AttachmentAccessLogEntity row) {
    AppUserEntity u = row.getUser();
    return new AttachmentAccessLogDto(
        row.getId(),
        row.getAttachment() != null ? row.getAttachment().getId() : null,
        row.getAttachmentVersion() != null ? row.getAttachmentVersion().getId() : null,
        row.getCorrespondence() != null ? row.getCorrespondence().getId() : null,
        u != null ? u.getId() : null,
        u != null ? u.getUsername() : null,
        u != null ? u.getFullNameAr() : null,
        u != null ? u.getFullNameEn() : null,
        row.getActionCode(),
        row.getOccurredAt(),
        row.getIpAddress(),
        row.getUserAgent(),
        row.isSuccess());
  }

  /** Resolves the best-effort client IP, preferring the first hop in {@code X-Forwarded-For}. */
  static String extractClientIp(HttpServletRequest request) {
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

  static String extractUserAgent(HttpServletRequest request) {
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
