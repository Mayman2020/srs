package com.gov.ac.web.dto;

public record DashboardSummaryDto(
    long totalCorrespondence,
    long inboundCount,
    long outboundCount,
    long inProgressCount,
    long completedCount) {}
