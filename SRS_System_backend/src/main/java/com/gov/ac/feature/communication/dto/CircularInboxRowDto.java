package com.gov.ac.feature.communication.dto;

import java.time.Instant;
import java.util.UUID;

public record CircularInboxRowDto(
    UUID id,
    String title,
    String createdBy,
    Instant createdAt,
    boolean broadcast,
    boolean read) {}
