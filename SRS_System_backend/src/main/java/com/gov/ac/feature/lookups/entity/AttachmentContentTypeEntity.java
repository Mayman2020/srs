package com.gov.ac.feature.lookups.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attachment_content_type", schema = "srs_system")
@Getter
@Setter
public class AttachmentContentTypeEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String code;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(name = "max_bytes")
  private Long maxBytes;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
