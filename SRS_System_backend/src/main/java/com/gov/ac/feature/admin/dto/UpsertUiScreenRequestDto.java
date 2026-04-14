package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertUiScreenRequestDto(
    @NotBlank String code,
    @NotBlank String routePath,
    @NotBlank String nameAr,
    @NotBlank String nameEn,
    String description,
    @NotNull Integer sortOrder,
    @NotNull Boolean active,
    /** Material icon ligature; omit or blank to default to {@code apps}. */
    String iconKey,
    /** Omit to default {@code false} on create; on update, omit to leave unchanged. */
    Boolean showInShellNav,
    /** Optional FK to {@code permission.id}; omit on update to leave unchanged. */
    Long requiredPermissionId) {}
