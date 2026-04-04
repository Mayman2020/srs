package com.gov.ac.feature.new_transaction_details.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowHistoryEntryDto(
    Long id,
    UUID correspondenceId,
    String eventTypeCode,
    String workflowActionTypeCode,
    Long workflowActionId,
    UUID actorUserId,
    String actorDisplayName,
    Instant occurredAt,
    Integer sequenceNo,
    String primaryCommentText,
    Map<String, Object> detail,
    Instant slaDueAt,
    Instant slaBreachedAt,
    Long actualDurationMs,
    String previousStatusCode,
    String newStatusCode,
    String camundaTaskId) {}
