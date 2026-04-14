package com.gov.ac.feature.workflow.routes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertServiceWorkflowRouteRequestDto(
    @NotNull Long correspondenceTypeId,
    @NotBlank @Size(max = 128) String processDefinitionKey,
    @NotBlank @Size(max = 250) String nameAr,
    @NotBlank @Size(max = 250) String nameEn,
    boolean defaultRoute,
    @NotNull Integer sortOrder,
    @NotNull Boolean active) {}
