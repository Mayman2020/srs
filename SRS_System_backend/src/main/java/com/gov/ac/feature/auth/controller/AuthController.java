package com.gov.ac.feature.auth.controller;

import com.gov.ac.feature.auth.dto.LoginRequestDto;
import com.gov.ac.feature.auth.dto.LoginResponseDto;
import com.gov.ac.feature.auth.dto.MfaChallengeRequestDto;
import com.gov.ac.feature.auth.dto.MfaVerifyRequestDto;
import com.gov.ac.feature.auth.dto.RefreshRequestDto;
import com.gov.ac.feature.auth.dto.SwitchRoleRequestDto;
import com.gov.ac.feature.auth.service.AuthService;
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
  public LoginResponseDto login(@Valid @RequestBody LoginRequestDto request) {
    return authService.login(request.username(), request.password());
  }

  @PostMapping("/refresh")
  public LoginResponseDto refresh(@Valid @RequestBody RefreshRequestDto request) {
    return authService.refresh(request.refreshToken());
  }

  /** Switch {@code active_role} claim; returns a fresh JWT (no logout). */
  @PostMapping("/switch-role")
  public LoginResponseDto switchRole(
      @Valid @RequestBody SwitchRoleRequestDto request, Authentication authentication) {
    UUID userId = (UUID) authentication.getPrincipal();
    return authService.switchRole(userId, authentication, request.roleCode());
  }

  @PostMapping("/mfa/challenge")
  public void mfaChallenge(@Valid @RequestBody MfaChallengeRequestDto request) {
    authService.requestMfaChallenge(request.username(), request.channel());
  }

  @PostMapping("/mfa/verify")
  public LoginResponseDto mfaVerify(@Valid @RequestBody MfaVerifyRequestDto request) {
    return authService.verifyMfaAndLogin(request.username(), request.password(), request.code());
  }
}
