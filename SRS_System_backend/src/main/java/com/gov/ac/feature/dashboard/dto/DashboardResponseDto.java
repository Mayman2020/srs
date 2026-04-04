package com.gov.ac.feature.dashboard.dto;

import java.util.List;

public record DashboardResponseDto(
    long totalCorrespondences,
    List<DashboardBucketDto> byStatus,
    List<DashboardBucketDto> byPriority,
    long overdueCount) {}
