package com.gov.ac.feature.lookups.dto;

import java.util.List;

public record LookupBundleDto(
    List<LookupItemDto> correspondenceTypes,
    List<LookupItemDto> correspondenceStatuses,
    List<LookupItemDto> priorities,
    List<LookupItemDto> confidentialities,
    List<LookupItemDto> classifications,
    List<LookupItemDto> workflowActionTypes,
    List<LookupItemDto> workflowHistoryEventTypes,
    List<LookupItemDto> orgVisualNodeStatuses) {}
