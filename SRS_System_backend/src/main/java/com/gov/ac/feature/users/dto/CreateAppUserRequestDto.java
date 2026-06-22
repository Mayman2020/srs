package com.gov.ac.feature.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAppUserRequestDto(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String fullNameAr,
    @NotBlank String fullNameEn,
    @NotNull @Email String email,
    @NotNull Long departmentId,
    Long securityClearanceId) {}
