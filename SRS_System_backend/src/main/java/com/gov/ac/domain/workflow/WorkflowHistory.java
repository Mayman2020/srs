package com.gov.ac.domain.workflow;

import com.gov.ac.domain.base.AuditableEntity;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.domain.user.AppUser;
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
@Table(name = "workflow_history")
@Getter
@Setter
public class WorkflowHistory extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private Correspondence correspondence;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_instance_id")
  private WorkflowInstance workflowInstance;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_history_event_type_id", nullable = false)
  private WorkflowHistoryEventType eventType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_type_id")
  private WorkflowActionType workflowActionType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_action_id")
  private WorkflowAction workflowAction;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private AppUser actor;

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
  private CorrespondenceStatus previousCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "new_correspondence_status_id")
  private CorrespondenceStatus newCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "priority_id_at_event")
  private Priority priorityAtEvent;

  @Column(name = "camunda_task_id")
  private String camundaTaskId;

  @Column(name = "camunda_activity_id")
  private String camundaActivityId;

  @Column(name = "source_system", nullable = false)
  private String sourceSystem = "AC_APP";
}
