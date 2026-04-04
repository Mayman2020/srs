package com.gov.ac.feature.auth.dto;

import java.util.UUID;

public record LoginResponse(String accessToken, UUID userId, String username) {}
