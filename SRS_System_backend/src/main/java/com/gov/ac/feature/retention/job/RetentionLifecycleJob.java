package com.gov.ac.feature.retention.job;

import com.gov.ac.feature.retention.RetentionProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionLifecycleJob {

  private final RetentionProcessingService retentionProcessingService;

  @Scheduled(fixedDelayString = "${ac.retention.poll-ms:3600000}", initialDelayString = "120000")
  public void run() {
    try {
      retentionProcessingService.runTick();
    } catch (RuntimeException ex) {
      log.error("Retention lifecycle tick failed: {}", ex.getMessage(), ex);
    }
  }
}
