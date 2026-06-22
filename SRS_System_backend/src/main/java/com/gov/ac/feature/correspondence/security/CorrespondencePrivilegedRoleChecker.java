package com.gov.ac.feature.correspondence.security;

import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorrespondencePrivilegedRoleChecker {

  private final EffectiveUserPermissionService effectiveUserPermissionService;

  public boolean hasPrivilegedViewRole(UUID userId) {
    return effectiveUserPermissionService.hasActivePermission(userId, "CORRESPONDENCE_VIEW_ANY");
  }
}
