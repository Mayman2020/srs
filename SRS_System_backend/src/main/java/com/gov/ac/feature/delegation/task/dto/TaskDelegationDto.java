package com.gov.ac.feature.delegation.task.dto;

import com.gov.ac.feature.correspondence.dto.UserSummaryDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read DTO for a {@code task_delegation} row. {@code revokedAt} is non-null iff the row is no
 * longer active (either explicitly revoked or expired by the system job).
 */
public record TaskDelegationDto(
    UUID id,
    UserSummaryDto delegator,
    UserSummaryDto delegate,
    String scopeType,
    UUID correspondenceId,
    String camundaTaskId,
    String processInstanceId,
    String allowedCorrespondenceTypeCodes,
    String allowedConfidentialityCodes,
    LocalDate validFrom,
    LocalDate validTo,
    String notes,
    Instant revokedAt,
    UUID revokedBy,
    UUID authorityDelegationId,
    boolean active) {}
