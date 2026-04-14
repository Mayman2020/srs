package com.gov.ac.feature.notification.dispatch.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsDispatchRequestDto(@NotBlank String phoneE164, @NotBlank String message) {}
