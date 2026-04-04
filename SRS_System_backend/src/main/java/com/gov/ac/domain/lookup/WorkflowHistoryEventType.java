package com.gov.ac.domain.lookup;

import com.gov.ac.domain.base.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_history_event_type")
@Getter
@Setter
public class WorkflowHistoryEventType extends SoftDeletableEntity {

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
}
