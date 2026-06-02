package com.gov.ac.feature.retention.dto;

import java.time.Instant;
import java.util.UUID;

public record LegalHoldDto(
    UUID id,
    UUID correspondenceId,
    String reason,
    UUID placedBy,
    Instant placedAt,
    Instant releasedAt,
    UUID releasedBy,
    String releaseReason) {}
