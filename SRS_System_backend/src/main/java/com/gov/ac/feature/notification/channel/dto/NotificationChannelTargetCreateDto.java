package com.gov.ac.feature.notification.channel.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationChannelTargetCreateDto(
    @NotBlank String channelCode,
    @NotBlank String targetCode,
    String targetUrl,
    String signingSecretRef,
    boolean enabled,
    String description) {}
