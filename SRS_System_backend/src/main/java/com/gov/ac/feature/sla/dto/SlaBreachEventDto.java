package com.gov.ac.feature.sla.dto;

import java.time.Instant;
import java.util.UUID;

public record SlaBreachEventDto(
    Long id,
    String taskId,
    String processInstanceId,
    UUID workflowInstanceId,
    UUID correspondenceId,
    String correspondenceReferenceNumber,
    Long slaPolicyId,
    String slaPolicyCode,
    Instant targetAt,
    Instant breachedAt,
    Integer lastStepExecutedOrder,
    Instant lastStepExecutedAt,
    String lastStepActionCode,
    Integer stepsExecutedTotal,
    Instant resolvedAt,
    String resolutionOutcome,
    Instant createdAt,
    Instant updatedAt) {}
