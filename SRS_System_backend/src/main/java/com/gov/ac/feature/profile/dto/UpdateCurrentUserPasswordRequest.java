package com.gov.ac.feature.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserPasswordRequest(
    @NotBlank(message = "profile.validation.currentPasswordRequired") String currentPassword,
    @NotBlank(message = "profile.validation.newPasswordRequired")
        @Size(min = 8, message = "profile.validation.newPasswordTooShort")
        @Size(max = 128, message = "profile.validation.newPasswordTooLong")
        String newPassword) {}
