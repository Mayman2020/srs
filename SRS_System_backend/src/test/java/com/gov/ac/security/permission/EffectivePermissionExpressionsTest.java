package com.gov.ac.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Mockito-only test for {@link EffectivePermissionExpressions}, the SpEL bean exposed as
 * {@code @PreAuthorize("@effectivePermission.has(...)")}.
 *
 * <p>Critical contract: the SpEL bean must consult the DB-resolved union (via {@link
 * EffectiveUserPermissionService#hasActivePermission}) and produce the SAME answer regardless of
 * which JWT role is currently active. This pins the architecture rule that {@code activeRole} is
 * for workflow/audit context only and never narrows authorization.
 */
@ExtendWith(MockitoExtension.class)
class EffectivePermissionExpressionsTest {

  @Mock private EffectiveUserPermissionService effectiveUserPermissionService;

  @InjectMocks private EffectivePermissionExpressions expressions;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void unauthenticatedReturnsFalse() {
    Authentication anon = new TestingAuthenticationToken(userId, "creds", "ROLE_USER");
    anon.setAuthenticated(false);
    assertThat(expressions.has(anon, "CORRESPONDENCE_VIEW")).isFalse();
  }

  @Test
  void nullAuthenticationReturnsFalse() {
    assertThat(expressions.has(null, "CORRESPONDENCE_VIEW")).isFalse();
  }

  @Test
  void returnsTrueWhenUserHasPermission() {
    Authentication auth = authenticated("ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(effectiveUserPermissionService.hasActivePermission(eq(userId), eq("CORRESPONDENCE_VIEW")))
        .thenReturn(true);

    assertThat(expressions.has(auth, "CORRESPONDENCE_VIEW")).isTrue();
  }

  @Test
  void answerIsSameRegardlessOfJwtActiveRole() {
    // Same user, same DB state. The SpEL bean must return the same answer whether the JWT was
    // issued for ROLE_STAFF or ROLE_SYS_ADMIN; this proves activeRole is NOT consulted for
    // authorization.
    when(effectiveUserPermissionService.hasActivePermission(eq(userId), eq("ADMIN_USER_MANAGE")))
        .thenReturn(true);

    Authentication staff = authenticated("ROLE_STAFF");
    SecurityContextHolder.getContext().setAuthentication(staff);
    boolean asStaff = expressions.has(staff, "ADMIN_USER_MANAGE");

    Authentication admin = authenticated("ROLE_SYS_ADMIN");
    SecurityContextHolder.getContext().setAuthentication(admin);
    boolean asAdmin = expressions.has(admin, "ADMIN_USER_MANAGE");

    assertThat(asStaff).isEqualTo(asAdmin).isTrue();
  }

  @Test
  void returnsFalseWhenPrincipalIsNotUuid() {
    Authentication weird = new TestingAuthenticationToken("not-a-uuid", "creds", "ROLE_USER");
    weird.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(weird);

    assertThat(expressions.has(weird, "CORRESPONDENCE_VIEW")).isFalse();
  }

  // The single-arg overload (the one actually invoked by @PreAuthorize SpEL) must resolve the
  // current Authentication from SecurityContextHolder rather than failing with a method-not-found
  // error. These tests pin the contract used in production controller annotations like
  // @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')").

  @Test
  void singleArgUsesSecurityContextWhenAuthenticated() {
    Authentication auth = authenticated("ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(effectiveUserPermissionService.hasActivePermission(eq(userId), eq("CORRESPONDENCE_VIEW")))
        .thenReturn(true);

    assertThat(expressions.has("CORRESPONDENCE_VIEW")).isTrue();
  }

  @Test
  void singleArgReturnsFalseWhenNoAuthenticationInContext() {
    SecurityContextHolder.clearContext();
    assertThat(expressions.has("CORRESPONDENCE_VIEW")).isFalse();
  }

  private Authentication authenticated(String role) {
    TestingAuthenticationToken token = new TestingAuthenticationToken(userId, "creds", role);
    token.setAuthenticated(true);
    return token;
  }
}
