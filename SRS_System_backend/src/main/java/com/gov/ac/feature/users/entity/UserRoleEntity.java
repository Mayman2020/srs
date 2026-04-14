package com.gov.ac.feature.users.entity;

import com.gov.ac.feature.shared.entity.AuditableEntity;
import com.gov.ac.feature.roles.entity.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_role", schema = "srs_system")
@Getter
@Setter
public class UserRoleEntity extends AuditableEntity {

  @EmbeddedId private UserRoleId id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("appUserId")
  @JoinColumn(name = "app_user_id", nullable = false)
  private AppUserEntity appUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("roleId")
  @JoinColumn(name = "role_id", nullable = false)
  private RoleEntity role;

  @Column(name = "valid_from", nullable = false)
  private Instant validFrom;

  @Column(name = "valid_to")
  private Instant validTo;
}
