package com.gov.ac.feature.sla.entity;

import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * DB-driven SLA policy. A row matches a task when every non-null criterion equals the task's
 * value (NULL = wildcard). Resolution at evaluation time prefers the highest-specificity active
 * row, with policy {@code id} as a stable tiebreaker.
 *
 * <p>Lifecycle: rows are soft-deleted ({@code deleted_at}) rather than physically removed so audit
 * queries that reference {@code sla_breach_event.sla_policy_id} keep their join.
 */
@Entity
@Table(name = "sla_policy", schema = "srs_system")
@Getter
@Setter
public class SlaPolicyEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_type_id")
  private CorrespondenceTypeEntity correspondenceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "priority_id")
  private PriorityEntity priority;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "confidentiality_id")
  private ConfidentialityEntity confidentiality;

  /** Optional Q/L/K/S level code; matches against {@code workflow_instance.current_level_code}. */
  @Column(name = "org_level_code", length = 8)
  private String orgLevelCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_type_id")
  private WorkflowActionTypeEntity workflowActionType;

  @Column(name = "target_hours", nullable = false)
  private Integer targetHours;

  @Column(name = "breach_grace_minutes", nullable = false)
  private Integer breachGraceMinutes = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  /**
   * Specificity score: number of non-null criteria. Higher = more specific. Computed in code
   * (rather than as a generated column) so unit tests can exercise it without a DB.
   */
  public int specificity() {
    int s = 0;
    if (correspondenceType != null) s++;
    if (priority != null) s++;
    if (confidentiality != null) s++;
    if (orgLevelCode != null && !orgLevelCode.isBlank()) s++;
    if (workflowActionType != null) s++;
    return s;
  }
}
