package com.gov.ac.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static UUID requireCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
      throw new BadCredentialsException("Authentication required");
    }
    return id;
  }

  /** Current app_user id when the request is authenticated; empty for jobs or anonymous. */
  public static Optional<UUID> currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
      return Optional.empty();
    }
    return Optional.of(id);
  }

  /**
   * Active SRS role code from JWT (e.g. {@code STAFF}), matching {@code ROLE_*} granted authority.
   */
  public static String requireCurrentRoleCode() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new BadCredentialsException("Authentication required");
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a != null && a.startsWith("ROLE_"))
        .map(a -> a.substring("ROLE_".length()))
        .findFirst()
        .orElseThrow(() -> new BadCredentialsException("No active role in session"));
  }
}
