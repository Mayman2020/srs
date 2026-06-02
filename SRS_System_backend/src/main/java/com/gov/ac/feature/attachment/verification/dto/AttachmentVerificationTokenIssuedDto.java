package com.gov.ac.feature.attachment.verification.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Issuance response: contains the raw token exactly once. The caller is expected to embed the
 * token into a QR / printed barcode immediately; the server never persists or returns it again.
 */
public record AttachmentVerificationTokenIssuedDto(
    UUID id,
    Long attachmentVersionId,
    String token,
    Instant issuedAt,
    Instant expiresAt) {}
