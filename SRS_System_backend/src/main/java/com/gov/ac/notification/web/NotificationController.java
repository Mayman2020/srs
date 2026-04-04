package com.gov.ac.notification.web;

import com.gov.ac.notification.NotificationService;
import com.gov.ac.notification.dto.NotificationItemDto;
import com.gov.ac.security.SecurityUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public Page<NotificationItemDto> list(@PageableDefault(size = 50) Pageable pageable) {
    return notificationService.listInbox(SecurityUtils.requireCurrentUserId(), pageable);
  }

  @PatchMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@PathVariable UUID id) {
    notificationService.markRead(id, SecurityUtils.requireCurrentUserId());
  }
}
