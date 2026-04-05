package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertUiScreenRequest(
    @NotBlank String code,
    @NotBlank String routePath,
    @NotBlank String nameAr,
    @NotBlank String nameEn,
    String description,
    @NotNull Integer sortOrder,
    @NotNull Boolean active) {}
