package com.gov.ac.modules.communication.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkCircularReadRequest(@NotBlank String userId) {}
