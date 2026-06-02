package com.gov.ac.feature.acting.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateActingAssignmentRequestDto(
    @NotNull UUID absentUserId,
    @NotNull UUID actingUserId,
    Long departmentId,
    Boolean includeDepartmentSubtree,
    String orgLevelCode,
    Long correspondenceTypeId,
    Long confidentialityId,
    Long workflowActionTypeId,
    String processDefinitionKey,
    String taskDefinitionKey,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validTo,
    String notes) {}
