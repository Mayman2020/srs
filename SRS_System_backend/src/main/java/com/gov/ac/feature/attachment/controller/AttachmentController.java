package com.gov.ac.feature.attachment.controller;

import com.gov.ac.feature.attachment.crypto.AttachmentEncryptionProperties;
import com.gov.ac.feature.attachment.crypto.AttachmentEncryptionService;
import com.gov.ac.feature.attachment.crypto.EncryptedBlobMetadata;
import com.gov.ac.feature.attachment.service.AttachmentDeletionService;
import com.gov.ac.feature.attachment.service.AttachmentStorageProperties;
import com.gov.ac.feature.attachment.dto.AttachmentUploadResponseDto;
import com.gov.ac.feature.attachment.mapper.AttachmentMapper;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.common.api.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentDeletionService attachmentDeletionService;
  private final AttachmentStorageProperties storageProperties;
  private final AttachmentEncryptionService attachmentEncryptionService;
  private final AttachmentEncryptionProperties encryptionProperties;

  /**
   * Any user who can create correspondence may upload a binary into the staging area; the
   * attachment is not yet linked to a correspondence at this point so finer-grained authorization
   * happens when the attachment is registered against a correspondence id.
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE')")
  public AttachmentUploadResponseDto upload(
      @RequestPart("file") MultipartFile file,
      @org.springframework.web.bind.annotation.RequestParam(name = "fileCode", required = false)
          String fileCode)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("file is required");
    }
    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      original = "upload.bin";
    }
    original = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    if (original.length() > 200) {
      original = original.substring(original.length() - 200);
    }

    Path root = Paths.get(storageProperties.root()).toAbsolutePath().normalize();
    Files.createDirectories(root);

    UUID userId = SecurityUtils.requireCurrentUserId();
    LocalDate today = LocalDate.now();
    String codePart = "UPLOAD";
    if (fileCode != null && !fileCode.isBlank()) {
      codePart = fileCode.replaceAll("[^a-zA-Z0-9._-]", "_");
      if (codePart.length() > 80) {
        codePart = codePart.substring(codePart.length() - 80);
      }
    }
    String relativeDir =
        "SRS/"
            + userId
            + "/"
            + today.getYear()
            + "/"
            + String.format("%02d", today.getMonthValue())
            + "/"
            + String.format("%02d", today.getDayOfMonth());
    Path dir = root.resolve(relativeDir).normalize();
    if (!dir.startsWith(root)) {
      throw new IllegalStateException("Invalid storage root");
    }
    Files.createDirectories(dir);

    String unique = codePart + "_" + UUID.randomUUID() + "_" + original;
    Path target = dir.resolve(unique).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalStateException("Invalid path");
    }

    String mime =
        file.getContentType() != null && !file.getContentType().isBlank()
            ? file.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String storageKey = relativeDir + "/" + unique;

    if (!Boolean.FALSE.equals(encryptionProperties.enabled())) {
      try (InputStream in = file.getInputStream()) {
        EncryptedBlobMetadata metadata = attachmentEncryptionService.encrypt(in, target);
        return AttachmentMapper.toEncryptedUploadResponse(storageKey, mime, metadata);
      }
    }

    Files.copy(file.getInputStream(), target);
    return AttachmentMapper.toUploadResponse(storageKey, file.getSize(), mime);
  }

  /**
   * Legacy direct download path removed (Slice 6). Returns {@code 410 Gone} with RFC 9457
   * problem+json including a {@code migrateTo} hint for the intent + short-lived token pipeline.
   */
  @Deprecated
  @GetMapping("/{id}/download")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
  public ResponseEntity<ProblemDetail> download(@PathVariable Long id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    org.slf4j.LoggerFactory.getLogger(AttachmentController.class)
        .warn(
            "[deprecation] legacy attachment download attachmentId={} actorUserId={} — migrate to POST /api/v1/attachments/{id}/download-intent then GET /api/v1/attachments/download/{token}",
            id,
            actor);
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.GONE, "Legacy attachment download endpoint has been removed.");
    pd.setTitle("Gone");
    pd.setType(URI.create("urn:problem-type:ac:legacy-attachment-download-removed"));
    pd.setProperty(
        "migrateTo",
        "POST /api/v1/attachments/{id}/download-intent then GET /api/v1/attachments/download/{token}");
    return ResponseEntity.status(HttpStatus.GONE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_UPDATE')")
  public void delete(@PathVariable Long id) {
    attachmentDeletionService.softDelete(id, SecurityUtils.requireCurrentUserId());
  }
}
