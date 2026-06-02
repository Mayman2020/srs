package com.gov.ac.feature.notification.inbox.controller;

import com.gov.ac.feature.notification.inbox.dto.NotificationItemDto;
import com.gov.ac.feature.notification.inbox.service.NotificationInboxService;
import com.gov.ac.security.SecurityUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('NOTIFICATION_VIEW')")
public class NotificationInboxController {

  private final NotificationInboxService notificationInboxService;

  @GetMapping
  public Page<NotificationItemDto> list(
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return notificationInboxService.listInbox(SecurityUtils.requireCurrentUserId(), pageable);
  }

  @PatchMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@PathVariable UUID id) {
    notificationInboxService.markRead(id, SecurityUtils.requireCurrentUserId());
  }

  /**
   * Marks every unread notification for the current user as read in a single bulk UPDATE.
   * Eliminates the N-request pattern from the frontend topbar.
   */
  @PatchMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllRead() {
    notificationInboxService.markAllRead(SecurityUtils.requireCurrentUserId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    notificationInboxService.deleteForRecipient(id, SecurityUtils.requireCurrentUserId());
  }
}
