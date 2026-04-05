package com.gov.ac.feature.admin.dto;

public record UiScreenDto(
    Long id,
    String code,
    String routePath,
    String nameAr,
    String nameEn,
    String description,
    Integer sortOrder,
    Boolean active) {}
