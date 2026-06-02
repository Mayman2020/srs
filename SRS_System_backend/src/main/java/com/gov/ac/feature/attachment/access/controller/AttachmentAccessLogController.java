package com.gov.ac.feature.attachment.access.controller;

import com.gov.ac.feature.attachment.access.dto.AttachmentAccessLogDto;
import com.gov.ac.feature.attachment.access.service.AttachmentAccessLogService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only views into {@code attachment_access_log}. Gated on the new {@code
 * ATTACHMENT_ACCESS_LOG_VIEW} permission; SYS_ADMIN and AUDITOR receive it from the V14
 * migration.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('ATTACHMENT_ACCESS_LOG_VIEW')")
public class AttachmentAccessLogController {

  private final AttachmentAccessLogService accessLogService;

  @GetMapping("/attachments/{id}/access-log")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_ACCESS_LOG_VIEW')")
  public List<AttachmentAccessLogDto> forAttachment(@PathVariable("id") Long id) {
    return accessLogService.listForAttachment(id);
  }

  @GetMapping("/correspondence/{id}/attachment-access-log")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_ACCESS_LOG_VIEW')")
  public List<AttachmentAccessLogDto> forCorrespondence(@PathVariable("id") UUID id) {
    return accessLogService.listForCorrespondence(id);
  }
}
