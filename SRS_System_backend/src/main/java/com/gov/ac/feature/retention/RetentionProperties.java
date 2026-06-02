package com.gov.ac.feature.retention;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.retention")
public record RetentionProperties(
    Long pollMs,
    Integer batchSize,
    Boolean dryRun) {

  public RetentionProperties {
    pollMs = (pollMs == null || pollMs <= 0) ? 3_600_000L : pollMs;
    batchSize = (batchSize == null || batchSize <= 0) ? 500 : batchSize;
    // Default TRUE — production must explicitly set AC_RETENTION_DRY_RUN=false after observation.
    dryRun = dryRun == null ? Boolean.TRUE : dryRun;
  }
}
