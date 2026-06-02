package com.gov.ac.feature.delegation.task.entity;

import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
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

/**
 * Time-bounded delegation of Camunda user-task assignment. Coexists with {@link
 * AuthorityDelegationEntity} (administrative-only delegation from V1) without replacing it.
 *
 * <p>Active row predicate (used by every read path): {@code revoked_at IS NULL AND today BETWEEN
 * valid_from AND valid_to}. The {@code scope_type} discriminator and the optional {@code
 * camunda_task_id} / {@code correspondence_id} / csv filters together determine which open tasks
 * the delegate inherits.
 */
@Entity
@Table(name = "task_delegation", schema = "srs_system")
@Getter
@Setter
public class TaskDelegationEntity extends AuditableEntity {

  /** Single specific Camunda user task or, if {@code camunda_task_id} is not yet known, the */
  /** entire correspondence. */
  public static final String SCOPE_TASK = "TASK";

  /** Every open task whose correspondence matches the allowed type + confidentiality csv filters. */
  public static final String SCOPE_TYPE_CONFIDENTIALITY = "TYPE_CONFIDENTIALITY";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "delegator_user_id", nullable = false)
  private AppUserEntity delegatorUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "delegate_user_id", nullable = false)
  private AppUserEntity delegateUser;

  @Column(name = "scope_type", nullable = false, length = 32)
  private String scopeType;

  @Column(name = "correspondence_id")
  private UUID correspondenceId;

  @Column(name = "camunda_task_id", length = 64)
  private String camundaTaskId;

  @Column(name = "process_instance_id", length = 64)
  private String processInstanceId;

  @Column(name = "allowed_correspondence_type_codes", columnDefinition = "text")
  private String allowedCorrespondenceTypeCodes;

  @Column(name = "allowed_confidentiality_codes", columnDefinition = "text")
  private String allowedConfidentialityCodes;

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

  /**
   * Optional link to an {@link AuthorityDelegationEntity} that authorised this task-level
   * delegation (e.g. an administrative blanket delegation that a user reuses to delegate a single
   * task).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "authority_delegation_id")
  private AuthorityDelegationEntity authorityDelegation;

  public boolean isActiveOn(LocalDate day) {
    return revokedAt == null
        && validFrom != null
        && validTo != null
        && !day.isBefore(validFrom)
        && !day.isAfter(validTo);
  }
}
