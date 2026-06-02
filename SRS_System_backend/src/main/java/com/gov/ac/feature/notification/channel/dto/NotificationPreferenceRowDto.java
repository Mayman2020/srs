package com.gov.ac.feature.notification.channel.dto;

import java.util.UUID;

public record NotificationPreferenceRowDto(
    UUID id, String eventTypeCode, String channelCode, boolean enabled) {}
