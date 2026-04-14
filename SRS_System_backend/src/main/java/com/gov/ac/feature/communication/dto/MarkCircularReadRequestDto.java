package com.gov.ac.feature.communication.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkCircularReadRequestDto(@NotBlank String userId) {}
