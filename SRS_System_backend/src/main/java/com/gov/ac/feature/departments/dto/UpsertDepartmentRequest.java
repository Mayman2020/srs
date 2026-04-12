package com.gov.ac.feature.departments.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertDepartmentRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 250) String nameAr,
    @NotBlank @Size(max = 250) String nameEn,
    Long parentId,
    @Min(0) Integer sortOrder) {}

