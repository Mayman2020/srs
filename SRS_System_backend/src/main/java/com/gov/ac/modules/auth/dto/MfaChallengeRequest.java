package com.gov.ac.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaChallengeRequest(@NotBlank String username, @NotBlank String channel) {}
