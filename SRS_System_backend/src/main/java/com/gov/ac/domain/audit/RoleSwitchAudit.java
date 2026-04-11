package com.gov.ac.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_switch_audit", schema = "srs_system")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleSwitchAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private UUID appUserId;

  @Column(name = "old_role_code", length = 100)
  private String oldRoleCode;

  @Column(name = "new_role_code", nullable = false, length = 100)
  private String newRoleCode;

  @Column(name = "switched_at", nullable = false)
  private Instant switchedAt;
}
