package com.gov.ac.feature.delegation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAuthorityDelegationRequest(
    @NotNull UUID delegateUserId,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validTo,
    String allowedCorrespondenceTypeCodes,
    String allowedConfidentialityCodes,
    Boolean canSignOnBehalf,
    String notes) {}
