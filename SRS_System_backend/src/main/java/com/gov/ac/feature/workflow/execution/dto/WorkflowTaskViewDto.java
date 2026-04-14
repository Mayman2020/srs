package com.gov.ac.feature.workflow.execution.dto;

import java.time.Instant;

/**
 * Business-facing view of an active user task (Camunda {@code Task} fields mapped without exposing
 * engine types to API layers).
 */
public record WorkflowTaskViewDto(
    String taskId,
    String taskDefinitionKey,
    String name,
    String assignee,
    String processInstanceId,
    Instant created) {}
