package com.gov.ac.feature.lookups.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lookup_catalog", schema = "srs_system")
@Getter
@Setter
public class LookupCatalogEntity {

  @Id
  @Column(name = "lookup_code", length = 64, nullable = false)
  private String lookupCode;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_lookup_code", referencedColumnName = "lookup_code")
  private LookupCatalogEntity parentCatalog;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;
}
