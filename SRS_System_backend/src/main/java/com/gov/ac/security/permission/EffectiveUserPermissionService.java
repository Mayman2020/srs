package com.gov.ac.security.permission;

import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.roles.repository.RolePermissionRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the effective permission set for a user as the <strong>union</strong> of all permissions
 * granted by their temporally valid {@code user_role} rows (same model as {@code /me/capabilities}).
 */
@Service
@RequiredArgsConstructor
public class EffectiveUserPermissionService {

  private final UserRoleRepository userRoleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PermissionRepository permissionRepository;

  /** PermissionEntity id union across all active role assignments for the user. */
  @Transactional(readOnly = true)
  public Set<Long> effectivePermissionIds(UUID userId) {
    List<Long> roleIds = userRoleRepository.findActiveRoleIdsByUserId(userId);
    Set<Long> permissionIds = new HashSet<>();
    for (Long roleId : roleIds) {
      permissionIds.addAll(rolePermissionRepository.findPermissionIdsByRoleId(roleId));
    }
    return permissionIds;
  }

  /**
   * True when the permission code exists, is active, and its id is included in the user's effective
   * permission id set (union of roles).
   */
  @Transactional(readOnly = true)
  public boolean hasActivePermission(UUID userId, String permissionCode) {
    return permissionRepository
        .findByCodeIgnoreCaseAndDeletedAtIsNull(permissionCode.trim())
        .filter(p -> Boolean.TRUE.equals(p.getActive()))
        .map(PermissionEntity::getId)
        .map(id -> effectivePermissionIds(userId).contains(id))
        .orElse(false);
  }
}
