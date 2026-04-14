package com.gov.ac.feature.lookups.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record LookupUpsertRequestDto(
    @NotBlank String code,
    @NotBlank String nameAr,
    @NotBlank String nameEn,
    String description,
    Integer sortOrder,
    Boolean active,
    Long parentId,
    Boolean terminal,
    Integer slaDays,
    Boolean restrictsExport,
    Boolean requiresClearance,
    /** {@code correspondence_status} only: success, danger, warning, info, secondary, neutral. */
    String uiVariant) {}
