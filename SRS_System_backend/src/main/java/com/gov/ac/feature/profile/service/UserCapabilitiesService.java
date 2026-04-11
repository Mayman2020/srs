package com.gov.ac.feature.profile.service;

import com.gov.ac.domain.admin.UiScreen;
import com.gov.ac.domain.user.Permission;
import com.gov.ac.feature.profile.dto.CapabilityScreenDto;
import com.gov.ac.feature.profile.dto.UserCapabilitiesDto;
import com.gov.ac.persistence.PermissionRepository;
import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.persistence.UiScreenRepository;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCapabilitiesService {

  private final RoleRepository roleRepository;
  private final EffectiveUserPermissionService effectiveUserPermissionService;
  private final PermissionRepository permissionRepository;
  private final UiScreenRepository uiScreenRepository;

  @Transactional(readOnly = true)
  public UserCapabilitiesDto loadForCurrentUser() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    List<String> roleCodes = new ArrayList<>(roleRepository.findActiveRoleCodesByUserId(userId));
    roleCodes.sort(String::compareToIgnoreCase);
    Set<Long> permissionIds = effectiveUserPermissionService.effectivePermissionIds(userId);
    List<String> permissionCodes = new ArrayList<>();
    for (Long pid : permissionIds) {
      permissionRepository
          .findByIdAndDeletedAtIsNull(pid)
          .filter(p -> Boolean.TRUE.equals(p.getActive()))
          .map(Permission::getCode)
          .ifPresent(permissionCodes::add);
    }
    permissionCodes.sort(String::compareToIgnoreCase);

    Set<Long> allowed = new HashSet<>(permissionIds);
    List<CapabilityScreenDto> screens =
        uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActive()))
            .filter(
                s ->
                    s.getRequiredPermissionId() == null
                        || allowed.contains(s.getRequiredPermissionId()))
            .sorted(Comparator.comparing(UiScreen::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(s -> new CapabilityScreenDto(s.getCode(), s.getRoutePath()))
            .toList();

    return new UserCapabilitiesDto(List.copyOf(roleCodes), permissionCodes, screens);
  }
}
