package com.gov.ac.modules.notification.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsDispatchRequest(@NotBlank String phoneE164, @NotBlank String message) {}
