package com.gov.ac.feature.notification.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ac.notification")
public record NotificationRoutingProperties(String routing) {

  public NotificationRoutingProperties {
    routing = (routing == null || routing.isBlank()) ? "outbox" : routing.trim().toLowerCase();
  }

  public boolean isOutbox() {
    return "outbox".equals(routing);
  }
}
