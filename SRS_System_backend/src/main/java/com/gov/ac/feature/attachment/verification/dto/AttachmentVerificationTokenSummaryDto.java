package com.gov.ac.feature.attachment.verification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Listing projection (admin-side). Never exposes the raw token, only metadata + a stable id the
 * issuer can use to revoke.
 */
public record AttachmentVerificationTokenSummaryDto(
    UUID id,
    Long attachmentVersionId,
    UUID issuedBy,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt,
    UUID revokedBy,
    Integer accessCount,
    Instant lastAccessedAt) {}
