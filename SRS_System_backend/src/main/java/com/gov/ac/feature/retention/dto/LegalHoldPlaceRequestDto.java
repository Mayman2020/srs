package com.gov.ac.feature.retention.dto;

import java.util.UUID;

public record LegalHoldPlaceRequestDto(UUID correspondenceId, String reason) {}
