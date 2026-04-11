package com.gov.ac.feature.lookups.admin.dto;

/**
 * Full row for lookup admin UI. Optional fields are populated per physical table
 * (e.g. {@code terminal} only for correspondence_status).
 */
public record LookupRowAdminDto(
    Long id,
    String lookupCode,
    String code,
    String nameAr,
    String nameEn,
    String description,
    Integer sortOrder,
    Boolean active,
    Long parentId,
    Boolean terminal,
    Integer slaDays,
    Boolean restrictsExport,
    Boolean requiresClearance,
    /** {@code correspondence_status} only: badge key. */
    String uiVariant) {}
