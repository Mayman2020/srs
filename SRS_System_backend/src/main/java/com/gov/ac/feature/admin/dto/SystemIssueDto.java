package com.gov.ac.feature.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record SystemIssueDto(
    Long id,
    String source,
    String severity,
    String message,
    String detail,
    String pageUrl,
    UUID userId,
    Integer httpStatus,
    Instant createdAt,
    Instant resolvedAt,
    UUID resolvedBy,
    String resolutionNote) {}
