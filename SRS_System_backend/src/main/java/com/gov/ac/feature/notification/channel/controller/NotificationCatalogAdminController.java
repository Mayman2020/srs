package com.gov.ac.feature.notification.channel.controller;

import com.gov.ac.feature.notification.channel.dto.NotificationCatalogAdminDto;
import com.gov.ac.feature.notification.channel.dto.NotificationCatalogAdminDto.NotificationCatalogAdminItemDto;
import com.gov.ac.feature.notification.channel.dto.UpsertNotificationCatalogItemRequestDto;
import com.gov.ac.feature.notification.channel.service.NotificationCatalogAdminService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-catalog/admin")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
public class NotificationCatalogAdminController {

  private final NotificationCatalogAdminService adminService;

  @GetMapping
  public NotificationCatalogAdminDto loadAdmin() {
    return adminService.loadAdmin();
  }

  @PostMapping("/event-types")
  @ResponseStatus(HttpStatus.CREATED)
  public NotificationCatalogAdminItemDto createEventType(
      @Valid @RequestBody UpsertNotificationCatalogItemRequestDto body) {
    return adminService.upsertEventType(actor(), body);
  }

  @PutMapping("/event-types/{code}")
  public NotificationCatalogAdminItemDto updateEventType(
      @PathVariable String code, @Valid @RequestBody UpsertNotificationCatalogItemRequestDto body) {
    if (!code.equalsIgnoreCase(body.code())) {
      throw new IllegalArgumentException("Path code must match body code");
    }
    return adminService.upsertEventType(actor(), body);
  }

  @DeleteMapping("/event-types/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEventType(@PathVariable String code) {
    adminService.deleteEventType(actor(), code);
  }

  @PostMapping("/channels")
  @ResponseStatus(HttpStatus.CREATED)
  public NotificationCatalogAdminItemDto createChannel(
      @Valid @RequestBody UpsertNotificationCatalogItemRequestDto body) {
    return adminService.upsertChannel(actor(), body);
  }

  @PutMapping("/channels/{code}")
  public NotificationCatalogAdminItemDto updateChannel(
      @PathVariable String code, @Valid @RequestBody UpsertNotificationCatalogItemRequestDto body) {
    if (!code.equalsIgnoreCase(body.code())) {
      throw new IllegalArgumentException("Path code must match body code");
    }
    return adminService.upsertChannel(actor(), body);
  }

  @DeleteMapping("/channels/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteChannel(@PathVariable String code) {
    adminService.deleteChannel(actor(), code);
  }

  private static UUID actor() {
    return SecurityUtils.requireCurrentUserId();
  }
}
