package com.gov.ac.domain.workflow;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.lookup.CorrespondenceType;
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
@Table(name = "service_workflow_route", schema = "srs_system")
@Getter
@Setter
public class ServiceWorkflowRoute extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_type_id", nullable = false)
  private CorrespondenceType correspondenceType;

  @Column(name = "process_definition_key", nullable = false, length = 128)
  private String processDefinitionKey;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(name = "is_default_route", nullable = false)
  private boolean defaultRoute;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
