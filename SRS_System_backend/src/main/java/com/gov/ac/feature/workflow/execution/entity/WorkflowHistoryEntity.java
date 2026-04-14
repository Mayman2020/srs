package com.gov.ac.feature.workflow.execution.entity;

import com.gov.ac.feature.shared.entity.AuditableEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
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
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_history", schema = "srs_system")
@Getter
@Setter
public class WorkflowHistoryEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_instance_id")
  private WorkflowInstanceEntity workflowInstance;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_history_event_type_id", nullable = false)
  private WorkflowHistoryEventTypeEntity eventType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_type_id")
  private WorkflowActionTypeEntity workflowActionType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_id")
  private WorkflowActionEntity workflowAction;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private AppUserEntity actor;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "sequence_no", nullable = false)
  private Integer sequenceNo;

  @Column(name = "primary_comment_text", columnDefinition = "text")
  private String primaryCommentText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> detail;

  @Column(name = "sla_due_at")
  private Instant slaDueAt;

  @Column(name = "sla_expected_at")
  private Instant slaExpectedAt;

  @Column(name = "sla_breached_at")
  private Instant slaBreachedAt;

  @Column(name = "actual_duration_ms")
  private Long actualDurationMs;

  @Column(name = "remaining_sla_ms")
  private Long remainingSlaMs;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_correspondence_status_id")
  private CorrespondenceStatusEntity previousCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "new_correspondence_status_id")
  private CorrespondenceStatusEntity newCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "priority_id_at_event")
  private PriorityEntity priorityAtEvent;

  @Column(name = "camunda_task_id")
  private String camundaTaskId;

  @Column(name = "camunda_activity_id")
  private String camundaActivityId;

  @Column(name = "source_system", nullable = false)
  private String sourceSystem = "AC_APP";
}
