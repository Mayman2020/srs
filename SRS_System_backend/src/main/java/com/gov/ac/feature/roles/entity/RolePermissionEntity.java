package com.gov.ac.feature.roles.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_permission", schema = "srs_system")
@Getter
@Setter
public class RolePermissionEntity {

  @EmbeddedId private RolePermissionId id;
}
