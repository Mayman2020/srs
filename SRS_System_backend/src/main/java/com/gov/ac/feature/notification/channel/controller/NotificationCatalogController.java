package com.gov.ac.feature.notification.channel.controller;

import com.gov.ac.feature.notification.channel.dto.NotificationCatalogDto;
import com.gov.ac.feature.notification.channel.service.NotificationCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog endpoint used by the preferences screen and the channel admin to render
 * localized labels for event-type and channel codes without hard-coding them on the FE.
 */
@RestController
@RequestMapping("/api/v1/notification-catalog")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationCatalogController {

  private final NotificationCatalogService catalogService;

  @GetMapping
  public NotificationCatalogDto load() {
    return catalogService.load();
  }
}
