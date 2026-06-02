package com.gov.ac.feature.sla.entity;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Per-task SLA breach ledger. Created the first time the evaluation job observes a Camunda task
 * past its SLA target and updated as each escalation step fires. The unique index on {@code
 * task_id} makes the evaluation pass idempotent: re-running it with the same overdue task simply
 * re-loads this row, never creates a second.
 *
 * <p>{@code resolved_at} is stamped when the underlying Camunda task completes (via a Task
 * complete listener); the job uses {@code resolved_at IS NULL} to maintain the overdue-active
 * gauge without scanning Camunda on every tick.
 *
 * <p>Intentionally not a {@link com.gov.ac.feature.shared.entity.SoftDeletableEntity}: an
 * append-only operational ledger doesn't need {@code deleted_at} columns, just timestamps.
 */
@Entity
@Table(name = "sla_breach_event", schema = "srs_system")
@Getter
@Setter
public class SlaBreachEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "task_id", nullable = false, length = 64)
  private String taskId;

  @Column(name = "process_instance_id", length = 64)
  private String processInstanceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_instance_id")
  private WorkflowInstanceEntity workflowInstance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_id")
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sla_policy_id")
  private SlaPolicyEntity policy;

  @Column(name = "target_at", nullable = false)
  private Instant targetAt;

  @Column(name = "breached_at", nullable = false)
  private Instant breachedAt;

  /** -1 means "no step executed yet". The next step to run is {@code last + 1}. */
  @Column(name = "last_step_executed_order", nullable = false)
  private Integer lastStepExecutedOrder = -1;

  @Column(name = "last_step_executed_at")
  private Instant lastStepExecutedAt;

  @Column(name = "last_step_action_code", length = 48)
  private String lastStepActionCode;

  @Column(name = "steps_executed_total", nullable = false)
  private Integer stepsExecutedTotal = 0;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolution_outcome", length = 64)
  private String resolutionOutcome;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Helper: a row is "active" while its underlying Camunda task is still running. */
  public boolean isActive() {
    return resolvedAt == null;
  }

  /** Helper: the task id can also reference a fictional UUID when needed. */
  public UUID correspondenceId() {
    return correspondence != null ? correspondence.getId() : null;
  }
}
