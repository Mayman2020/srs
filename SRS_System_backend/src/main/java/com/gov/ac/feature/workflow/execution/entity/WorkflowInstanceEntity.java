package com.gov.ac.feature.workflow.execution.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.WorkflowInstanceStatusEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name = "workflow_instance", schema = "srs_system")
@Getter
@Setter
public class WorkflowInstanceEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @Column(name = "process_definition_key", nullable = false)
  private String processDefinitionKey;

  @Column(name = "process_instance_id", nullable = false, unique = true)
  private String processInstanceId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_instance_status_id", nullable = false)
  private WorkflowInstanceStatusEntity status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "business_key")
  private String businessKey;

  /** JSON snapshot of the routing chain produced by {@code routingChainDelegate} (V5 column). */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "routing_chain_json", columnDefinition = "jsonb")
  private String routingChainJson;

  /** Current routing-chain level code (Q/L/K/S). */
  @Column(name = "current_level_code", length = 8)
  private String currentLevelCode;

  /** Current routing stop department id. */
  @Column(name = "current_department_id")
  private Long currentDepartmentId;

  /** Number of SLA breaches recorded against this instance. */
  @Column(name = "escalation_count", nullable = false)
  private Integer escalationCount = 0;

  /** Originator department id at process start. */
  @Column(name = "originator_department_id")
  private Long originatorDepartmentId;

  /** Final target department id at process start. */
  @Column(name = "target_department_id")
  private Long targetDepartmentId;
}
