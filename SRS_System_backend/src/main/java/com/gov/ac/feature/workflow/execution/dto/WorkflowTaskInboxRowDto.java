package com.gov.ac.feature.workflow.execution.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Row in the workflow task inbox — a single open Camunda user task for the caller, joined with
 * the originating correspondence so the UI can render an actionable list (assignee or candidate
 * for the caller via assignment, candidate user, or any of the user's role-based candidate
 * groups).
 */
public record WorkflowTaskInboxRowDto(
    String taskId,
    String taskName,
    String taskDefinitionKey,
    String assigneeUserId,
    String processInstanceId,
    Instant createdAt,
    Instant dueDate,
    UUID correspondenceId,
    String correspondenceReferenceNumber,
    String correspondenceTitle,
    String correspondenceTypeCode,
    String correspondenceStatusCode,
    String priorityCode,
    String currentLevelCode,
    Long currentDepartmentId,
    /**
     * UUID (string) of the user who would have held this task if no task delegation had rewired
     * it. Null when the caller is the canonical assignee — i.e. the task is not currently routed
     * via a delegation. When non-null, the caller is the acting delegate.
     */
    String originalAssigneeUserId,
    /** {@code true} when the caller is acting as someone else's delegate on this task. */
    boolean actingAsDelegate,
    /** UUID (string) of the active task_delegation row that rewired this task, if any. */
    String taskDelegationId,
    /** UUID string of the workflow's first resolved assignee (before acting / delegation). */
    String workflowDirectAssigneeUserId,
    /** UUID string of the absent user when acting-manager coverage is in effect. */
    String actingForAbsentUserId,
    /** UUID string of the acting_assignment row when acting coverage is in effect. */
    String actingAssignmentId,
    /** {@code true} when the caller is the acting manager (substitute) on this task. */
    boolean actingAsManager) {}
