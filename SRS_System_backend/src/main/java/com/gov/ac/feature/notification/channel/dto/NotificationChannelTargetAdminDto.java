package com.gov.ac.feature.notification.channel.dto;

import java.util.UUID;

public record NotificationChannelTargetAdminDto(
    UUID id,
    String channelCode,
    String targetCode,
    String targetUrl,
    String signingSecretRef,
    boolean enabled,
    String description) {}
