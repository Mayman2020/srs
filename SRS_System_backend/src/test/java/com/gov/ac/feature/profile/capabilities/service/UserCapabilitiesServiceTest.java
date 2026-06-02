package com.gov.ac.feature.profile.capabilities.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.admin.entity.UiScreenEntity;
import com.gov.ac.feature.admin.repository.UiScreenRepository;
import com.gov.ac.feature.profile.capabilities.dto.CapabilityScreenDto;
import com.gov.ac.feature.profile.capabilities.dto.UserCapabilitiesDto;
import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Mockito-only test for {@link UserCapabilitiesService}: asserts the DTO returned by {@code
 * GET /api/v1/me/capabilities} is the union of (active roles, active canonical permission codes,
 * permission-gated screens) and that the JWT current role does NOT narrow the result.
 */
@ExtendWith(MockitoExtension.class)
class UserCapabilitiesServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private EffectiveUserPermissionService effectiveUserPermissionService;
  @Mock private PermissionRepository permissionRepository;
  @Mock private UiScreenRepository uiScreenRepository;

  @InjectMocks private UserCapabilitiesService service;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    // Seed the SecurityContext with a UUID principal so SecurityUtils.requireCurrentUserId()
    // returns the expected user id without needing a static mock.
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(userId, "creds", "ROLE_USER"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void userOnlyReturnsThatRolesCapabilities() {
    when(roleRepository.findActiveRoleCodesByUserId(userId)).thenReturn(List.of("USER"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of(10L));
    stubPermission(10L, "DASHBOARD_VIEW");
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc())
        .thenReturn(List.of(screen("dashboard", "/dashboard", 10L, 1)));

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    assertThat(dto.roles()).containsExactly("USER");
    assertThat(dto.permissions()).containsExactly("DASHBOARD_VIEW");
    assertThat(dto.screens())
        .extracting(CapabilityScreenDto::code)
        .containsExactly("dashboard");
  }

  @Test
  void adminOnlyReturnsThatRolesCapabilities() {
    when(roleRepository.findActiveRoleCodesByUserId(userId)).thenReturn(List.of("SYS_ADMIN"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId))
        .thenReturn(Set.of(10L, 11L, 12L));
    stubPermission(10L, "DASHBOARD_VIEW");
    stubPermission(11L, "ADMIN_USER_MANAGE");
    stubPermission(12L, "ADMIN_ROLE_MANAGE");
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc())
        .thenReturn(
            List.of(
                screen("dashboard", "/dashboard", 10L, 1),
                screen("users", "/users", 11L, 2),
                screen("roles", "/roles", 12L, 3)));

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    assertThat(dto.permissions())
        .containsExactlyInAnyOrder("DASHBOARD_VIEW", "ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE");
    assertThat(dto.screens())
        .extracting(CapabilityScreenDto::code)
        .containsExactly("dashboard", "users", "roles");
  }

  @Test
  void userPlusAdminReturnsUnionOfBothRoles() {
    when(roleRepository.findActiveRoleCodesByUserId(userId))
        .thenReturn(List.of("SYS_ADMIN", "STAFF"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId))
        .thenReturn(Set.of(10L, 11L));
    stubPermission(10L, "DASHBOARD_VIEW");
    stubPermission(11L, "ADMIN_USER_MANAGE");
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc())
        .thenReturn(
            List.of(
                screen("dashboard", "/dashboard", 10L, 1),
                screen("users", "/users", 11L, 2)));

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    // Roles are sorted alphabetically.
    assertThat(dto.roles()).containsExactly("STAFF", "SYS_ADMIN");
    assertThat(dto.permissions())
        .containsExactlyInAnyOrder("DASHBOARD_VIEW", "ADMIN_USER_MANAGE");
  }

  @Test
  void activeRoleSwitchDoesNotReduceCapabilities() {
    // Same user, two different JWT current roles - capabilities must be identical because they
    // are computed from the DB role union, not from authority granted by the converter.
    when(roleRepository.findActiveRoleCodesByUserId(userId))
        .thenReturn(List.of("SYS_ADMIN", "STAFF"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId))
        .thenReturn(Set.of(10L, 11L));
    stubPermission(10L, "DASHBOARD_VIEW");
    stubPermission(11L, "ADMIN_USER_MANAGE");
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of());

    // Acting as STAFF in the JWT.
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(userId, "creds", "ROLE_STAFF"));
    UserCapabilitiesDto asStaff = service.loadForCurrentUser();

    // Switch to SYS_ADMIN in the JWT, same DB state.
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(userId, "creds", "ROLE_SYS_ADMIN"));
    UserCapabilitiesDto asAdmin = service.loadForCurrentUser();

    assertThat(asStaff.permissions())
        .as("capabilities must be the same regardless of JWT current role")
        .containsExactlyInAnyOrderElementsOf(asAdmin.permissions());
    assertThat(asStaff.roles()).containsExactlyElementsOf(asAdmin.roles());
  }

  @Test
  void inactivePermissionIsFilteredFromCodes() {
    when(roleRepository.findActiveRoleCodesByUserId(userId)).thenReturn(List.of("USER"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId))
        .thenReturn(Set.of(10L, 11L));
    stubPermission(10L, "DASHBOARD_VIEW");
    PermissionEntity inactive = activePermission(11L, "LEGACY_CODE");
    inactive.setActive(false);
    when(permissionRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(inactive));
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of());

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    assertThat(dto.permissions()).containsExactly("DASHBOARD_VIEW");
  }

  @Test
  void screensWithoutRequiredPermissionAreVisibleToEveryone() {
    when(roleRepository.findActiveRoleCodesByUserId(userId)).thenReturn(List.of("USER"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of());
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc())
        .thenReturn(List.of(screen("profile", "/profile", null, 1)));

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    assertThat(dto.screens())
        .extracting(CapabilityScreenDto::code)
        .containsExactly("profile");
  }

  @Test
  void inactiveScreensAreExcluded() {
    when(roleRepository.findActiveRoleCodesByUserId(userId)).thenReturn(List.of("USER"));
    when(effectiveUserPermissionService.effectivePermissionIds(userId)).thenReturn(Set.of(10L));
    UiScreenEntity active = screen("dashboard", "/dashboard", 10L, 1);
    UiScreenEntity inactive = screen("legacy", "/legacy", 10L, 2);
    inactive.setActive(false);
    when(uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc())
        .thenReturn(List.of(active, inactive));

    UserCapabilitiesDto dto = service.loadForCurrentUser();

    assertThat(dto.screens())
        .extracting(CapabilityScreenDto::code)
        .containsExactly("dashboard");
  }

  private void stubPermission(Long id, String code) {
    when(permissionRepository.findByIdAndDeletedAtIsNull(id))
        .thenReturn(Optional.of(activePermission(id, code)));
  }

  private static PermissionEntity activePermission(Long id, String code) {
    PermissionEntity p = new PermissionEntity();
    p.setId(id);
    p.setCode(code);
    p.setActive(true);
    p.setNameAr(code);
    p.setNameEn(code);
    return p;
  }

  private static UiScreenEntity screen(String code, String routePath, Long permId, int sortOrder) {
    UiScreenEntity s = new UiScreenEntity();
    s.setCode(code);
    s.setRoutePath(routePath);
    s.setNameAr(code);
    s.setNameEn(code);
    s.setSortOrder(sortOrder);
    s.setActive(true);
    s.setRequiredPermissionId(permId);
    s.setIconKey("apps");
    s.setShowInShellNav(true);
    return s;
  }
}
