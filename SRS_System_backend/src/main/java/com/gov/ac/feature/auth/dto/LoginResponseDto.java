package com.gov.ac.feature.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * {@code accessToken} carries claims: {@code sub}, {@code userId}, {@code username}, {@code roles},
 * {@code currentRole}. {@code refreshToken} is opaque (JTI UUID) when issued by this API.
 */
public record LoginResponseDto(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    UUID userId,
    String username,
    List<String> roles,
    String currentRole,
    String profileImageUrl) {}
