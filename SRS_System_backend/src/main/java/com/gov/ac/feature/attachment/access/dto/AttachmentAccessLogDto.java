package com.gov.ac.feature.attachment.access.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentAccessLogDto(
    Long id,
    Long attachmentId,
    Long attachmentVersionId,
    UUID correspondenceId,
    UUID userId,
    String username,
    String fullNameAr,
    String fullNameEn,
    String actionCode,
    Instant occurredAt,
    String ipAddress,
    String userAgent,
    boolean success) {}
