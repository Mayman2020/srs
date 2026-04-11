package com.gov.ac.security.permission;

import com.gov.ac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * SpEL entry point for {@code @PreAuthorize} using DB-resolved permissions (union of active roles),
 * not the JWT active role alone.
 */
@Component("effectivePermission")
@RequiredArgsConstructor
public class EffectivePermissionExpressions {

  private final EffectiveUserPermissionService effectiveUserPermissionService;

  /**
   * @param permissionCode e.g. {@code user.manage}, {@code lookup.manage}
   */
  public boolean has(Authentication authentication, String permissionCode) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    return SecurityUtils.currentUserId()
        .map(uid -> effectiveUserPermissionService.hasActivePermission(uid, permissionCode))
        .orElse(false);
  }
}
