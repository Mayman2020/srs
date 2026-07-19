package com.gov.ac.feature.notification.channel.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationOutboxAdminDto(
    UUID id,
    String idempotencyKey,
    String eventTypeCode,
    String channelCode,
    UUID recipientUserId,
    String recipientAddress,
    String correlationResourceType,
    String correlationResourceId,
    String status,
    Integer attemptCount,
    Instant nextAttemptAt,
    Instant lastAttemptedAt,
    String lastError) {}
