package com.gov.ac.feature.profile.capabilities.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.admin.entity.UiScreenEntity;
import com.gov.ac.feature.admin.repository.UiScreenRepository;
import com.gov.ac.feature.profile.capabilities.dto.ShellNavItemDto;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mockito-only test for {@link ShellNavigationService}: asserts {@code
 * GET /api/v1/profile/me/navigation} filters {@code ui_screen} rows by the same effective
 * permission union returned by {@code /me/capabilities}, and that {@code show_in_shell_nav} is
 * respected (the repository finder enforces it).
 */
@ExtendWith(MockitoExtension.class)
class ShellNavigationServiceTest {

  @Mock private UiScreenRepository uiScreenRepository;
  @Mock private EffectiveUserPermissionService effectiveUserPermissionService;

  @InjectMocks private ShellNavigationService service;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  void onlyReturnsScreensWhoseRequiredPermissionIsInTheUnion() {
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of(10L));
    when(uiScreenRepository.findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc())
        .thenReturn(
            List.of(
                screen("dashboard", "/dashboard", 10L),
                screen("admin_users", "/users", 20L)));

    List<ShellNavItemDto> items = service.navigationForUser(userId);

    assertThat(items).extracting(ShellNavItemDto::code).containsExactly("dashboard");
  }

  @Test
  void rowsWithoutRequiredPermissionAreAlwaysVisible() {
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of());
    when(uiScreenRepository.findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc())
        .thenReturn(List.of(screen("public", "/public", null)));

    List<ShellNavItemDto> items = service.navigationForUser(userId);

    assertThat(items).extracting(ShellNavItemDto::code).containsExactly("public");
  }

  @Test
  void inactiveScreensAreExcluded() {
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of(10L));
    UiScreenEntity active = screen("dashboard", "/dashboard", 10L);
    UiScreenEntity inactive = screen("legacy", "/legacy", 10L);
    inactive.setActive(false);
    when(uiScreenRepository.findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc())
        .thenReturn(List.of(active, inactive));

    List<ShellNavItemDto> items = service.navigationForUser(userId);

    assertThat(items).extracting(ShellNavItemDto::code).containsExactly("dashboard");
  }

  @Test
  void unionOfTwoRolesShowsBothLinks() {
    when(effectiveUserPermissionService.effectivePermissionIds(userId))
        .thenReturn(Set.of(10L, 20L));
    when(uiScreenRepository.findByDeletedAtIsNullAndShowInShellNavTrueOrderBySortOrderAsc())
        .thenReturn(
            List.of(
                screen("dashboard", "/dashboard", 10L),
                screen("admin_users", "/users", 20L)));

    List<ShellNavItemDto> items = service.navigationForUser(userId);

    assertThat(items)
        .extracting(ShellNavItemDto::code)
        .containsExactly("dashboard", "admin_users");
  }

  private static UiScreenEntity screen(String code, String routePath, Long permId) {
    UiScreenEntity s = new UiScreenEntity();
    s.setCode(code);
    s.setRoutePath(routePath);
    s.setNameAr(code);
    s.setNameEn(code);
    s.setSortOrder(1);
    s.setActive(true);
    s.setRequiredPermissionId(permId);
    s.setIconKey("apps");
    s.setShowInShellNav(true);
    return s;
  }
}
