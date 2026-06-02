package com.gov.ac.feature.attachment.download.job;

import com.gov.ac.feature.attachment.download.AttachmentDownloadTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically deletes attachment_download_token rows past {@code expires_at + 1h}. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentDownloadTokenCleanupJob {

  private final AttachmentDownloadTokenService tokenService;

  @Scheduled(fixedDelayString = "${ac.attachment.download-token.cleanup-poll-ms:600000}")
  public void run() {
    try {
      tokenService.purgeExpired();
    } catch (RuntimeException ex) {
      log.warn("AttachmentDownloadTokenCleanupJob failed: {}", ex.getMessage(), ex);
    }
  }
}
