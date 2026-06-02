package com.gov.ac.feature.attachment.verification.controller;

import com.gov.ac.feature.attachment.verification.AttachmentVerificationTokenService;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenIssueRequestDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenIssuedDto;
import com.gov.ac.feature.attachment.verification.dto.AttachmentVerificationTokenSummaryDto;
import com.gov.ac.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttachmentVerificationAdminController {

  private final AttachmentVerificationTokenService tokenService;

  @PostMapping("/{id}/verification-tokens")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_VERIFY_TOKEN_ISSUE')")
  public AttachmentVerificationTokenIssuedDto issue(
      @PathVariable("id") Long attachmentId,
      @RequestBody(required = false) AttachmentVerificationTokenIssueRequestDto body,
      HttpServletRequest request) {
    return tokenService.issue(attachmentId, SecurityUtils.requireCurrentUserId(), request, body);
  }

  @GetMapping("/{id}/verification-tokens")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_VERIFY_TOKEN_VIEW')")
  public List<AttachmentVerificationTokenSummaryDto> list(@PathVariable("id") Long attachmentId) {
    return tokenService.listForAttachment(attachmentId, SecurityUtils.requireCurrentUserId());
  }

  @DeleteMapping("/{id}/verification-tokens/{tokenId}")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_VERIFY_TOKEN_ISSUE')")
  public void revoke(
      @PathVariable("id") Long attachmentId, @PathVariable("tokenId") UUID tokenId) {
    tokenService.revoke(attachmentId, tokenId, SecurityUtils.requireCurrentUserId());
  }
}
