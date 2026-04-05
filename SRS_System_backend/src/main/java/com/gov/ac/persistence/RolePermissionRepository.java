package com.gov.ac.persistence;

import com.gov.ac.domain.user.RolePermission;
import com.gov.ac.domain.user.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

  @Query("SELECT rp.id.permissionId FROM RolePermission rp WHERE rp.id.roleId = :roleId")
  List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM RolePermission rp WHERE rp.id.roleId = :roleId")
  void deleteByRoleId(@Param("roleId") Long roleId);
}
