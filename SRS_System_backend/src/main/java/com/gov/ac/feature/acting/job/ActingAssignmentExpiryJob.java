package com.gov.ac.feature.acting.job;

import com.gov.ac.feature.acting.repository.ActingAssignmentRepository;
import com.gov.ac.feature.acting.service.ActingAssignmentService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActingAssignmentExpiryJob {

  private final ActingAssignmentService actingAssignmentService;

  @Scheduled(fixedDelayString = "${ac.acting-assignment.expiry-poll-ms:600000}")
  public void run() {
    LocalDate today = LocalDate.now();
    int n = actingAssignmentService.expireOverdue(today);
    if (n > 0) {
      log.info("ActingAssignmentExpiryJob expired {} row(s) on {}", n, today);
    } else {
      log.debug("ActingAssignmentExpiryJob ran with no expirations on {}", today);
    }
  }
}
