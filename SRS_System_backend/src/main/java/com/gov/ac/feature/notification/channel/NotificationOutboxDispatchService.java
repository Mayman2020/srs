package com.gov.ac.feature.notification.channel;

import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.channel.provider.NotificationChannelProvider;
import com.gov.ac.feature.notification.channel.repository.NotificationOutboxRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxDispatchService {

  public static final String AUDIT_OUTBOX_DEAD = "NOTIFICATION_OUTBOX_DEAD_LETTER";

  private final NotificationOutboxRepository outboxRepository;
  private final NotificationDispatchProperties dispatchProperties;
  private final List<NotificationChannelProvider> providers;
  private final AuditTrailService auditTrailService;

  @Transactional
  public void dispatchBatch() {
    Instant now = Instant.now();
    int batchSize = dispatchProperties.batchSize();
    List<NotificationOutboxEntity> batch =
        outboxRepository.findDueBatchForUpdateSkipLocked(now, batchSize);
    for (NotificationOutboxEntity row : batch) {
      row.setStatus(NotificationOutboxEntity.STATUS_IN_FLIGHT);
      row.setLastAttemptedAt(now);
    }
    outboxRepository.saveAll(batch);
    outboxRepository.flush();

    for (NotificationOutboxEntity row : batch) {
      NotificationChannelProvider provider =
          providers.stream().filter(p -> p.supports(row)).findFirst().orElse(null);
      if (provider == null) {
        markDead(row, "No channel provider for " + row.getChannelCode());
        continue;
      }
      try {
        provider.dispatch(row);
        row.setStatus(NotificationOutboxEntity.STATUS_SENT);
        row.setLastError(null);
      } catch (TerminalNotificationDispatchException ex) {
        markDead(row, ex.getMessage());
      } catch (Exception ex) {
        handleFailure(row, ex);
      }
      outboxRepository.save(row);
    }
  }

  private void handleFailure(NotificationOutboxEntity row, Exception ex) {
    int attempts = row.getAttemptCount() == null ? 0 : row.getAttemptCount();
    attempts++;
    row.setAttemptCount(attempts);
    row.setLastError(ex.getMessage());
    long backoff =
        Math.min(
            dispatchProperties.maxBackoffMs(),
            dispatchProperties.baseBackoffMs() * (1L << Math.min(attempts, 20)));
    row.setNextAttemptAt(Instant.now().plus(backoff, ChronoUnit.MILLIS));
    row.setStatus(NotificationOutboxEntity.STATUS_PENDING);
    if (attempts >= dispatchProperties.maxAttempts()) {
      markDead(row, ex.getMessage());
    }
  }

  private void markDead(NotificationOutboxEntity row, String err) {
    row.setStatus(NotificationOutboxEntity.STATUS_DEAD);
    row.setLastError(err);
    log.warn("notification_outbox DEAD id={} channel={} err={}", row.getId(), row.getChannelCode(), err);
    try {
      auditTrailService.append(
          new CreateAuditEventRequestDto(
              "SYSTEM",
              AUDIT_OUTBOX_DEAD,
              "NOTIFICATION_OUTBOX",
              row.getId() != null ? row.getId().toString() : "",
              "{\"channel\":\"" + row.getChannelCode() + "\",\"error\":\"" + escapeJson(err) + "\"}",
              null,
              null,
              Instant.now()));
    } catch (RuntimeException auditEx) {
      log.warn("Failed to append NOTIFICATION_OUTBOX_DEAD_LETTER audit: {}", auditEx.getMessage());
    }
  }

  private static String escapeJson(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
  }
}
