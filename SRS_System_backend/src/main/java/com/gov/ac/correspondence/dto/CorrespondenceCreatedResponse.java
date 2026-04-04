package com.gov.ac.correspondence.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceCreatedResponse {

  UUID id;
  String referenceNumber;
  String correspondenceTypeCode;
  String correspondenceStatusCode;
  UUID workflowInstanceId;
  String processDefinitionKey;
  String camundaProcessInstanceId;
  Instant createdAt;
}
