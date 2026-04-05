package com.gov.ac.domain.user;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.org.Department;
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
@Table(name = "app_user")
@Getter
@Setter
public class AppUser extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

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
  private Department department;

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
}
