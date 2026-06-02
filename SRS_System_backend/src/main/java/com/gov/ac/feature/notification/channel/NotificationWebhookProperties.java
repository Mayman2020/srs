package com.gov.ac.feature.notification.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.notification.webhook")
public record NotificationWebhookProperties(String signingSecret) {

  public NotificationWebhookProperties {
    signingSecret = signingSecret == null ? "" : signingSecret;
  }
}
