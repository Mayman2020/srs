package com.gov.ac.notification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationItemDto {

  UUID id;
  /** Recipient ({@code recipient_user_id}). */
  UUID userId;
  /** {@code notification_event_type.code} — stable “type” for UI. */
  String type;
  /** Client-side i18n key for the body. */
  String messageKey;
  /** Optional interpolation map for the message key. */
  Map<String, Object> messageParams;
  boolean read;
  Instant createdAt;
}
