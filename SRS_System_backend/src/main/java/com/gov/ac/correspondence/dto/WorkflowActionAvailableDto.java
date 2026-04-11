package com.gov.ac.correspondence.dto;

/** A workflow action the current user may execute on a correspondence (from {@code workflow_action_type}). */
public record WorkflowActionAvailableDto(
    long id,
    String code,
    String nameAr,
    String nameEn,
    boolean requiresComment,
    int sortOrder,
    String uiVariant) {}
