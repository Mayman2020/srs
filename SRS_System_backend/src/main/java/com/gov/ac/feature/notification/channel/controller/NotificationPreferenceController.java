package com.gov.ac.feature.notification.channel.controller;

import com.gov.ac.feature.notification.channel.NotificationPreferenceService;
import com.gov.ac.feature.notification.channel.dto.NotificationPreferenceRowDto;
import com.gov.ac.feature.notification.channel.dto.NotificationPreferenceUpsertDto;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationPreferenceController {

  private final NotificationPreferenceService preferenceService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_PREFERENCE_MANAGE')")
  public List<NotificationPreferenceRowDto> list() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return preferenceService.listForUser(userId);
  }

  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('NOTIFICATION_PREFERENCE_MANAGE')")
  public void replace(@Valid @RequestBody List<NotificationPreferenceUpsertDto> body) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    preferenceService.upsertOwn(userId, body);
  }
}
