package com.gov.ac.feature.communication.dto;

import java.time.Instant;
import java.util.UUID;

public record CircularReadReportSummaryDto(
    UUID circularId,
    String title,
    boolean broadcast,
    Instant createdAt,
    int totalRecipients,
    int readCount,
    double readPercent) {}
