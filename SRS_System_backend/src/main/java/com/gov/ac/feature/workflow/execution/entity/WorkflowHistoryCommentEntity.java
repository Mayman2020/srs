package com.gov.ac.feature.workflow.execution.entity;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_history_comment", schema = "srs_system")
@Getter
@Setter
public class WorkflowHistoryCommentEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_history_id", nullable = false)
  private WorkflowHistoryEntity workflowHistory;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_user_id", nullable = false)
  private AppUserEntity author;

  @Column(nullable = false, columnDefinition = "text")
  private String body;
}
