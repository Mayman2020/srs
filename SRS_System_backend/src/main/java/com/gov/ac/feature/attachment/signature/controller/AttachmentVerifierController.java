package com.gov.ac.feature.attachment.signature.controller;

import com.gov.ac.feature.attachment.signature.DocumentSignatureService;
import com.gov.ac.feature.attachment.signature.dto.AttachmentVerificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stable public-ish endpoint exposed for the QR / print-verification flow. Authenticated only —
 * external verifiers (e.g. a future kiosk) will get an unauthenticated mode in a later slice.
 */
@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttachmentVerifierController {

  private final DocumentSignatureService service;

  @GetMapping("/attachment-versions/{id}")
  public AttachmentVerificationDto verifyAttachmentVersion(@PathVariable("id") Long versionId) {
    return service.verifierProjection(versionId);
  }
}
