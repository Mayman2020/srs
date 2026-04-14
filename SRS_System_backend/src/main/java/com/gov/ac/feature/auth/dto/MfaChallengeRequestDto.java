package com.gov.ac.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaChallengeRequestDto(@NotBlank String username, @NotBlank String channel) {}
