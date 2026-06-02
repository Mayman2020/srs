package com.gov.ac.feature.retention.entity;

import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "retention_policy", schema = "srs_system")
@Getter
@Setter
public class RetentionPolicyEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "name_ar", columnDefinition = "text")
  private String nameAr;

  @Column(name = "name_en", columnDefinition = "text")
  private String nameEn;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "applies_to", nullable = false, length = 32)
  private String appliesTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correspondence_type_id")
  private CorrespondenceTypeEntity correspondenceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "confidentiality_id")
  private ConfidentialityEntity confidentiality;

  @Column(name = "retain_for_days", nullable = false)
  private Integer retainForDays;

  @Column(name = "action_after", nullable = false, length = 16)
  private String actionAfter;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;
}
