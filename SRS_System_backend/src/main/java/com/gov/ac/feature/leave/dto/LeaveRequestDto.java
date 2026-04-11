package com.gov.ac.feature.leave.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestDto(
    UUID id,
    UUID userId,
    String username,
    String fullNameAr,
    String fullNameEn,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    String statusCode,
    UUID decidedBy,
    Instant decidedAt,
    String decisionNote,
    Instant createdAt) {}
