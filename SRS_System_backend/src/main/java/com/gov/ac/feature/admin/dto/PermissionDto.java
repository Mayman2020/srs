package com.gov.ac.feature.admin.dto;

public record PermissionDto(
    Long id,
    String code,
    String nameAr,
    String nameEn,
    String description,
    Integer sortOrder,
    Boolean active,
    Long uiScreenId) {}
