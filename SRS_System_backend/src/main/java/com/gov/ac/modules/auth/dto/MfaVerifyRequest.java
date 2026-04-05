package com.gov.ac.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
    @NotBlank String username, @NotBlank String password, @NotBlank String code) {}
