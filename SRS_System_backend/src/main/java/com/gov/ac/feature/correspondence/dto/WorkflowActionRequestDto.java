package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Size;

/**
 * @param action Required non-blank {@code workflow_action_type.code} for Camunda {@code wfDecision}.
 */
public record WorkflowActionRequestDto(
    @Size(max = 64) String action, @Size(max = 20000) String comment) {}
