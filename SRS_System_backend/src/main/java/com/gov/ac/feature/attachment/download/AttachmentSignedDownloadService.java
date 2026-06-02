package com.gov.ac.feature.attachment.download;

import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.service.AttachmentAccessLogService;
import com.gov.ac.feature.attachment.crypto.AttachmentDecryptionService;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.service.AttachmentContentStore;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Streams the decrypted attachment bytes after validating the single-use download token. Lives
 * here (not inside the controller) so that {@code ModuleBoundaryArchTest.controllersDoNotDependOnEntities}
 * stays clean — controllers carry only DTOs / primitives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentSignedDownloadService {

  private final AttachmentDownloadTokenService tokenService;
  private final AttachmentDecryptionService decryptionService;
  private final AttachmentContentStore contentStore;
  private final AttachmentAccessLogService accessLogService;

  public ResponseEntity<StreamingResponseBody> streamForToken(
      String token, UUID userId, HttpServletRequest request) {
    AttachmentVersionEntity version = tokenService.consume(token, userId, request);
    AttachmentEntity attachment = version.getAttachment();
    String displayName = attachment != null ? attachment.getDisplayName() : "download";
    String mime =
        version.getMimeType() != null && !version.getMimeType().isBlank()
            ? version.getMimeType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(displayName));
    headers.setContentType(safeMediaType(mime));
    boolean encrypted = AttachmentDecryptionService.isEncrypted(version);
    if (!encrypted && version.getByteSize() != null && version.getByteSize() >= 0) {
      headers.setContentLength(version.getByteSize());
    }

    StreamingResponseBody body =
        out -> {
          try {
            if (encrypted) {
              decryptionService.streamPlaintext(version, out);
            } else {
              contentStore.copyToOutputStream(version.getStorageKey(), out);
            }
            try {
              accessLogService.record(
                  version, userId, AttachmentAccessLogEntity.ACTION_DOWNLOAD, true, request);
            } catch (RuntimeException ex) {
              log.warn(
                  "Attachment access log write failed (success path) versionId={}: {}",
                  version.getId(),
                  ex.getMessage());
            }
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
