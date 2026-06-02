package com.gov.ac.feature.notification.channel.job;

import com.gov.ac.feature.notification.channel.NotificationOutboxDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxDispatchJob {

  private final NotificationOutboxDispatchService dispatchService;

  @Scheduled(
      fixedDelayString = "${ac.notification.dispatch.poll-ms:5000}",
      initialDelayString = "15000")
  public void run() {
    try {
      dispatchService.dispatchBatch();
    } catch (RuntimeException ex) {
      log.error("notification_outbox dispatch failed: {}", ex.getMessage(), ex);
    }
  }
}
