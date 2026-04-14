package com.gov.ac.feature.correspondence.mapper;

import com.gov.ac.feature.correspondence.dto.CorrespondenceCreatedResponseDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceCreateMapper {

  public CorrespondenceCreatedResponseDto toCreatedResponse(
      CorrespondenceEntity correspondence,
      WorkflowInstanceEntity workflowInstance,
      String camundaProcessInstanceId) {
    return CorrespondenceCreatedResponseDto.builder()
        .id(correspondence.getId())
        .referenceNumber(correspondence.getReferenceNumber())
        .correspondenceTypeCode(correspondence.getCorrespondenceType().getCode())
        .correspondenceStatusCode(correspondence.getCorrespondenceStatus().getCode())
        .workflowInstanceId(workflowInstance.getId())
        .processDefinitionKey(workflowInstance.getProcessDefinitionKey())
        .camundaProcessInstanceId(camundaProcessInstanceId)
        .createdAt(correspondence.getCreatedAt())
        .build();
  }
}
