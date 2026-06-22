package com.gov.ac.feature.lookups.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.entity.RoleEntity;
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
@Table(name = "workflow_action_type", schema = "srs_system")
@Getter
@Setter
public class WorkflowActionTypeEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String code;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "allowed_from_correspondence_status_id")
  private CorrespondenceStatusEntity allowedFromCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_correspondence_status_id")
  private CorrespondenceStatusEntity nextCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_role_id")
  private RoleEntity requiredRole;

  @Column(name = "requires_comment", nullable = false)
  private Boolean requiresComment = false;

  /**
   * Slice 5 — when TRUE, completing this action requires a VALID+VERIFIED
   * {@code document_signature} on the latest version of every active attachment by the actor.
   */
  @Column(name = "requires_signature", nullable = false)
  private Boolean requiresSignature = false;

  @Column(name = "show_in_task_decision_ui", nullable = false)
  private Boolean showInTaskDecisionUi = false;

  /** Semantic button style for task UI: primary, secondary, danger, warning, success (Flyway V28). */
  @Column(name = "ui_variant", nullable = false, length = 32)
  private String uiVariant = "secondary";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_permission_id")
  private PermissionEntity requiredPermission;

  @Column(name = "requires_target_user", nullable = false)
  private Boolean requiresTargetUser = false;

  @Column(name = "requires_target_department", nullable = false)
  private Boolean requiresTargetDepartment = false;

  @Column(name = "keeps_task_open", nullable = false)
  private Boolean keepsTaskOpen = false;

  @Column(name = "suppress_process_end_status", nullable = false)
  private Boolean suppressProcessEndStatus = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notification_event_type_id")
  private NotificationEventTypeEntity notificationEventType;

  /** When TRUE, multi-instance routing loop exits early (REJECT, RETURN). */
  @Column(name = "terminates_routing_chain", nullable = false)
  private Boolean terminatesRoutingChain = false;
}
