package com.gov.ac.feature.delegation.dto;

import com.gov.ac.feature.correspondence.dto.UserSummaryDto;
import java.time.LocalDate;
import java.util.UUID;

public record AuthorityDelegationDto(
    UUID id,
    UserSummaryDto delegator,
    UserSummaryDto delegate,
    LocalDate validFrom,
    LocalDate validTo,
    String allowedCorrespondenceTypeCodes,
    String allowedConfidentialityCodes,
    boolean canSignOnBehalf,
    String notes) {}
