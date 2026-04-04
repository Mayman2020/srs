package com.gov.ac.correspondence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorrespondenceCreateForm {

  @NotBlank
  @Size(max = 64)
  private String correspondenceTypeCode;

  @NotBlank
  @Size(max = 64)
  private String priorityCode;

  @NotBlank
  @Size(max = 64)
  private String confidentialityCode;

  @NotBlank
  @Size(max = 64)
  private String classificationCode;

  @NotBlank
  @Size(max = 500)
  private String subject;

  @Size(max = 20000)
  private String description;

  @Size(max = 500000)
  private String bodyHtml;

  private Long senderOrganizationId;
  private Long recipientOrganizationId;

  @Size(max = 128)
  private String externalReferenceNumber;

  private LocalDate externalReferenceDate;
  private Long ownerDepartmentId;
  private Instant dueDate;

  @Size(max = 100)
  private String barcodeValue;

  /** Stored as {@code correspondence_comment} and mirrored on {@code workflow_history}. */
  @Size(max = 20000)
  private String primaryComment;

  @Valid @Size(max = 30) private List<CorrespondenceAttachmentForm> attachments;
}
