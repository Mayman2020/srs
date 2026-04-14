package com.gov.ac.feature.notification.inbox.mapper;

import com.gov.ac.feature.notification.inbox.dto.NotificationItemDto;
import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import java.util.Map;

public final class NotificationInboxMapper {

  private NotificationInboxMapper() {}

  public static NotificationItemDto toDto(InAppNotificationEntity notification) {
    return NotificationItemDto.builder()
        .id(notification.getId())
        .userId(notification.getRecipient().getId())
        .type(notification.getEventType().getCode())
        .messageKey(notification.getMessageKey())
        .messageParams(
            notification.getMessageParams() != null
                ? Map.copyOf(notification.getMessageParams())
                : Map.of())
        .read(notification.getReadAt() != null)
        .createdAt(notification.getCreatedAt())
        .build();
  }
}
