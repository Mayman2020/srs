package com.gov.ac.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mockito-only contract test for {@link EffectiveUserPermissionService}.
 *
 * <p>The Postgres-level temporal/soft-delete filter contract (valid_from, valid_to, deleted_at,
 * is_active) is locked in by {@code EffectivePermissionUnionPostgresTest} which boots a real
 * database. Here we cover the service-level wiring: USER only, ADMIN only, USER+ADMIN union, and
 * alias-aware permission resolution that powers {@code @PreAuthorize}.
 */
@ExtendWith(MockitoExtension.class)
class EffectiveUserPermissionServiceTest {

  @Mock private UserRoleRepository userRoleRepository;
  @Mock private PermissionRepository permissionRepository;

  @InjectMocks private EffectiveUserPermissionService service;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  void userOnlyReturnsThatRolesPermissions() {
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(10L, 11L));

    Set<Long> result = service.effectivePermissionIds(userId);

    assertThat(result).containsExactlyInAnyOrder(10L, 11L);
  }

  @Test
  void adminOnlyReturnsThatRolesPermissions() {
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(20L, 21L, 22L));

    Set<Long> result = service.effectivePermissionIds(userId);

    assertThat(result).containsExactlyInAnyOrder(20L, 21L, 22L);
  }

  @Test
  void userPlusAdminReturnsUnionOfBothRoles() {
    // The single SQL union deduplicates at the DB layer with SELECT DISTINCT. Simulate the SQL
    // having already returned the union so the service is asserted to NOT add its own filtering
    // (any duplication or extra filtering here would be a regression).
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(10L, 11L, 20L, 21L, 22L));

    Set<Long> result = service.effectivePermissionIds(userId);

    assertThat(result).containsExactlyInAnyOrder(10L, 11L, 20L, 21L, 22L);
  }

  @Test
  void emptyAssignmentsReturnsEmptySet() {
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId)).thenReturn(List.of());

    assertThat(service.effectivePermissionIds(userId)).isEmpty();
  }

  @Test
  void hasActivePermissionResolvesByCanonicalCode() {
    PermissionEntity perm = activePermission(42L, "CORRESPONDENCE_VIEW");
    when(permissionRepository.findByCanonicalOrAliasCode("CORRESPONDENCE_VIEW"))
        .thenReturn(Optional.of(perm));
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(41L, 42L, 43L));

    assertThat(service.hasActivePermission(userId, "CORRESPONDENCE_VIEW")).isTrue();
  }

  @Test
  void hasActivePermissionResolvesByLegacyAlias() {
    // CANCEL_TRANSACTION is a legacy V30 alias mapped to canonical CORRESPONDENCE_DELETE in V7.
    PermissionEntity canonical = activePermission(99L, "CORRESPONDENCE_DELETE");
    when(permissionRepository.findByCanonicalOrAliasCode("CANCEL_TRANSACTION"))
        .thenReturn(Optional.of(canonical));
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(99L));

    assertThat(service.hasActivePermission(userId, "CANCEL_TRANSACTION")).isTrue();
  }

  @Test
  void hasActivePermissionFalseWhenPermissionInactive() {
    PermissionEntity perm = activePermission(42L, "CORRESPONDENCE_VIEW");
    perm.setActive(false);
    when(permissionRepository.findByCanonicalOrAliasCode("CORRESPONDENCE_VIEW"))
        .thenReturn(Optional.of(perm));

    assertThat(service.hasActivePermission(userId, "CORRESPONDENCE_VIEW")).isFalse();
  }

  @Test
  void hasActivePermissionFalseWhenIdNotInUnion() {
    PermissionEntity perm = activePermission(42L, "CORRESPONDENCE_VIEW");
    when(permissionRepository.findByCanonicalOrAliasCode("CORRESPONDENCE_VIEW"))
        .thenReturn(Optional.of(perm));
    when(userRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing(userId))
        .thenReturn(List.of(1L, 2L, 3L));

    assertThat(service.hasActivePermission(userId, "CORRESPONDENCE_VIEW")).isFalse();
  }

  @Test
  void hasActivePermissionFalseForBlankCode() {
    assertThat(service.hasActivePermission(userId, null)).isFalse();
    assertThat(service.hasActivePermission(userId, "")).isFalse();
    assertThat(service.hasActivePermission(userId, "   ")).isFalse();
  }

  @Test
  void temporalAndSoftDeleteFiltersAreEnforcedAtTheRepositoryLayer() {
    String nativeQuery = readNativeQuerySource("findEffectivePermissionIdsByUserId");
    assertThat(nativeQuery)
        .as("UserRoleRepository.findEffectivePermissionIdsByUserId must filter all predicates")
        .contains("ur.valid_from <= current_timestamp")
        .contains("ur.valid_to is null or ur.valid_to > current_timestamp")
        .contains("r.deleted_at is null")
        .contains("r.is_active = true")
        .contains("p.deleted_at is null")
        .contains("p.is_active = true");

    String unionQuery = readNativeQuerySource("findEffectivePermissionIdsByUserIdIncludingActing");
    assertThat(unionQuery)
        .contains("acting_assignment")
        .contains("aa.valid_from <= current_date")
        .contains("aa.valid_to >= current_date");
  }

  private static String readNativeQuerySource(String methodName) {
    try {
      java.lang.reflect.Method m =
          UserRoleRepository.class.getMethod(methodName, java.util.UUID.class);
      org.springframework.data.jpa.repository.Query annotation =
          m.getAnnotation(org.springframework.data.jpa.repository.Query.class);
      if (annotation == null) {
        throw new IllegalStateException(methodName + " is missing @Query");
      }
      return annotation.value();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(methodName + " must exist on UserRoleRepository", e);
    }
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
}
