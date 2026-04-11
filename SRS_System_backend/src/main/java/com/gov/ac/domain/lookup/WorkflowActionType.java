package com.gov.ac.domain.lookup;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.user.Role;
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
public class WorkflowActionType extends SoftDeletableEntity {

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
  private CorrespondenceStatus allowedFromCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_correspondence_status_id")
  private CorrespondenceStatus nextCorrespondenceStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_role_id")
  private Role requiredRole;

  @Column(name = "requires_comment", nullable = false)
  private Boolean requiresComment = false;

  @Column(name = "show_in_task_decision_ui", nullable = false)
  private Boolean showInTaskDecisionUi = false;

  /** Semantic button style for task UI: primary, secondary, danger, warning, success (Flyway V28). */
  @Column(name = "ui_variant", nullable = false, length = 32)
  private String uiVariant = "secondary";
}
