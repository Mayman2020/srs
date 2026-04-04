package com.gov.ac.attachment;

import com.gov.ac.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.domain.correspondence.Attachment;
import com.gov.ac.domain.correspondence.AttachmentVersion;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.AttachmentRepository;
import com.gov.ac.persistence.AttachmentVersionRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
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

  @Transactional(readOnly = true)
  public ResponseEntity<StreamingResponseBody> download(Long attachmentId, UUID viewerId) {
    AppUser viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn("Attachment download denied: unknown viewer userId={}", viewerId);
                  return new ForbiddenException("You do not have access to this attachment");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this attachment");
    }

    Attachment attachment =
        attachmentRepository
            .findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(attachmentId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));

    Correspondence correspondence = attachment.getCorrespondence();
    if (correspondence.getDeletedAt() != null) {
      throw new NotFoundException("Attachment not found");
    }

    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    Long currentVersionId = attachment.getCurrentVersionId();
    if (currentVersionId == null) {
      throw new NotFoundException("Attachment not found");
    }

    AttachmentVersion version =
        attachmentVersionRepository
            .findByIdAndDeletedAtIsNullWithAttachment(currentVersionId)
            .orElseThrow(() -> new NotFoundException("Attachment not found"));

    if (!version.getAttachment().getId().equals(attachment.getId())) {
      throw new NotFoundException("Attachment not found");
    }

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
    if (blob.byteSize() >= 0) {
      headers.setContentLength(blob.byteSize());
    }

    StreamingResponseBody body =
        outputStream -> {
          try {
            blob.writeBody(attachmentContentStore, outputStream);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        };

    return ResponseEntity.ok().headers(headers).body(body);
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
