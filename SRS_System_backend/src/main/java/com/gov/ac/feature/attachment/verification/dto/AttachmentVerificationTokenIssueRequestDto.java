package com.gov.ac.feature.attachment.verification.dto;

/**
 * Issuance request. {@code ttlDays} is optional; when null we apply
 * {@code ac.attachment.verify.public.default-ttl-days}. A zero or negative value is rejected.
 * Setting {@code permanent=true} produces a row with NULL {@code expires_at} (printed permanent
 * letters).
 */
public record AttachmentVerificationTokenIssueRequestDto(
    Integer ttlDays,
    Boolean permanent) {}
