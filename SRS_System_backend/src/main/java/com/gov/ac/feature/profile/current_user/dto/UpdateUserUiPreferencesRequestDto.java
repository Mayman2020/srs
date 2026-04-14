package com.gov.ac.feature.profile.current_user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserUiPreferencesRequestDto(
    @NotBlank @Pattern(regexp = "^(light|dark)$", message = "profile.validation.uiThemeInvalid")
        String uiTheme,
    @NotBlank @Pattern(regexp = "^(ar|en)$", message = "profile.validation.uiLocaleInvalid")
        String uiLocale) {}
