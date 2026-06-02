package com.gov.ac.feature.correspondence.readtracking.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read receipt for a single (correspondence, user) pair. {@code userId} / {@code username} are
 * left null when this DTO is embedded as the caller's own receipt to keep the payload compact.
 */
public record CorrespondenceReadReceiptDto(
    Long id,
    UUID userId,
    String username,
    String fullNameAr,
    String fullNameEn,
    Instant firstOpenedAt,
    Instant lastOpenedAt,
    int openCount,
    Instant acknowledgedAt,
    String acknowledgementComment) {}
