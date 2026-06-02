package com.gov.ac.feature.retention.dto;

import java.time.Instant;
import java.util.UUID;

public record ArchiveTransitionLogDto(
    UUID id,
    String appliedTo,
    String resourceId,
    UUID policyId,
    String action,
    Instant executedAt,
    String detailJson) {}
