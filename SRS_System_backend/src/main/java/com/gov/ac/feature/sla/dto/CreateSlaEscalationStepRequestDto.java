package com.gov.ac.feature.sla.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSlaEscalationStepRequestDto(
    @NotNull @Min(0) Integer stepOrder,
    @NotBlank @Size(max = 48) String actionCode,
    @NotNull @Min(0) Integer delayAfterBreachMinutes,
    @Size(max = 64) String targetRoleCode,
    String description,
    Boolean active) {}
