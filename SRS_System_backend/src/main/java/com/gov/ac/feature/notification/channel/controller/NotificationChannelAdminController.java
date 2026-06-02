package com.gov.ac.feature.notification.channel.controller;

import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetAdminDto;
import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetCreateDto;
import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetUpdateDto;
import com.gov.ac.feature.notification.channel.service.NotificationChannelAdminService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/notification-channel-targets")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationChannelAdminController {

  private final NotificationChannelAdminService channelAdminService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public List<NotificationChannelTargetAdminDto> list() {
    return channelAdminService.list();
  }

  @PostMapping
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public NotificationChannelTargetAdminDto create(@Valid @RequestBody NotificationChannelTargetCreateDto body) {
    return channelAdminService.create(body);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public NotificationChannelTargetAdminDto update(
      @PathVariable UUID id, @Valid @RequestBody NotificationChannelTargetUpdateDto body) {
    return channelAdminService.update(id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_CHANNEL_ADMIN')")
  public void delete(@PathVariable UUID id) {
    channelAdminService.delete(id);
  }
}
