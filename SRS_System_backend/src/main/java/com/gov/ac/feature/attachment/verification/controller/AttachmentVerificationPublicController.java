package com.gov.ac.feature.attachment.verification.controller;

import com.gov.ac.feature.attachment.verification.AttachmentVerificationTokenService;
import com.gov.ac.feature.attachment.verification.PublicVerifyRateLimiter;
import com.gov.ac.feature.attachment.verification.dto.AttachmentPublicVerificationDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unauthenticated public verification endpoint for printed QR codes. Secured only by the
 * unguessable raw token + in-app rate limiting + edge-level controls documented in the runbook.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class AttachmentVerificationPublicController {

  private final PublicVerifyRateLimiter rateLimiter;
  private final AttachmentVerificationTokenService tokenService;

  @GetMapping("/verify/{token}")
  public AttachmentPublicVerificationDto verify(
      @PathVariable("token") String token, HttpServletRequest request) {
    String ip = extractClientIp(request);
    String hash = AttachmentVerificationTokenService.sha256Hex(token);
    if (rateLimiter.tryAcquireBlocked(ip, hash)) {
      tokenService.recordRateLimited(token, request);
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts");
    }
    return tokenService.verifyPublic(token, request);
  }

  private static String extractClientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }
}
