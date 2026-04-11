package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpsertCorrespondenceNonarchivedItemRequest(
    @NotBlank String itemType,
    String descriptionText,
    @Min(1) int quantity,
    int sortOrder) {}
