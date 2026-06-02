package com.gov.ac.feature.notification.channel.dto;

import java.util.List;

/**
 * Lightweight catalog returned by {@code GET /api/v1/notification-catalog}. Used by the
 * preferences and channel admin UIs to render localized labels without hardcoding codes.
 */
public record NotificationCatalogDto(
    List<NotificationCatalogItemDto> eventTypes,
    List<NotificationCatalogItemDto> channels) {

  public record NotificationCatalogItemDto(String code, String nameEn, String nameAr) {}
}
