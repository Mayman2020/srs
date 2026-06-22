package com.gov.ac.feature.organization.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Lookup row representing one of the Q/L/K/S military hierarchy levels.
 *
 * <p>Coded codes are stable application keys ({@code Q}, {@code L}, {@code K}, {@code S}).
 * {@code rankOrder} expresses authority (lower = higher) and drives chain calculations.
 */
@Entity
@Table(name = "organizational_unit_level", schema = "srs_system")
@Getter
@Setter
public class OrganizationalUnitLevelEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 8)
  private String code;

  @Column(name = "name_ar", nullable = false)
  private String nameAr;

  @Column(name = "name_en", nullable = false)
  private String nameEn;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "rank_order", nullable = false)
  private Integer rankOrder;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  /** Default {@code role.code} for routing stops at this org level (V26). */
  @Column(name = "default_role_code", length = 64)
  private String defaultRoleCode;
}
