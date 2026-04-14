package com.gov.ac.feature.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecideLeaveRequestDto(
    @NotBlank String statusCode,
    @Size(max = 2000) String decisionNote) {}
