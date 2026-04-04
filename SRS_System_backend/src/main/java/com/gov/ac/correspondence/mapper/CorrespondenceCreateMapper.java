package com.gov.ac.correspondence.mapper;

import com.gov.ac.correspondence.dto.CorrespondenceCreatedResponse;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.workflow.WorkflowInstance;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceCreateMapper {

  public CorrespondenceCreatedResponse toCreatedResponse(
      Correspondence correspondence,
      WorkflowInstance workflowInstance,
      String camundaProcessInstanceId) {
    return CorrespondenceCreatedResponse.builder()
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
