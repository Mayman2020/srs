package com.gov.ac.feature.users.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user", schema = "srs_system")
@Getter
@Setter
public class AppUserEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** PostgreSQL {@code citext}. Do not use {@code Types#OTHER} here: Hibernate 6 maps it to varbinary for bind/read. */
  @Column(nullable = false, columnDefinition = "citext")
  private String username;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "full_name_ar", nullable = false)
  private String fullNameAr;

  @Column(name = "full_name_en", nullable = false)
  private String fullNameEn;

  @Column(nullable = false, columnDefinition = "citext")
  private String email;

  private String phone;

  @Column(name = "national_id")
  private String nationalId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private DepartmentEntity department;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "failed_login_count", nullable = false)
  private Integer failedLoginCount = 0;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt;

  @Column(name = "mfa_enabled", nullable = false)
  private Boolean mfaEnabled = false;

  /** {@code light} or {@code dark} — persisted for UI theme. */
  @Column(name = "profile_image_path", length = 512)
  private String profileImagePath;

  @Column(name = "profile_image_content_type", length = 128)
  private String profileImageContentType;

  @Column(name = "ui_theme", nullable = false, length = 16)
  private String uiTheme = "light";

  /** {@code ar} or {@code en} — persisted UI language. */
  @Column(name = "ui_locale", nullable = false, length = 8)
  private String uiLocale = "ar";

  /**
   * Maximum confidentiality level this user is cleared to view (FK to {@code confidentiality.id}).
   * Null is treated by {@code CorrespondenceViewAuthorization} as the lowest clearance.
   */
  @Column(name = "security_clearance_id")
  private Long securityClearanceId;
}
