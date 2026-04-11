package com.gov.ac.domain.workflow;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.WorkflowInstanceStatus;
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
@Entity
@Table(name = "workflow_instance", schema = "srs_system")
@Getter
@Setter
public class WorkflowInstance extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private Correspondence correspondence;

  @Column(name = "process_definition_key", nullable = false)
  private String processDefinitionKey;

  @Column(name = "process_instance_id", nullable = false, unique = true)
  private String processInstanceId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_instance_status_id", nullable = false)
  private WorkflowInstanceStatus status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "business_key")
  private String businessKey;
}
