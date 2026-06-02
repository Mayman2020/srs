package com.gov.ac.feature.sla.mapper;

import com.gov.ac.feature.sla.dto.SlaBreachEventDto;
import com.gov.ac.feature.sla.dto.SlaEscalationStepDto;
import com.gov.ac.feature.sla.dto.SlaPolicyDto;
import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import java.util.List;

public final class SlaPolicyMapper {

  private SlaPolicyMapper() {}

  public static SlaPolicyDto toDto(SlaPolicyEntity entity, List<SlaEscalationStepEntity> steps) {
    if (entity == null) {
      return null;
    }
    return new SlaPolicyDto(
        entity.getId(),
        entity.getCode(),
        entity.getNameAr(),
        entity.getNameEn(),
        entity.getDescription(),
        entity.getCorrespondenceType() != null ? entity.getCorrespondenceType().getId() : null,
        entity.getCorrespondenceType() != null ? entity.getCorrespondenceType().getCode() : null,
        entity.getPriority() != null ? entity.getPriority().getId() : null,
        entity.getPriority() != null ? entity.getPriority().getCode() : null,
        entity.getConfidentiality() != null ? entity.getConfidentiality().getId() : null,
        entity.getConfidentiality() != null ? entity.getConfidentiality().getCode() : null,
        entity.getOrgLevelCode(),
        entity.getWorkflowActionType() != null ? entity.getWorkflowActionType().getId() : null,
        entity.getWorkflowActionType() != null ? entity.getWorkflowActionType().getCode() : null,
        entity.getTargetHours(),
        entity.getBreachGraceMinutes(),
        Boolean.TRUE.equals(entity.getActive()),
        entity.specificity(),
        steps == null ? List.of() : steps.stream().map(SlaPolicyMapper::toStepDto).toList(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public static SlaEscalationStepDto toStepDto(SlaEscalationStepEntity entity) {
    if (entity == null) {
      return null;
    }
    return new SlaEscalationStepDto(
        entity.getId(),
        entity.getStepOrder(),
        entity.getActionCode(),
        entity.getDelayAfterBreachMinutes(),
        entity.getTargetRoleCode(),
        entity.getDescription(),
        Boolean.TRUE.equals(entity.getActive()));
  }

  public static SlaBreachEventDto toBreachDto(SlaBreachEventEntity entity) {
    if (entity == null) {
      return null;
    }
    return new SlaBreachEventDto(
        entity.getId(),
        entity.getTaskId(),
        entity.getProcessInstanceId(),
        entity.getWorkflowInstance() != null ? entity.getWorkflowInstance().getId() : null,
        entity.correspondenceId(),
        entity.getCorrespondence() != null ? entity.getCorrespondence().getReferenceNumber() : null,
        entity.getPolicy() != null ? entity.getPolicy().getId() : null,
        entity.getPolicy() != null ? entity.getPolicy().getCode() : null,
        entity.getTargetAt(),
        entity.getBreachedAt(),
        entity.getLastStepExecutedOrder(),
        entity.getLastStepExecutedAt(),
        entity.getLastStepActionCode(),
        entity.getStepsExecutedTotal(),
        entity.getResolvedAt(),
        entity.getResolutionOutcome(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
