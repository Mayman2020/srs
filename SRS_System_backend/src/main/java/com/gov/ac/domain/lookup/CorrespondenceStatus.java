package com.gov.ac.domain.lookup;

import com.gov.ac.domain.base.SoftDeletableEntity;
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
@Table(name = "correspondence_status")
@Getter
@Setter
public class CorrespondenceStatus extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_type_id")
  private CorrespondenceType correspondenceType;

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

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
