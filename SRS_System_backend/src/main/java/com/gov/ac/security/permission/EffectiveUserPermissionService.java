package com.gov.ac.security.permission;

import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the effective permission set for a user as the <strong>union</strong> of all permissions
 * granted by their temporally valid {@code user_role} rows (same model as {@code /me/capabilities}).
 *
 * <p>The union is computed by a single SQL join in {@link
 * UserRoleRepository#findEffectivePermissionIdsByUserId(UUID)} that filters by:
 *
 * <ul>
 *   <li>{@code user_role.valid_from / valid_to} (expired and future-dated rows are ignored).
 *   <li>{@code role.deleted_at IS NULL} and {@code role.is_active = true}.
 *   <li>{@code permission.deleted_at IS NULL} and {@code permission.is_active = true}.
 * </ul>
 *
 * <p>The JWT {@code currentRole} / {@code active_role} claim is intentionally not read here:
 * capability resolution must always reflect the union of every currently valid role assignment,
 * regardless of which role is selected for workflow/audit context in the JWT.
 */
@Service
@RequiredArgsConstructor
public class EffectiveUserPermissionService {

  private final UserRoleRepository userRoleRepository;
  private final PermissionRepository permissionRepository;

  /**
   * Permission id union across all active, non-deleted role assignments for the user, computed by a
   * single filtered SQL join.
   */
  @Transactional(readOnly = true)
  public Set<Long> effectivePermissionIds(UUID userId) {
    return new HashSet<>(
        userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId));
  }

  /**
   * True when the permission code exists, is active, and its id is included in the user's effective
   * permission id set (union of roles).
   *
   * <p>Resolves through {@code permission_alias} so legacy codes ({@code correspondence.view},
   * {@code VIEW_TRANSACTIONS}) and the canonical SCREAMING_SNAKE codes ({@code
   * CORRESPONDENCE_VIEW}) both map to the same permission row.
   */
  @Transactional(readOnly = true)
  public boolean hasActivePermission(UUID userId, String permissionCode) {
    if (permissionCode == null || permissionCode.isBlank()) {
      return false;
    }
    return permissionRepository
        .findByCanonicalOrAliasCode(permissionCode.trim())
        .filter(p -> Boolean.TRUE.equals(p.getActive()))
        .map(PermissionEntity::getId)
        .map(id -> effectivePermissionIds(userId).contains(id))
        .orElse(false);
  }
}
