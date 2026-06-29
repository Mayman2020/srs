package com.gov.ac.feature.notification.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertNotificationCatalogItemRequestDto(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 200) String nameAr,
    @NotBlank @Size(max = 200) String nameEn,
    @NotNull Integer sortOrder,
    @NotNull Boolean active) {}
