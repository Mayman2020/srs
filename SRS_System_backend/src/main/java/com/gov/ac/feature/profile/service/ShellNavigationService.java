package com.gov.ac.feature.profile.service;

import com.gov.ac.domain.admin.UiScreen;
import com.gov.ac.feature.profile.dto.ShellNavItemDto;
import com.gov.ac.persistence.UiScreenRepository;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShellNavigationService {

  private final UiScreenRepository uiScreenRepository;
  private final EffectiveUserPermissionService effectiveUserPermissionService;

  /**
   * Shell sidebar entries from {@code ui_screen} with {@code show_in_shell_nav}, filtered by the
   * user's effective permissions (union of all temporally valid role assignments).
   */
  @Transactional(readOnly = true)
  public List<ShellNavItemDto> navigationForUser(UUID userId) {
    Set<Long> allowedPermissionIds = effectiveUserPermissionService.effectivePermissionIds(userId);

    return uiScreenRepository.findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc().stream()
        .filter(UiScreen::getActive)
        .filter(
            s ->
                s.getRequiredPermissionId() == null
                    || allowedPermissionIds.contains(s.getRequiredPermissionId()))
        .map(ShellNavigationService::toNav)
        .toList();
  }

  private static ShellNavItemDto toNav(UiScreen s) {
    return new ShellNavItemDto(
        s.getCode(),
        s.getRoutePath(),
        s.getNameAr(),
        s.getNameEn(),
        s.getSortOrder(),
        s.getIconKey());
  }
}
