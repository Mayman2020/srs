package com.gov.ac.feature.dashboard.dto;

/**
 * Aggregated correspondence count for one lookup row (status, priority, etc.); labels come from DB.
 */
public record DashboardBucketDto(
    long lookupId, String code, String nameAr, String nameEn, int sortOrder, long count, String uiVariant) {}
