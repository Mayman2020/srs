package com.gov.ac.modules.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * {@code accessToken} carries claims: {@code sub}, {@code userId}, {@code username}, {@code roles},
 * {@code currentRole}. {@code refreshToken} is opaque (JTI UUID) when issued by this API.
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    UUID userId,
    String username,
    List<String> roles,
    String currentRole) {}
