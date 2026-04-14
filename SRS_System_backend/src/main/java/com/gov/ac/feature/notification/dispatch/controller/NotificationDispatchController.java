package com.gov.ac.feature.notification.dispatch.controller;

import com.gov.ac.feature.notification.dispatch.dto.EmailDispatchRequestDto;
import com.gov.ac.feature.notification.dispatch.dto.SmsDispatchRequestDto;
import com.gov.ac.feature.notification.dispatch.service.OutboundMailService;
import com.gov.ac.feature.notification.dispatch.service.OutboundSmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/dispatch")
@RequiredArgsConstructor
public class NotificationDispatchController {

  private final OutboundMailService outboundMailService;
  private final OutboundSmsService outboundSmsService;

  @PostMapping("/email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dispatchEmail(@Valid @RequestBody EmailDispatchRequestDto body) {
    outboundMailService.send(body.to(), body.subject(), body.body());
  }

  @PostMapping("/sms")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void dispatchSms(@Valid @RequestBody SmsDispatchRequestDto body) {
    outboundSmsService.send(body.phoneE164(), body.message());
  }
}
