package com.gov.ac.modules.notification.web;

import com.gov.ac.modules.notification.NotificationService;
import com.gov.ac.modules.notification.dispatch.OutboundMailService;
import com.gov.ac.modules.notification.dispatch.OutboundSmsService;
import com.gov.ac.modules.notification.dto.NotificationItemDto;
import com.gov.ac.modules.notification.web.dto.EmailDispatchRequest;
import com.gov.ac.modules.notification.web.dto.SmsDispatchRequest;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final OutboundMailService outboundMailService;
  private final OutboundSmsService outboundSmsService;

  @GetMapping
  public Page<NotificationItemDto> list(
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return notificationService.listInbox(SecurityUtils.requireCurrentUserId(), pageable);
  }

  @PatchMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@PathVariable UUID id) {
    notificationService.markRead(id, SecurityUtils.requireCurrentUserId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    notificationService.deleteForRecipient(id, SecurityUtils.requireCurrentUserId());
  }

  /** Direct email channel (integrate real SMTP / provider via {@code spring.mail}). */
  @PostMapping("/dispatch/email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dispatchEmail(@Valid @RequestBody EmailDispatchRequest body) {
    outboundMailService.send(body.to(), body.subject(), body.body());
  }

  /** SMS stub — replace with gateway integration. */
  @PostMapping("/dispatch/sms")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dispatchSms(@Valid @RequestBody SmsDispatchRequest body) {
    outboundSmsService.send(body.phoneE164(), body.message());
  }
}
