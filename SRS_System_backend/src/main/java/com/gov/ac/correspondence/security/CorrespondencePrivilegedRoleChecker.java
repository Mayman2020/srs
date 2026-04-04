package com.gov.ac.correspondence.security;

import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.security.rbac.RbacRoleCodes;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorrespondencePrivilegedRoleChecker {

  private final RoleRepository roleRepository;

  public boolean hasPrivilegedViewRole(UUID userId) {
    List<String> codes = roleRepository.findActiveRoleCodesByUserId(userId);
    return codes.stream().anyMatch(RbacRoleCodes.CORRESPONDENCE_VIEW_ANY::contains);
  }
}
