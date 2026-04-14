package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorrespondenceCreateFormDto {

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

  @Valid @Size(max = 30) private List<CorrespondenceAttachmentFormDto> attachments;

  /**
   * Optional: first Camunda user task goes to this user (UUID). Mutually exclusive with {@link
   * #workflowFirstCandidateGroup}.
   */
  private UUID workflowFirstAssigneeUserId;

  /**
   * Optional: first task is a candidate group for this {@code role.code} (e.g. STAFF). Any active
   * user with that role may claim the task. Mutually exclusive with {@link
   * #workflowFirstAssigneeUserId}.
   */
  @Size(max = 64)
  private String workflowFirstCandidateGroup;

  /** {@code AUTO} uses default route for the type; {@code MANUAL} requires {@link #serviceWorkflowRouteId}. */
  @Size(max = 16)
  private String workflowRouteMode = "AUTO";

  /** When {@link #workflowRouteMode} is {@code MANUAL}, id from {@code GET /api/v1/workflow-routes}. */
  private Long serviceWorkflowRouteId;

  /** When true, marks correspondence created from the «supply / توريد» flow. */
  private Boolean supplyTransaction = false;

  @Size(max = 500)
  private String beneficiaryName;

  @Size(max = 500)
  private String beneficiaryOrganization;

  @Size(max = 128)
  private String beneficiaryIdentifier;
}
