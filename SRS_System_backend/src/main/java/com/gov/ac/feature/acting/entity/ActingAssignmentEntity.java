package com.gov.ac.feature.acting.entity;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.shared.entity.AuditableEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "acting_assignment", schema = "srs_system")
@Getter
@Setter
public class ActingAssignmentEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "absent_user_id", nullable = false)
  private AppUserEntity absentUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "acting_user_id", nullable = false)
  private AppUserEntity actingUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  private DepartmentEntity department;

  @Column(name = "include_department_subtree", nullable = false)
  private boolean includeDepartmentSubtree;

  @Column(name = "org_level_code", length = 8)
  private String orgLevelCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_type_id")
  private CorrespondenceTypeEntity correspondenceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "confidentiality_id")
  private ConfidentialityEntity confidentiality;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_type_id")
  private WorkflowActionTypeEntity workflowActionType;

  @Column(name = "process_definition_key", length = 128)
  private String processDefinitionKey;

  @Column(name = "task_definition_key", length = 255)
  private String taskDefinitionKey;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = false)
  private LocalDate validTo;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_by")
  private UUID revokedBy;

  public boolean isActiveOn(LocalDate day) {
    return revokedAt == null
        && validFrom != null
        && validTo != null
        && !day.isBefore(validFrom)
        && !day.isAfter(validTo);
  }

  public int specificityScore() {
    int n = 0;
    if (department != null) n++;
    if (orgLevelCode != null && !orgLevelCode.isBlank()) n++;
    if (correspondenceType != null) n++;
    if (confidentiality != null) n++;
    if (workflowActionType != null) n++;
    if (processDefinitionKey != null && !processDefinitionKey.isBlank()) n++;
    if (taskDefinitionKey != null && !taskDefinitionKey.isBlank()) n++;
    return n;
  }
}
