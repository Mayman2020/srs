package com.gov.ac.modules.auth.controller;

import com.gov.ac.modules.auth.dto.LoginRequest;
import com.gov.ac.modules.auth.dto.LoginResponse;
import com.gov.ac.modules.auth.dto.MfaChallengeRequest;
import com.gov.ac.modules.auth.dto.MfaVerifyRequest;
import com.gov.ac.modules.auth.dto.RefreshRequest;
import com.gov.ac.modules.auth.dto.SwitchRoleRequest;
import com.gov.ac.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.username(), request.password());
  }

  @PostMapping("/refresh")
  public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request.refreshToken());
  }

  /** Switch {@code active_role} claim; returns a fresh JWT (no logout). */
  @PostMapping("/switch-role")
  public LoginResponse switchRole(
      @Valid @RequestBody SwitchRoleRequest request, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return authService.switchRole(userId, authentication, request.roleCode());
  }

  @PostMapping("/mfa/challenge")
  public void mfaChallenge(@Valid @RequestBody MfaChallengeRequest request) {
    authService.requestMfaChallenge(request.username(), request.channel());
  }

  @PostMapping("/mfa/verify")
  public LoginResponse mfaVerify(@Valid @RequestBody MfaVerifyRequest request) {
    return authService.verifyMfaAndLogin(request.username(), request.password(), request.code());
  }
}
