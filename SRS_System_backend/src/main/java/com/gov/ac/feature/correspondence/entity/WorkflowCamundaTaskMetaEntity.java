package com.gov.ac.feature.correspondence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_camunda_task_meta", schema = "srs_system")
@Getter
@Setter
public class WorkflowCamundaTaskMetaEntity {

  @Id
  @Column(name = "task_definition_key", length = 128)
  private String taskDefinitionKey;

  @Column(name = "advances_routing_cursor", nullable = false)
  private Boolean advancesRoutingCursor = false;

  @Column(name = "name_ar")
  private String nameAr;

  @Column(name = "name_en")
  private String nameEn;
}
