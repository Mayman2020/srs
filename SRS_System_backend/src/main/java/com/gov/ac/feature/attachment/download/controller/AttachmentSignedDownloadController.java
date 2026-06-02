package com.gov.ac.feature.attachment.download.controller;

import com.gov.ac.feature.attachment.download.AttachmentSignedDownloadService;
import com.gov.ac.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Streams the decrypted attachment bytes after validating the supplied single-use token. The
 * controller stays thin — actual decryption + access-log writes live in {@link
 * AttachmentSignedDownloadService} so that
 * {@code ModuleBoundaryArchTest.controllersDoNotDependOnEntities} can keep controllers free of
 * direct entity dependencies.
 */
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttachmentSignedDownloadController {

  private final AttachmentSignedDownloadService signedDownloadService;

  @GetMapping("/download/{token}")
  public ResponseEntity<StreamingResponseBody> stream(
      @PathVariable("token") String token, HttpServletRequest request) {
    return signedDownloadService.streamForToken(token, SecurityUtils.requireCurrentUserId(), request);
  }
}
