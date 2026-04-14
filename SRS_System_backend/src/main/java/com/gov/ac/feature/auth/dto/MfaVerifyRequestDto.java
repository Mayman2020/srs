package com.gov.ac.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequestDto(
    @NotBlank String username, @NotBlank String password, @NotBlank String code) {}
