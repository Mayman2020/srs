package com.gov.ac.feature.correspondence.dto;

import com.gov.ac.common.audit.UserAuditRefDto;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
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
  UserAuditRefDto createdByUser;
  UserAuditRefDto updatedByUser;

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

  /**
   * Read receipt for the calling user, or {@code null} if read tracking has not yet recorded one
   * (e.g. tracking failed isolation, or the first open is still in flight). Added in Slice 1 of
   * the defense-grade hardening phase; clients must treat it as optional.
   */
  CorrespondenceReadReceiptDto myReadReceipt;

  /**
   * Whether the workspace UI should expose the Acknowledge action for this correspondence.
   * Always {@code true} in Slice 1; will become policy-driven in Slice 6 (SLA policy engine).
   */
  boolean acknowledgementSupported;
}
