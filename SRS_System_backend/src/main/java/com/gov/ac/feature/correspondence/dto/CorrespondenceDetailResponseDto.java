package com.gov.ac.feature.correspondence.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceDetailResponseDto {
  UUID id;
  String referenceNumber;
  LookupLabelDto correspondenceType;
  LookupLabelDto correspondenceStatus;
  LookupLabelDto priority;
  LookupLabelDto confidentiality;
  LookupLabelDto classification;
  String subject;
  String description;
  String bodyHtml;
  String replyDraftHtml;
  OrganizationSummaryDto senderOrganization;
  OrganizationSummaryDto recipientOrganization;
  DepartmentSummaryDto ownerDepartment;
  String externalReferenceNumber;
  LocalDate externalReferenceDate;
  Instant dueDate;
  String barcodeValue;
  long totalAttachmentBytes;
  Instant createdAt;
  Instant updatedAt;

  String workflowRouteMode;
  Long serviceWorkflowRouteId;
  String workflowProcessDefinitionKey;
  boolean supplyTransaction;
  String beneficiaryName;
  String beneficiaryOrganization;
  String beneficiaryIdentifier;

  List<CorrespondenceAttachmentDetailDto> attachments;
  List<CorrespondenceTimelineEntryDto> timeline;
  List<CorrespondenceCommentDetailDto> comments;
  /** Task decisions the viewer may submit (from {@code workflow_action_type}, filtered by status/role/task). */
  List<WorkflowActionAvailableDto> availableWorkflowActions;
  /** Whether the viewer may call POST cancel (from {@code correspondence_status} metadata). */
  boolean cancelAllowed;
}
