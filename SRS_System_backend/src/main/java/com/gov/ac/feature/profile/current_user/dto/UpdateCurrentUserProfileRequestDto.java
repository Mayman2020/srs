package com.gov.ac.feature.profile.current_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileRequestDto(
    @NotBlank(message = "profile.validation.fullNameArRequired")
        @Size(max = 200, message = "profile.validation.fullNameArTooLong")
        String fullNameAr,
    @NotBlank(message = "profile.validation.fullNameEnRequired")
        @Size(max = 200, message = "profile.validation.fullNameEnTooLong")
        String fullNameEn,
    @NotBlank(message = "profile.validation.emailRequired")
        @Email(message = "profile.validation.emailInvalid")
        @Size(max = 320, message = "profile.validation.emailTooLong")
        String email,
    @Size(max = 32, message = "profile.validation.phoneTooLong") String phone,
    @Size(max = 32, message = "profile.validation.nationalIdTooLong") String nationalId) {}
