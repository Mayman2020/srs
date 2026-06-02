package com.gov.ac.feature.attachment.download;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings bound from {@code ac.attachment.download-token.*}. */
@ConfigurationProperties(prefix = "ac.attachment.download-token")
public record AttachmentDownloadTokenProperties(
    /** Token lifetime in seconds. Kept short on purpose (default 60). */
    Long ttlSeconds,
    /** Cleanup job poll interval; rows past {@code expires_at + 1h} are deleted. */
    Long cleanupPollMs) {

  public AttachmentDownloadTokenProperties {
    ttlSeconds = (ttlSeconds == null || ttlSeconds <= 0) ? 60L : ttlSeconds;
    cleanupPollMs = (cleanupPollMs == null || cleanupPollMs <= 0) ? 600_000L : cleanupPollMs;
  }
}
