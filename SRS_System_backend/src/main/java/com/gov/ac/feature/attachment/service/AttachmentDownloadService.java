package com.gov.ac.feature.attachment.service;

import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.service.AttachmentAccessLogService;
import com.gov.ac.feature.attachment.crypto.AttachmentDecryptionService;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentDownloadService {

  private final AttachmentRepository attachmentRepository;
  private final AttachmentVersionRepository attachmentVersionRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AttachmentContentStore attachmentContentStore;
  private final AttachmentAccessLogService attachmentAccessLogService;
  private final AttachmentDecryptionService attachmentDecryptionService;

  @Transactional(readOnly = true)
  public ResponseEntity<StreamingResponseBody> download(Long attachmentId, UUID viewerId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn("AttachmentEntity download denied: unknown viewer userId={}", viewerId);
                  return new ForbiddenException("You do not have access to this attachment");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this attachment");
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

    Long currentVersionId = attachment.getCurrentVersionId();
    if (currentVersionId == null) {
      throw new NotFoundException("AttachmentEntity not found");
    }

    AttachmentVersionEntity version =
        attachmentVersionRepository
            .findByIdAndDeletedAtIsNullWithAttachment(currentVersionId)
            .orElseThrow(() -> new NotFoundException("AttachmentEntity not found"));

    if (!version.getAttachment().getId().equals(attachment.getId())) {
      throw new NotFoundException("AttachmentEntity not found");
    }

    recordAccess(version, viewerId);

    boolean encrypted = AttachmentDecryptionService.isEncrypted(version);
    AuthorizedAttachmentBlob blob =
        new AuthorizedAttachmentBlob(
            attachment.getDisplayName(),
            version.getMimeType() != null && !version.getMimeType().isBlank()
                ? version.getMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE,
            version.getByteSize(),
            version.getStorageKey());

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(blob.displayName()));
    MediaType mediaType = safeMediaType(blob.mimeType());
    headers.setContentType(mediaType);
    // Content-Length is omitted for encrypted blobs because the on-disk size is ciphertext size.
    if (!encrypted && blob.byteSize() >= 0) {
      headers.setContentLength(blob.byteSize());
    }

    StreamingResponseBody body =
        outputStream -> {
          try {
            if (encrypted) {
              attachmentDecryptionService.streamPlaintext(version, outputStream);
            } else {
              blob.writeBody(attachmentContentStore, outputStream);
            }
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        };

    return ResponseEntity.ok().headers(headers).body(body);
  }

  /**
   * Persist the access-log row and emit the canonical audit event. The access-log service runs
   * in a {@code REQUIRES_NEW} transaction and emits the {@code ATTACHMENT_DOWNLOADED} audit
   * event in the same boundary so that a logging failure can never block the binary stream.
   */
  private void recordAccess(AttachmentVersionEntity version, UUID viewerId) {
    try {
      attachmentAccessLogService.record(
          version,
          viewerId,
          AttachmentAccessLogEntity.ACTION_DOWNLOAD,
          true,
          currentRequestOrNull());
    } catch (RuntimeException ex) {
      log.warn(
          "Attachment access log write failed (non-fatal) attachmentVersionId={} viewerId={}: {}",
          version.getId(),
          viewerId,
          ex.getMessage());
    }
  }

  private static HttpServletRequest currentRequestOrNull() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      return attrs != null ? attrs.getRequest() : null;
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static MediaType safeMediaType(String mime) {
    try {
      return MediaType.parseMediaType(mime);
    } catch (Exception e) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  private static String contentDispositionAttachment(String displayName) {
    String base = displayName != null ? displayName.replace("\"", "'") : "download";
    if (base.isBlank()) {
      base = "download";
    }
    String asciiFallback = base.replaceAll("[^\\x20-\\x7E]", "_");
    if (asciiFallback.isBlank()) {
      asciiFallback = "download";
    }
    String utf8 =
        java.net.URLEncoder.encode(base, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + utf8;
  }
}
