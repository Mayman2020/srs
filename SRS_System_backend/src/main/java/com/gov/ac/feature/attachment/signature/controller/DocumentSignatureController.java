package com.gov.ac.feature.attachment.signature.controller;

import com.gov.ac.feature.attachment.signature.DocumentSignatureService;
import com.gov.ac.feature.attachment.signature.dto.DocumentSignatureDto;
import com.gov.ac.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-attachment-version digital signature endpoints. Class-level {@code isAuthenticated()} is
 * enforced (and asserted by {@code ModuleBoundaryArchTest}); per-method permissions follow the
 * V18 canon ({@code ATTACHMENT_SIGN_VIEW}, {@code ATTACHMENT_SIGN_CREATE},
 * {@code ATTACHMENT_SIGNATURE_ADMIN}).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentSignatureController {

  private final DocumentSignatureService service;

  @PostMapping("/attachments/{id}/signatures")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_SIGN_CREATE')")
  @ResponseStatus(HttpStatus.CREATED)
  public DocumentSignatureDto create(@PathVariable("id") Long attachmentId) {
    return service.create(SecurityUtils.requireCurrentUserId(), attachmentId);
  }

  @GetMapping("/attachments/{id}/signatures")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_SIGN_VIEW')")
  public List<DocumentSignatureDto> list(@PathVariable("id") Long attachmentId) {
    return service.listForAttachment(SecurityUtils.requireCurrentUserId(), attachmentId);
  }

  @PostMapping("/signatures/{id}/verify")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_SIGN_VIEW')")
  public DocumentSignatureDto verify(@PathVariable("id") UUID signatureId) {
    return service.verify(SecurityUtils.requireCurrentUserId(), signatureId);
  }

  @DeleteMapping("/signatures/{id}")
  @PreAuthorize("@effectivePermission.has('ATTACHMENT_SIGNATURE_ADMIN')")
  public DocumentSignatureDto revoke(@PathVariable("id") UUID signatureId) {
    return service.revoke(SecurityUtils.requireCurrentUserId(), signatureId);
  }
}
