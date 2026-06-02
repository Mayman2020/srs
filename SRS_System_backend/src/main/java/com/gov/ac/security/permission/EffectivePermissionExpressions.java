package com.gov.ac.security.permission;

import com.gov.ac.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SpEL entry point for {@code @PreAuthorize} using DB-resolved permissions (union of active roles),
 * not the JWT active role alone.
 *
 * <p>Two overloads are exposed:
 *
 * <ul>
 *   <li>{@link #has(String)} — single-arg form used by {@code @PreAuthorize} expressions like {@code
 *       @effectivePermission.has('CORRESPONDENCE_VIEW')}. Spring Security's SpEL does not
 *       auto-inject {@code authentication} when calling a method on a custom bean, so this overload
 *       reads from {@link SecurityContextHolder} directly.
 *   <li>{@link #has(Authentication, String)} — explicit form retained for tests and for callers
 *       that want to pass the {@code authentication} SpEL variable themselves.
 * </ul>
 */
@Component("effectivePermission")
@RequiredArgsConstructor
public class EffectivePermissionExpressions {

  private final EffectiveUserPermissionService effectiveUserPermissionService;

  /**
   * Single-arg variant used by {@code @PreAuthorize("@effectivePermission.has('PERM_CODE')")}.
   * Resolves the current {@link Authentication} from {@link SecurityContextHolder}; returns {@code
   * false} for anonymous / unauthenticated callers.
   *
   * @param permissionCode permission identifier (e.g. {@code CORRESPONDENCE_VIEW}, {@code
   *     ADMIN_USER_MANAGE})
   */
  public boolean has(String permissionCode) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return has(authentication, permissionCode);
  }

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
