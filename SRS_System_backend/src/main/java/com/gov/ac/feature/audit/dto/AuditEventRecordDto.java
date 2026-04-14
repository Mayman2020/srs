package com.gov.ac.feature.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventRecordDto(
    UUID id,
    Instant occurredAt,
    String actorUserId,
    String actionCode,
    String resourceType,
    String resourceId,
    String detailJson,
    String ipAddress,
    String userAgent) {}
