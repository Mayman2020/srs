package com.gov.ac.feature.notification.channel.dto;

import java.util.List;

public record NotificationCatalogAdminDto(
    List<NotificationCatalogAdminItemDto> eventTypes,
    List<NotificationCatalogAdminItemDto> channels) {

  public record NotificationCatalogAdminItemDto(
      String code, String nameEn, String nameAr, int sortOrder, boolean active) {}
}
