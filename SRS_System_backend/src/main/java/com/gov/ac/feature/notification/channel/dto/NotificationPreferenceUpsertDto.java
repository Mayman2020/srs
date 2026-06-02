package com.gov.ac.feature.notification.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpsertDto(
    @NotBlank String eventTypeCode,
    @NotBlank String channelCode,
    @NotNull Boolean enabled) {}
