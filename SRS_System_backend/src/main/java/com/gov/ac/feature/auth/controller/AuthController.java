package com.gov.ac.feature.auth.controller;

import com.gov.ac.feature.auth.dto.ForgotPasswordRequestDto;
import com.gov.ac.feature.auth.dto.LoginRequestDto;
import com.gov.ac.feature.auth.dto.LoginResponseDto;
import com.gov.ac.feature.auth.dto.MfaChallengeRequestDto;
import com.gov.ac.feature.auth.dto.MfaVerifyRequestDto;
import com.gov.ac.feature.auth.dto.RefreshRequestDto;
import com.gov.ac.feature.auth.dto.ResetPasswordRequestDto;
import com.gov.ac.feature.auth.dto.SwitchRoleRequestDto;
import com.gov.ac.feature.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication & session (JWT + refresh + MFA). Frontend: {@code features/auth/login}. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @PreAuthorize("permitAll()")
  public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
    return authService.login(request.username(), request.password());
  }

  @PostMapping("/refresh")
  @PreAuthorize("permitAll()")
  public LoginResponseDto refresh(@Valid @RequestBody RefreshRequestDto request) {
    return authService.refresh(request.refreshToken());
  }

  /** Switch {@code active_role} claim; returns a fresh JWT (no logout). */
  @PostMapping("/switch-role")
  @PreAuthorize("isAuthenticated()")
  public LoginResponseDto switchRole(
      @Valid @RequestBody SwitchRoleRequestDto request, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return authService.switchRole(userId, authentication, request.roleCode());
  }

  @PostMapping("/mfa/challenge")
  @PreAuthorize("permitAll()")
  public void mfaChallenge(@Valid @RequestBody MfaChallengeRequestDto request) {
    authService.requestMfaChallenge(request.username(), request.channel());
  }

  @PostMapping("/mfa/verify")
  @PreAuthorize("permitAll()")
  public LoginResponseDto mfaVerify(@Valid @RequestBody MfaVerifyRequestDto request) {
    return authService.verifyMfaAndLogin(request.username(), request.password(), request.code());
  }

  @PostMapping("/forgot-password")
  @PreAuthorize("permitAll()")
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
    authService.requestPasswordReset(request.username());
  }

  @PostMapping("/reset-password")
  @PreAuthorize("permitAll()")
  public void resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
    authService.resetPassword(request.token(), request.newPassword());
  }
}
