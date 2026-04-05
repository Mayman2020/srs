package com.gov.ac.correspondence.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CorrespondenceDetailResponse {
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

  List<CorrespondenceAttachmentDetailDto> attachments;
  List<CorrespondenceTimelineEntryDto> timeline;
  List<CorrespondenceCommentDetailDto> comments;
}
