package com.gov.ac.domain.correspondence;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.org.Classification;
import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.org.Organization;
import com.gov.ac.domain.workflow.ServiceWorkflowRoute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence", schema = "srs_system")
@NamedEntityGraph(
    name = "Correspondence.list",
    attributeNodes = {
      @NamedAttributeNode("correspondenceType"),
      @NamedAttributeNode("correspondenceStatus"),
      @NamedAttributeNode("priority"),
      @NamedAttributeNode("ownerDepartment")
    })
@Getter
@Setter
public class Correspondence extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "reference_number", nullable = false)
  private String referenceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_type_id", nullable = false)
  private CorrespondenceType correspondenceType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_status_id", nullable = false)
  private CorrespondenceStatus correspondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "priority_id", nullable = false)
  private Priority priority;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "confidentiality_id", nullable = false)
  private Confidentiality confidentiality;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "classification_id", nullable = false)
  private Classification classification;

  @Column(nullable = false, length = 500)
  private String subject;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "body_html", columnDefinition = "text")
  private String bodyHtml;

  @Column(name = "reply_draft_html", columnDefinition = "text")
  private String replyDraftHtml;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_organization_id")
  private Organization senderOrganization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_organization_id")
  private Organization recipientOrganization;

  @Column(name = "external_reference_number")
  private String externalReferenceNumber;

  @Column(name = "external_reference_date")
  private LocalDate externalReferenceDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_department_id")
  private Department ownerDepartment;

  @Column(name = "due_date")
  private Instant dueDate;

  @Column(name = "barcode_value")
  private String barcodeValue;

  @Column(name = "total_attachment_bytes", nullable = false)
  private Long totalAttachmentBytes = 0L;

  @Column(name = "workflow_route_mode", nullable = false, length = 16)
  private String workflowRouteMode = "AUTO";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_workflow_route_id")
  private ServiceWorkflowRoute serviceWorkflowRoute;

  @Column(name = "supply_transaction", nullable = false)
  private Boolean supplyTransaction = false;

  @Column(name = "beneficiary_name", length = 500)
  private String beneficiaryName;

  @Column(name = "beneficiary_organization", length = 500)
  private String beneficiaryOrganization;

  @Column(name = "beneficiary_identifier", length = 128)
  private String beneficiaryIdentifier;
}
