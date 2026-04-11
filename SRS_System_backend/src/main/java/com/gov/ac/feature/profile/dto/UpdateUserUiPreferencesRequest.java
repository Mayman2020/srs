package com.gov.ac.feature.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserUiPreferencesRequest(
    @NotBlank @Pattern(regexp = "^(light|dark)$", message = "profile.validation.uiThemeInvalid")
        String uiTheme,
    @NotBlank @Pattern(regexp = "^(ar|en)$", message = "profile.validation.uiLocaleInvalid")
        String uiLocale) {}
