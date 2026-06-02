package com.gov.ac.feature.organization.dto;

/** Read-only DTO for the FE org-levels admin grid. */
public record OrganizationalUnitLevelDto(
    Long id,
    String code,
    String nameAr,
    String nameEn,
    String description,
    Integer rankOrder,
    Boolean active) {}
