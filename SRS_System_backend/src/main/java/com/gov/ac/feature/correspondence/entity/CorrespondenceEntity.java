package com.gov.ac.feature.correspondence.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;
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
    name = "CorrespondenceEntity.list",
    attributeNodes = {
      @NamedAttributeNode("correspondenceType"),
      @NamedAttributeNode("correspondenceStatus"),
      @NamedAttributeNode("priority"),
      @NamedAttributeNode("ownerDepartment")
    })
@Getter
@Setter
public class CorrespondenceEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "reference_number", nullable = false)
  private String referenceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_type_id", nullable = false)
  private CorrespondenceTypeEntity correspondenceType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_status_id", nullable = false)
  private CorrespondenceStatusEntity correspondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "priority_id", nullable = false)
  private PriorityEntity priority;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "confidentiality_id", nullable = false)
  private ConfidentialityEntity confidentiality;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "classification_id", nullable = false)
  private ClassificationEntity classification;

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
  private OrganizationEntity senderOrganization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_organization_id")
  private OrganizationEntity recipientOrganization;

  @Column(name = "external_reference_number")
  private String externalReferenceNumber;

  @Column(name = "external_reference_date")
  private LocalDate externalReferenceDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_department_id")
  private DepartmentEntity ownerDepartment;

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
  private ServiceWorkflowRouteEntity serviceWorkflowRoute;

  @Column(name = "supply_transaction", nullable = false)
  private Boolean supplyTransaction = false;

  @Column(name = "beneficiary_name", length = 500)
  private String beneficiaryName;

  @Column(name = "beneficiary_organization", length = 500)
  private String beneficiaryOrganization;

  @Column(name = "beneficiary_identifier", length = 128)
  private String beneficiaryIdentifier;
}
