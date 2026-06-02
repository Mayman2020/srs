package com.gov.ac.feature.sla.dto;

import java.time.Instant;
import java.util.List;

/**
 * Display DTO for {@link com.gov.ac.feature.sla.entity.SlaPolicyEntity}. Codes carry the i18n
 * names so the FE can render without a second lookup roundtrip.
 */
public record SlaPolicyDto(
    Long id,
    String code,
    String nameAr,
    String nameEn,
    String description,
    Long correspondenceTypeId,
    String correspondenceTypeCode,
    Long priorityId,
    String priorityCode,
    Long confidentialityId,
    String confidentialityCode,
    String orgLevelCode,
    Long workflowActionTypeId,
    String workflowActionTypeCode,
    Integer targetHours,
    Integer breachGraceMinutes,
    boolean active,
    int specificity,
    List<SlaEscalationStepDto> steps,
    Instant createdAt,
    Instant updatedAt) {}
