package com.gov.ac.feature.sla.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for creating or updating an SLA policy. Any criterion left {@code null} acts as a
 * wildcard. Steps are replaced wholesale on update (no PATCH semantics) to keep policy authoring
 * straightforward.
 */
public record CreateSlaPolicyRequestDto(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 255) String nameAr,
    @NotBlank @Size(max = 255) String nameEn,
    String description,
    Long correspondenceTypeId,
    Long priorityId,
    Long confidentialityId,
    @Size(max = 8) String orgLevelCode,
    Long workflowActionTypeId,
    @NotNull @Min(1) Integer targetHours,
    @Min(0) Integer breachGraceMinutes,
    Boolean active,
    @Valid List<CreateSlaEscalationStepRequestDto> steps) {}
