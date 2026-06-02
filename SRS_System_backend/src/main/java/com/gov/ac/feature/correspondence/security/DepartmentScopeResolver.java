package com.gov.ac.feature.correspondence.security;

import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the department scope to apply when aggregating correspondence numbers for the current
 * user. Privileged callers (system / archive admins, ministry leadership) get the full picture;
 * everyone else is bound to their own department.
 */
@Component
@RequiredArgsConstructor
public class DepartmentScopeResolver {

  private final AppUserRepository appUserRepository;
  private final CorrespondencePrivilegedRoleChecker privilegedRoleChecker;

  /**
   * @return the department id the caller is restricted to, or {@code null} when the caller has a
   *     privileged view role (no scope filter — global numbers).
   */
  public Long resolveDepartmentScope(UUID userId) {
    if (privilegedRoleChecker.hasPrivilegedViewRole(userId)) {
      return null;
    }
    AppUserEntity user = appUserRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
    if (user == null || user.getDepartment() == null) {
      return null;
    }
    return user.getDepartment().getId();
  }
}
