package com.gov.ac.correspondence.dto;

import com.gov.ac.common.audit.UserAuditRefDto;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
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
  /** Nullable; {@code app_user} for {@code created_by}. */
  UserAuditRefDto createdByUser;
  /** Nullable; {@code app_user} for {@code updated_by}. */
  UserAuditRefDto updatedByUser;
}
