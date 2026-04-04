package com.gov.ac.web.dto;

import java.util.List;

/** Aggregated counts keyed by lookup `code` values (DB-driven labels via /lookups). */
public record DashboardChartsDto(
    List<CodeCountDto> byCorrespondenceStatus,
    List<CodeCountDto> byCorrespondenceType,
    List<CodeCountDto> byPriority) {}
