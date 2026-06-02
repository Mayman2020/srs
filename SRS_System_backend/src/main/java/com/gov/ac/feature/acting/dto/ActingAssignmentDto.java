package com.gov.ac.feature.acting.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActingAssignmentDto(
    UUID id,
    UUID absentUserId,
    String absentUsername,
    UUID actingUserId,
    String actingUsername,
    Long departmentId,
    boolean includeDepartmentSubtree,
    String orgLevelCode,
    Long correspondenceTypeId,
    Long confidentialityId,
    Long workflowActionTypeId,
    String processDefinitionKey,
    String taskDefinitionKey,
    LocalDate validFrom,
    LocalDate validTo,
    String notes,
    Instant revokedAt,
    boolean active,
    String lifecycleStatus) {}
