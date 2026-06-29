package com.gov.ac.feature.communication.dto;

import java.time.Instant;

public record CircularReadRecipientRowDto(
    String userId, String username, String fullNameAr, String fullNameEn, Instant readAt, boolean read) {}
