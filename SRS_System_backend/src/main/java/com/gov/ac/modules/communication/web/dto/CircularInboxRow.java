package com.gov.ac.modules.communication.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CircularInboxRow(
    UUID id,
    String title,
    String createdBy,
    Instant createdAt,
    boolean broadcast,
    boolean read) {}
