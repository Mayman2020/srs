package com.gov.ac.security.rbac;

import java.util.Set;

/**
 * Stable {@code role.code} values from the {@code role} lookup table, used for authorization.
 */
public final class RbacRoleCodes {

  private RbacRoleCodes() {}

  /** Roles that may list users, list roles, and assign roles (administration). */
  public static final Set<String> USER_MANAGEMENT = Set.of("SYS_ADMIN");

  /**
   * Roles that may view any correspondence without department or workflow checks (see
   * correspondence view rules).
   */
  public static final Set<String> CORRESPONDENCE_VIEW_ANY =
      Set.of("SYS_ADMIN", "ADMIN", "AUDITOR");
}
