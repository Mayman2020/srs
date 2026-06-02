package com.gov.ac.feature.attachment.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings bound from {@code ac.attachment.verify.public.*}.
 *
 * <p>The rate-limit is an in-memory sliding window per {@code (ip, token_hash)}; production
 * deployments are expected to layer an additional edge-level rate limit at the reverse proxy
 * (documented in {@code runbook.md} §12 Slice 6).
 */
@ConfigurationProperties(prefix = "ac.attachment.verify.public")
public record AttachmentVerificationProperties(
    Integer rateLimitPerMinute,
    Integer defaultTtlDays) {

  public AttachmentVerificationProperties {
    rateLimitPerMinute = (rateLimitPerMinute == null || rateLimitPerMinute <= 0) ? 30 : rateLimitPerMinute;
    defaultTtlDays = (defaultTtlDays == null || defaultTtlDays < 0) ? 1825 : defaultTtlDays;
  }
}
