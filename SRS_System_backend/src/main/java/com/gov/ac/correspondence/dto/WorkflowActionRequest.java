package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Size;

/**
 * @param action Required non-blank {@code workflow_action_type.code} for Camunda {@code wfDecision}.
 */
public record WorkflowActionRequest(
    @Size(max = 64) String action, @Size(max = 20000) String comment) {}
