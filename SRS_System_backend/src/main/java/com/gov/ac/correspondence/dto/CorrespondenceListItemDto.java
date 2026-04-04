package com.gov.ac.correspondence.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceListItemDto {
  UUID id;
  String referenceNumber;
  String subject;
  Instant createdAt;
  Instant updatedAt;
  Instant dueDate;
  LookupLabelDto correspondenceType;
  LookupLabelDto correspondenceStatus;
  LookupLabelDto priority;
  DepartmentSummaryDto ownerDepartment;
}
