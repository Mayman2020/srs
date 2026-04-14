package com.gov.ac.feature.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record UpdateAppUserRequestDto(
    @NotNull String fullNameAr,
    @NotNull String fullNameEn,
    @NotNull @Email String email,
    @NotNull Long departmentId,
    @NotNull Boolean active,
    String password) {}
