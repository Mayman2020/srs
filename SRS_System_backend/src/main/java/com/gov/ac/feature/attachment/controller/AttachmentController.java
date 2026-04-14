package com.gov.ac.feature.attachment.controller;

import com.gov.ac.feature.attachment.service.AttachmentDeletionService;
import com.gov.ac.feature.attachment.service.AttachmentDownloadService;
import com.gov.ac.feature.attachment.service.AttachmentStorageProperties;
import com.gov.ac.feature.attachment.dto.AttachmentUploadResponseDto;
import com.gov.ac.feature.attachment.mapper.AttachmentMapper;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.common.api.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentDownloadService attachmentDownloadService;
  private final AttachmentDeletionService attachmentDeletionService;
  private final AttachmentStorageProperties storageProperties;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    Files.copy(file.getInputStream(), target);

    String storageKey = relativeDir + "/" + unique;
    String mime =
        file.getContentType() != null && !file.getContentType().isBlank()
            ? file.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return AttachmentMapper.toUploadResponse(storageKey, file.getSize(), mime);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
    return attachmentDownloadService.download(id, SecurityUtils.requireCurrentUserId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    attachmentDeletionService.softDelete(id, SecurityUtils.requireCurrentUserId());
  }
}
