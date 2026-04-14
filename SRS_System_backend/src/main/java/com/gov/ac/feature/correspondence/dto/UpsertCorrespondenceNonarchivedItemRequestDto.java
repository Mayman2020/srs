package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpsertCorrespondenceNonarchivedItemRequestDto(
    @NotBlank String itemType,
    String descriptionText,
    @Min(1) int quantity,
    int sortOrder) {}
