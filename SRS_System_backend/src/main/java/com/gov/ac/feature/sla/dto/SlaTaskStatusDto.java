package com.gov.ac.feature.sla.dto;

import java.time.Instant;

/**
 * Per-task SLA status used by the workflow inbox countdown chip and the correspondence-details
 * SLA panel. Computed on demand from the active policy + (optionally) the breach event row.
 */
public record SlaTaskStatusDto(
    String taskId,
    Long slaPolicyId,
    String slaPolicyCode,
    Instant taskCreatedAt,
    Instant targetAt,
    Instant breachedAt,
    Instant resolvedAt,
    boolean overdue,
    long secondsRemaining,
    long secondsOverdue,
    Integer lastStepExecutedOrder,
    String lastStepActionCode,
    Integer stepsExecutedTotal) {}
