package com.gov.ac.feature.delegation.task.job;

import com.gov.ac.feature.delegation.task.service.TaskDelegationService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Idempotent expiry sweep: every {@code ac.task-delegation.expiry-poll-ms} milliseconds, marks
 * every active task delegation whose {@code valid_to} is strictly before today as expired and
 * emits a {@code TASK_DELEGATION_EXPIRED} audit event for each. Running the job twice in a row
 * is a no-op because the second pass finds nothing to expire (the {@code revoked_at IS NULL}
 * predicate filters already-expired rows).
 *
 * <p>Default cadence is 10 minutes. The job is intentionally light-touch — it does NOT walk
 * Camunda to "undo" any in-flight task reassignments; the listener-driven model is forward-only
 * and an expired delegation simply stops applying to future task creations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDelegationExpiryJob {

  private final TaskDelegationService taskDelegationService;

  @Scheduled(fixedDelayString = "${ac.task-delegation.expiry-poll-ms:600000}")
  public void run() {
    LocalDate today = LocalDate.now();
    int expired = taskDelegationService.expireOverdue(today);
    if (expired > 0) {
      log.info("TaskDelegationExpiryJob expired {} delegations on {}", expired, today);
    } else {
      log.debug("TaskDelegationExpiryJob ran with no expirations on {}", today);
    }
  }
}
