package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * @param action Required non-blank {@code workflow_action_type.code} for Camunda {@code wfDecision}.
 * @param targetUserId Required for {@code REFER}: user who receives the active task.
 * @param targetDepartmentId Required for {@code FORWARD}: department the correspondence is routed toward.
 */
public record WorkflowActionRequestDto(
    @Size(max = 64) String action,
    @Size(max = 20000) String comment,
    UUID targetUserId,
    Long targetDepartmentId) {}
