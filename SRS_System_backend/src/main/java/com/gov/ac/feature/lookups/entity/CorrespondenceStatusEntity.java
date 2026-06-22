package com.gov.ac.feature.lookups.entity;

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

@Entity
@Table(name = "correspondence_status", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceStatusEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_type_id")
  private CorrespondenceTypeEntity correspondenceType;

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

  @Column(name = "is_terminal", nullable = false)
  private Boolean terminal = false;

  /** When false, user-initiated cancel is blocked (non-terminal states only; see Flyway V28). */
  @Column(name = "allows_cancel", nullable = false)
  private Boolean allowsCancel = true;

  /**
   * Exactly one active row: status applied when a correspondence is cancelled (resolved in Java instead
   * of hardcoding {@code CANCELLED}).
   */
  @Column(name = "cancel_outcome", nullable = false)
  private Boolean cancelOutcome = false;

  /** Exactly one active row: status applied when Camunda process completes successfully. */
  @Column(name = "process_complete_outcome", nullable = false)
  private Boolean processCompleteOutcome = false;

  /** Home dashboard KPI bucket; see Flyway V26 and {@code KpiSegmentCodes}. */
  @Column(name = "kpi_segment", length = 32)
  private String kpiSegment;

  /**
   * Badge style for UI (success, danger, warning, info, secondary, neutral); see Flyway V29 — not derived
   * from {@link #code}.
   */
  @Column(name = "ui_variant", nullable = false, length = 32)
  private String uiVariant = "neutral";

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
