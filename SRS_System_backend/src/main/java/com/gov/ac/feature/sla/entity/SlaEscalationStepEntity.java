package com.gov.ac.feature.sla.entity;

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
 * Ordered escalation step belonging to an {@link SlaPolicyEntity}. {@code stepOrder} positions the
 * step in the sequence (0 is first); {@code delayAfterBreachMinutes} is the soft window past the
 * breach moment before the step is allowed to fire.
 *
 * <p>Codes are enforced by the V16 CHECK constraint:
 * <ul>
 *   <li>{@code NOTIFY_MANAGER} — notify the assignee's manager chain.
 *   <li>{@code REASSIGN_TO_DELEGATE} — auto-reassign to the active authority delegate.
 *   <li>{@code ESCALATE_TO_HIGHER_LEVEL} — notify users at the next-higher Q/L/K/S level.
 *   <li>{@code NOTIFY_AUDIT_ADMIN} — notify auditors and system administrators.
 * </ul>
 */
@Entity
@Table(name = "sla_escalation_step", schema = "srs_system")
@Getter
@Setter
public class SlaEscalationStepEntity extends SoftDeletableEntity {

  public static final String ACTION_NOTIFY_MANAGER = "NOTIFY_MANAGER";
  public static final String ACTION_REASSIGN_TO_DELEGATE = "REASSIGN_TO_DELEGATE";
  public static final String ACTION_ESCALATE_TO_HIGHER_LEVEL = "ESCALATE_TO_HIGHER_LEVEL";
  public static final String ACTION_NOTIFY_AUDIT_ADMIN = "NOTIFY_AUDIT_ADMIN";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sla_policy_id", nullable = false)
  private SlaPolicyEntity policy;

  @Column(name = "step_order", nullable = false)
  private Integer stepOrder;

  @Column(name = "action_code", nullable = false, length = 48)
  private String actionCode;

  @Column(name = "delay_after_breach_minutes", nullable = false)
  private Integer delayAfterBreachMinutes = 0;

  @Column(name = "target_role_code", length = 64)
  private String targetRoleCode;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
