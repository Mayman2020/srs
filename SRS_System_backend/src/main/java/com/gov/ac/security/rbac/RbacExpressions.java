package com.gov.ac.security.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("rbacExpressions")
public class RbacExpressions {

  public boolean canManageUsers(Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(RbacExpressions::stripRolePrefix)
        .anyMatch(RbacRoleCodes.USER_MANAGEMENT::contains);
  }

  private static String stripRolePrefix(String authority) {
    if (authority != null && authority.startsWith("ROLE_")) {
      return authority.substring("ROLE_".length());
    }
    return authority != null ? authority : "";
  }
}
