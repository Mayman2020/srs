package com.gov.ac.feature.dashboard.dto;

import java.util.List;

public record DashboardResponseDto(
    long totalCorrespondences,
    List<DashboardBucketDto> byStatus,
    List<DashboardBucketDto> byPriority,
    List<DashboardBucketDto> byOrgLevel,
    List<DashboardBucketDto> byConfidentiality,
    long overdueCount,
    long kpiSlaDoneCount,
    long kpiPipelineCount,
    long kpiInboxCount,
    long kpiOutboundCount) {}
