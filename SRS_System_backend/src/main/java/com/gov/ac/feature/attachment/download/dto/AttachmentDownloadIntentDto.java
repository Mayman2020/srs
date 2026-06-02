package com.gov.ac.feature.attachment.download.dto;

import java.time.Instant;

/**
 * Response of {@code POST /api/v1/attachments/{id}/download-intent}. The raw token is shown to
 * the caller exactly once; the server stores only its SHA-256.
 */
public record AttachmentDownloadIntentDto(String token, Instant expiresAt) {}
