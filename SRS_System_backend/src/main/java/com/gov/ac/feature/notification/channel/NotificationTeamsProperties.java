package com.gov.ac.feature.notification.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.notification.teams")
public record NotificationTeamsProperties(String defaultTargetCode) {

  public NotificationTeamsProperties {
    defaultTargetCode = defaultTargetCode == null ? "" : defaultTargetCode.trim();
  }
}
