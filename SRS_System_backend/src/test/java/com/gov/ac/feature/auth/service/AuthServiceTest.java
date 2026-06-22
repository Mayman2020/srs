package com.gov.ac.feature.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.auth.repository.MfaOtpChallengeRepository;
import com.gov.ac.feature.auth.repository.PasswordResetTokenRepository;
import com.gov.ac.feature.auth.repository.RefreshTokenRepository;
import com.gov.ac.feature.audit.repository.RoleSwitchAuditRepository;
import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.common.api.ForbiddenException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private AppUserRepository appUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private RoleSwitchAuditRepository roleSwitchAuditRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private MfaOtpChallengeRepository mfaOtpChallengeRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtIssuer jwtIssuer;
  @Mock private NotificationOutboxService notificationOutboxService;

  @InjectMocks private AuthService authService;

  private AppUserEntity user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "lockoutMaxFailedAttempts", 3);
    ReflectionTestUtils.setField(authService, "lockoutLockMinutes", 15);
    ReflectionTestUtils.setField(authService, "passwordResetFrontendBaseUrl", "http://localhost:4200");
    ReflectionTestUtils.setField(authService, "passwordResetTtlSeconds", 3600L);
    user = new AppUserEntity();
    user.setId(UUID.randomUUID());
    user.setUsername("alice");
    user.setActive(true);
    user.setPasswordHash("hash");
    user.setFailedLoginCount(0);
    user.setMfaEnabled(false);
  }

  @Test
  void login_locksAccountAfterRepeatedFailures() {
    when(appUserRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.login("alice", "bad"))
        .isInstanceOf(BadCredentialsException.class);
    assertThatThrownBy(() -> authService.login("alice", "bad"))
        .isInstanceOf(BadCredentialsException.class);
    assertThatThrownBy(() -> authService.login("alice", "bad"))
        .isInstanceOf(BadCredentialsException.class);

    assertThat(user.getFailedLoginCount()).isEqualTo(3);
    assertThat(user.getLockedUntil()).isNotNull();
  }

  @Test
  void login_rejectsLockedAccount() {
    user.setLockedUntil(Instant.now().plusSeconds(600));
    when(appUserRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login("alice", "secret"))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("ACCOUNT_LOCKED");
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void requestPasswordReset_enqueuesEmailWhenUserExists() {
    user.setEmail("alice@example.com");
    when(appUserRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(user));

    authService.requestPasswordReset("alice");

    verify(notificationOutboxService)
        .enqueueDirectEmail(any(), anyString(), anyString(), anyString(), anyString());
    verify(passwordResetTokenRepository).save(any());
  }
}
