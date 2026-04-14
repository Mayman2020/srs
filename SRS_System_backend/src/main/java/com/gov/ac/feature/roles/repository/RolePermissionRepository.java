package com.gov.ac.feature.roles.repository;

import com.gov.ac.feature.roles.entity.RolePermissionEntity;
import com.gov.ac.feature.roles.entity.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

  @Query("SELECT rp.id.permissionId FROM RolePermissionEntity rp WHERE rp.id.roleId = :roleId")
  List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM RolePermissionEntity rp WHERE rp.id.roleId = :roleId")
  void deleteByRoleId(@Param("roleId") Long roleId);
}
