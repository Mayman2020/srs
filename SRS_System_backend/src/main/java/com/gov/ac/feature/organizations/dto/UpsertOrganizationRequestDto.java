package com.gov.ac.feature.organizations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertOrganizationRequestDto(
    Long parentId,
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 500) String nameAr,
    @NotBlank @Size(max = 500) String nameEn,
    boolean external,
    @Size(max = 4000) String description) {}
