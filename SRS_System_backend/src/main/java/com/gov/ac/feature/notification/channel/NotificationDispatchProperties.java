package com.gov.ac.feature.notification.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.notification.dispatch")
public record NotificationDispatchProperties(
    Long pollMs,
    Integer batchSize,
    Integer maxAttempts,
    Long baseBackoffMs,
    Long maxBackoffMs) {

  public NotificationDispatchProperties {
    pollMs = (pollMs == null || pollMs <= 0) ? 5_000L : pollMs;
    batchSize = (batchSize == null || batchSize <= 0) ? 50 : batchSize;
    maxAttempts = (maxAttempts == null || maxAttempts <= 0) ? 8 : maxAttempts;
    baseBackoffMs = (baseBackoffMs == null || baseBackoffMs <= 0) ? 5_000L : baseBackoffMs;
    maxBackoffMs = (maxBackoffMs == null || maxBackoffMs <= 0) ? 3_600_000L : maxBackoffMs;
  }
}
