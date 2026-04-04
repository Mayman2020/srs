package com.gov.ac.web.dto;

import java.util.List;

public record LookupBundleDto(
    List<LookupItemDto> correspondenceTypes,
    List<LookupItemDto> correspondenceStatuses,
    List<LookupItemDto> priorities,
    List<LookupItemDto> confidentialities,
    List<LookupItemDto> workflowActionTypes,
    List<LookupItemDto> workflowHistoryEventTypes) {}
