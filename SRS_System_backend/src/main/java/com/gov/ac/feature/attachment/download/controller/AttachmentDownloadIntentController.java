package com.gov.ac.feature.attachment.download.controller;

import com.gov.ac.feature.attachment.download.AttachmentDownloadTokenService;
import com.gov.ac.feature.attachment.download.dto.AttachmentDownloadIntentDto;
import com.gov.ac.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues a single-use signed-download token for the requested attachment. The class-level
 * {@code isAuthenticated()} is asserted by {@code ModuleBoundaryArchTest}; the method-level
 * permission gate mirrors the legacy {@code GET /attachments/{id}/download}.
 */
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttachmentDownloadIntentController {

  private final AttachmentDownloadTokenService tokenService;

  @PostMapping("/{id}/download-intent")
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
  public AttachmentDownloadIntentDto requestIntent(
      @PathVariable("id") Long attachmentId, HttpServletRequest request) {
    return tokenService.issue(attachmentId, SecurityUtils.requireCurrentUserId(), request);
  }
}
