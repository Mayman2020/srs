package com.gov.ac.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
