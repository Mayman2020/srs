package com.gov.ac.security.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real-Postgres slice test for {@code UserRoleRepository#findEffectivePermissionIdsByUserId}: the
 * single SQL union must honor all four predicates simultaneously:
 *
 * <ul>
 *   <li>{@code user_role.valid_from / valid_to}: expired AND future-dated rows are ignored.
 *   <li>{@code role.deleted_at IS NULL AND role.is_active = true}.
 *   <li>{@code permission.deleted_at IS NULL AND permission.is_active = true}.
 * </ul>
 *
 * <p>This test boots a real Postgres 16 container, runs the full Flyway migration set (V1..V15)
 * for the canonical permission catalog, then seeds a synthetic user with multiple {@code user_role}
 * rows that exercise each predicate independently.
 *
 * <p>Disabled automatically when Docker is not available on the host so {@code mvn -B test} stays
 * green in environments without a Docker daemon (CI without Docker, dev machines without Docker
 * Desktop, etc.).
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(excludeAutoConfiguration = CamundaBpmAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(EffectivePermissionUnionPostgresTest.TestConfig.class)
@ActiveProfiles("test")
class EffectivePermissionUnionPostgresTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("srs_test")
          .withUsername("srs_test")
          .withPassword("srs_test")
          .withUrlParam("currentSchema", "srs_system");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    JdbcTemplate jdbcTemplate(javax.sql.DataSource ds) {
      return new JdbcTemplate(ds);
    }
  }

  @Autowired private UserRoleRepository userRoleRepository;
  @Autowired private JdbcTemplate jdbc;

  @Test
  @DisplayName("expired user_role rows are ignored")
  void expiredUserRoleIsIgnored() {
    UUID userId = newUser("expired-only");
    Long roleId = newRole("R_EXPIRED", true /* active */);
    Long permId = newPermission("PERM_EXPIRED", true /* active */);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAgo(30), daysAgo(1)); // valid_to is in the past

    List<Long> ids = userRoleRepository.findEffectivePermissionIdsByUserId(userId);

    assertThat(ids).as("expired user_role row must be filtered").isEmpty();
  }

  @Test
  @DisplayName("future-dated user_role rows are ignored")
  void futureDatedUserRoleIsIgnored() {
    UUID userId = newUser("future-only");
    Long roleId = newRole("R_FUTURE", true);
    Long permId = newPermission("PERM_FUTURE", true);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAhead(1), daysAhead(30)); // valid_from is in the future

    List<Long> ids = userRoleRepository.findEffectivePermissionIdsByUserId(userId);

    assertThat(ids).as("future-dated user_role row must be filtered").isEmpty();
  }

  @Test
  @DisplayName("soft-deleted role removes capability immediately")
  void softDeletedRoleRemovesCapability() {
    UUID userId = newUser("role-deleted");
    Long roleId = newRole("R_TO_DELETE", true);
    Long permId = newPermission("PERM_VIA_DELETED_ROLE", true);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAgo(1), null);

    // Before soft-delete: capability is present.
    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId)).contains(permId);

    // Soft-delete the role.
    jdbc.update("UPDATE srs_system.role SET deleted_at = now() WHERE id = ?", roleId);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId))
        .as("soft-deleted role must drop out of the effective union")
        .doesNotContain(permId);
  }

  @Test
  @DisplayName("deactivated role removes capability immediately")
  void deactivatedRoleRemovesCapability() {
    UUID userId = newUser("role-inactive");
    Long roleId = newRole("R_TO_DEACTIVATE", true);
    Long permId = newPermission("PERM_VIA_INACTIVE_ROLE", true);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAgo(1), null);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId)).contains(permId);

    jdbc.update("UPDATE srs_system.role SET is_active = FALSE WHERE id = ?", roleId);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId))
        .as("deactivated role must drop out of the effective union")
        .doesNotContain(permId);
  }

  @Test
  @DisplayName("soft-deleted permission removes capability immediately")
  void softDeletedPermissionRemovesCapability() {
    UUID userId = newUser("perm-deleted");
    Long roleId = newRole("R_KEEP_FOR_PERM_DELETE", true);
    Long permId = newPermission("PERM_TO_DELETE", true);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAgo(1), null);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId)).contains(permId);

    jdbc.update("UPDATE srs_system.permission SET deleted_at = now() WHERE id = ?", permId);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId))
        .as("soft-deleted permission must drop out of the effective union")
        .doesNotContain(permId);
  }

  @Test
  @DisplayName("deactivated permission removes capability immediately")
  void deactivatedPermissionRemovesCapability() {
    UUID userId = newUser("perm-inactive");
    Long roleId = newRole("R_KEEP_FOR_PERM_DEACTIVATE", true);
    Long permId = newPermission("PERM_TO_DEACTIVATE", true);
    grantRolePermission(roleId, permId);
    assignUserRole(userId, roleId, daysAgo(1), null);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId)).contains(permId);

    jdbc.update("UPDATE srs_system.permission SET is_active = FALSE WHERE id = ?", permId);

    assertThat(userRoleRepository.findEffectivePermissionIdsByUserId(userId))
        .as("deactivated permission must drop out of the effective union")
        .doesNotContain(permId);
  }

  @Test
  @DisplayName("union of two simultaneously valid roles returns both permission sets")
  void unionOfTwoValidRoles() {
    UUID userId = newUser("user-plus-admin");
    Long userRole = newRole("R_USER_T", true);
    Long adminRole = newRole("R_ADMIN_T", true);
    Long viewPerm = newPermission("PERM_T_VIEW", true);
    Long managePerm = newPermission("PERM_T_MANAGE", true);
    grantRolePermission(userRole, viewPerm);
    grantRolePermission(adminRole, managePerm);
    assignUserRole(userId, userRole, daysAgo(2), null);
    assignUserRole(userId, adminRole, daysAgo(1), null);

    List<Long> ids = userRoleRepository.findEffectivePermissionIdsByUserId(userId);

    assertThat(ids).contains(viewPerm, managePerm);
  }

  @Test
  @DisplayName("expired plus valid roles together: expired row contributes nothing")
  void mixedExpiredAndValid() {
    UUID userId = newUser("mixed");
    Long validRole = newRole("R_M_VALID", true);
    Long expiredRole = newRole("R_M_EXPIRED", true);
    Long validPerm = newPermission("PERM_M_VALID", true);
    Long expiredPerm = newPermission("PERM_M_EXPIRED", true);
    grantRolePermission(validRole, validPerm);
    grantRolePermission(expiredRole, expiredPerm);
    assignUserRole(userId, validRole, daysAgo(1), null);
    assignUserRole(userId, expiredRole, daysAgo(30), daysAgo(1));

    List<Long> ids = userRoleRepository.findEffectivePermissionIdsByUserId(userId);

    assertThat(ids).contains(validPerm).doesNotContain(expiredPerm);
  }

  // --- helpers ---------------------------------------------------------------

  private UUID newUser(String suffix) {
    UUID id = UUID.randomUUID();
    Long deptId =
        jdbc.queryForObject(
            "SELECT id FROM srs_system.department WHERE deleted_at IS NULL ORDER BY id LIMIT 1",
            Long.class);
    String username = "rbac-test-" + suffix + "-" + id.toString().substring(0, 8);
    jdbc.update(
        "INSERT INTO srs_system.app_user "
            + "(id, username, password_hash, full_name_ar, full_name_en, email, "
            + " department_id, is_active, created_at, updated_at) "
            + "VALUES (?, CAST(? AS public.citext), ?, ?, ?, CAST(? AS public.citext), "
            + " ?, TRUE, now(), now())",
        id,
        username,
        "$2a$10$DUMMY_HASH_FOR_TEST_ONLY_NEVER_USED",
        "RBAC " + suffix,
        "RBAC " + suffix,
        username + "@test.invalid",
        deptId);
    return id;
  }

  private Long newRole(String code, boolean active) {
    String uniqueCode = code + "_" + UUID.randomUUID().toString().substring(0, 8);
    return jdbc.queryForObject(
        "INSERT INTO srs_system.role (code, name_ar, name_en, sort_order, is_active, "
            + " created_at, updated_at) "
            + "VALUES (?, ?, ?, 9999, ?, now(), now()) RETURNING id",
        Long.class,
        uniqueCode,
        uniqueCode,
        uniqueCode,
        active);
  }

  private Long newPermission(String code, boolean active) {
    String uniqueCode = code + "_" + UUID.randomUUID().toString().substring(0, 8);
    return jdbc.queryForObject(
        "INSERT INTO srs_system.permission (code, name_ar, name_en, sort_order, is_active, "
            + " created_at, updated_at) "
            + "VALUES (?, ?, ?, 9999, ?, now(), now()) RETURNING id",
        Long.class,
        uniqueCode,
        uniqueCode,
        uniqueCode,
        active);
  }

  private void grantRolePermission(Long roleId, Long permId) {
    jdbc.update(
        "INSERT INTO srs_system.role_permission (role_id, permission_id) VALUES (?, ?)",
        roleId,
        permId);
  }

  private void assignUserRole(
      UUID userId, Long roleId, OffsetDateTime validFrom, OffsetDateTime validTo) {
    jdbc.update(
        "INSERT INTO srs_system.user_role (app_user_id, role_id, valid_from, valid_to, "
            + " created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, now(), now())",
        userId,
        roleId,
        validFrom,
        validTo);
  }

  private static OffsetDateTime daysAgo(int days) {
    return OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
  }

  private static OffsetDateTime daysAhead(int days) {
    return OffsetDateTime.now(ZoneOffset.UTC).plusDays(days);
  }
}
