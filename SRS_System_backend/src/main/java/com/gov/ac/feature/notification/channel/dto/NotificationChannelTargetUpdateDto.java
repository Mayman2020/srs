package com.gov.ac.feature.notification.channel.dto;

public record NotificationChannelTargetUpdateDto(
    String targetUrl, String signingSecretRef, Boolean enabled, String description) {}
