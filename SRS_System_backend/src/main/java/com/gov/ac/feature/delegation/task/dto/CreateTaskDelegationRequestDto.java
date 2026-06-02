package com.gov.ac.feature.delegation.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for creating a task delegation. Either {@code camundaTaskId} or
 * {@code correspondenceId} must be set when {@code scopeType=TASK}; the service rejects malformed
 * combinations.
 */
public record CreateTaskDelegationRequestDto(
    @NotNull UUID delegateUserId,
    @NotNull @Size(max = 32) String scopeType,
    UUID correspondenceId,
    @Size(max = 64) String camundaTaskId,
    @Size(max = 64) String processInstanceId,
    String allowedCorrespondenceTypeCodes,
    String allowedConfidentialityCodes,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validTo,
    String notes,
    UUID authorityDelegationId) {}
