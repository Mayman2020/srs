package com.gov.ac.feature.sla.dto;

/** Display DTO for {@link com.gov.ac.feature.sla.entity.SlaEscalationStepEntity}. */
public record SlaEscalationStepDto(
    Long id,
    Integer stepOrder,
    String actionCode,
    Integer delayAfterBreachMinutes,
    String targetRoleCode,
    String description,
    boolean active) {}
