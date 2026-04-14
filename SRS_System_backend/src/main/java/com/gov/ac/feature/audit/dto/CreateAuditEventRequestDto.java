package com.gov.ac.feature.audit.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateAuditEventRequestDto(
    @NotBlank String actorUserId,
    @NotBlank String actionCode,
    String resourceType,
    String resourceId,
    String detailJson,
    String ipAddress,
    String userAgent,
    Instant occurredAt) {}
