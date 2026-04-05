package com.gov.ac.modules.audit.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventRecord(
    UUID id,
    Instant occurredAt,
    String actorUserId,
    String actionCode,
    String resourceType,
    String resourceId,
    String detailJson,
    String ipAddress,
    String userAgent) {}
