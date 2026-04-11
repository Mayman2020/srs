package com.gov.ac.feature.workflow.dto;

public record ServiceWorkflowRouteDto(
    long id,
    long correspondenceTypeId,
    String correspondenceTypeCode,
    String processDefinitionKey,
    String nameAr,
    String nameEn,
    boolean defaultRoute,
    int sortOrder,
    boolean active) {}
