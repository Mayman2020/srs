package com.gov.ac.attachment.web;

import com.gov.ac.attachment.AttachmentDownloadService;
import com.gov.ac.attachment.AttachmentStorageProperties;
import com.gov.ac.attachment.dto.AttachmentUploadResponse;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.common.api.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentDownloadService attachmentDownloadService;
  private final AttachmentStorageProperties storageProperties;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AttachmentUploadResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
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

    String ym = YearMonth.now().toString();
    String relativeDir = "uploads/" + ym;
    Path dir = root.resolve(relativeDir).normalize();
    if (!dir.startsWith(root)) {
      throw new IllegalStateException("Invalid storage root");
    }
    Files.createDirectories(dir);

    String unique = UUID.randomUUID() + "_" + original;
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
    return new AttachmentUploadResponse(storageKey, file.getSize(), mime);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
    return attachmentDownloadService.download(id, SecurityUtils.requireCurrentUserId());
  }
}
