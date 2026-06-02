package com.gov.ac.feature.notification.channel.controller;

import com.gov.ac.feature.notification.channel.dto.NotificationOutboxAdminDto;
import com.gov.ac.feature.notification.channel.service.NotificationOutboxAdminService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-outbox")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationOutboxAdminController {

  private final NotificationOutboxAdminService outboxAdminService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public Page<NotificationOutboxAdminDto> page(
      @RequestParam(required = false) String status, Pageable pageable) {
    return outboxAdminService.page(status, pageable);
  }

  @PostMapping("/{id}/requeue")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public void requeue(@PathVariable UUID id) {
    outboxAdminService.requeue(id);
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public void cancel(@PathVariable UUID id) {
    outboxAdminService.cancel(id);
  }
}
