package com.gov.ac.feature.notification.channel.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.notification.channel.dto.NotificationOutboxAdminDto;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationOutboxRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationOutboxAdminService {

  private final NotificationOutboxRepository outboxRepository;

  @Transactional(readOnly = true)
  public Page<NotificationOutboxAdminDto> page(String status, Pageable pageable) {
    Page<NotificationOutboxEntity> page =
        (status == null || status.isBlank())
            ? outboxRepository.findAllByOrderByCreatedAtDesc(pageable)
            : outboxRepository.findAllByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
    return page.map(NotificationOutboxAdminService::toDto);
  }

  @Transactional
  public void requeue(UUID id) {
    NotificationOutboxEntity row =
        outboxRepository.findById(id).orElseThrow(() -> new NotFoundException("outbox row not found"));
    if (!NotificationOutboxEntity.STATUS_DEAD.equals(row.getStatus())) {
      throw new BadRequestException("Only DEAD rows can be re-queued");
    }
    row.setStatus(NotificationOutboxEntity.STATUS_PENDING);
    row.setAttemptCount(0);
    row.setNextAttemptAt(Instant.now());
    row.setLastError(null);
    outboxRepository.save(row);
  }

  @Transactional
  public void cancel(UUID id) {
    NotificationOutboxEntity row =
        outboxRepository.findById(id).orElseThrow(() -> new NotFoundException("outbox row not found"));
    row.setStatus(NotificationOutboxEntity.STATUS_DEAD);
    row.setLastError("CANCELLED_BY_ADMIN");
    outboxRepository.save(row);
  }

  private static NotificationOutboxAdminDto toDto(NotificationOutboxEntity e) {
    return new NotificationOutboxAdminDto(
        e.getId(),
        e.getIdempotencyKey(),
        e.getEventTypeCode(),
        e.getChannelCode(),
        e.getRecipientUserId(),
        e.getRecipientAddress(),
        e.getCorrelationResourceType(),
        e.getCorrelationResourceId(),
        e.getStatus(),
        e.getAttemptCount(),
        e.getNextAttemptAt(),
        e.getLastAttemptedAt(),
        e.getLastError());
  }
}
