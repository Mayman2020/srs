package com.gov.ac.domain.workflow;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.WorkflowActionType;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "workflow_action", schema = "srs_system")
@Getter
@Setter
public class WorkflowAction extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_instance_id", nullable = false)
  private WorkflowInstance workflowInstance;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private Correspondence correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_action_type_id", nullable = false)
  private WorkflowActionType actionType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private AppUser actor;

  @Column(name = "comment_text")
  private String commentText;

  @Column(columnDefinition = "jsonb")
  private String payload;

  @Column(name = "camunda_task_id")
  private String camundaTaskId;

  @Column(name = "camunda_activity_id")
  private String camundaActivityId;
}
