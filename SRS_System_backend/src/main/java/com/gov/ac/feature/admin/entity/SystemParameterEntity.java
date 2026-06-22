package com.gov.ac.feature.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_parameter", schema = "srs_system")
@Getter
@Setter
public class SystemParameterEntity {

  @Id
  @Column(name = "param_key", nullable = false, length = 64)
  private String paramKey;

  @Column(name = "param_value", nullable = false, length = 512)
  private String paramValue;

  @Column(name = "name_ar")
  private String nameAr;

  @Column(name = "name_en")
  private String nameEn;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
