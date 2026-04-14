package com.gov.ac.feature.auth.service;

import com.gov.ac.feature.audit.entity.RoleSwitchAuditEntity;
import com.gov.ac.feature.auth.entity.MfaOtpChallengeEntity;
import com.gov.ac.feature.auth.entity.RefreshTokenEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.auth.dto.LoginResponseDto;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.auth.repository.MfaOtpChallengeRepository;
import com.gov.ac.feature.auth.repository.RefreshTokenRepository;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.audit.repository.RoleSwitchAuditRepository;
import com.gov.ac.common.api.ForbiddenException;
import com.nimbusds.jose.JOSEException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final long ACCESS_TTL_SECONDS = 8 * 3600L;

  private final AppUserRepository appUserRepository;
  private final RoleRepository roleRepository;
  private final RoleSwitchAuditRepository roleSwitchAuditRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final MfaOtpChallengeRepository mfaOtpChallengeRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtIssuer jwtIssuer;

  @Value("${ac.auth.refresh-ttl-seconds:604800}")
  private long refreshTtlSeconds;

  @Value("${ac.auth.mfa-otp-ttl-seconds:300}")
  private long mfaOtpTtlSeconds;

  @Transactional
  public LoginResponseDto login(String username, String password) {
    AppUserEntity user = loadUserForPasswordCheck(username, password);
    if (Boolean.TRUE.equals(user.getMfaEnabled())) {
      throw new ForbiddenException("MFA_REQUIRED");
    }
    return issueSession(user, true);
  }

  @Transactional
  public LoginResponseDto refresh(String refreshTokenJti) {
    UUID jti;
    try {
      jti = UUID.fromString(refreshTokenJti.trim());
    } catch (Exception e) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    RefreshTokenEntity rt =
        refreshTokenRepository
            .findByJti(jti)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (rt.getExpiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("Refresh expired");
    }
    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(rt.getUser().getId())
            .orElseThrow(() -> new BadCredentialsException("User not found"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    refreshTokenRepository.delete(rt);
    return issueSession(user, true);
  }

  @Transactional
  public LoginResponseDto switchRole(UUID userId, Authentication authentication, String roleCode) {
    String normalized = roleCode == null ? "" : roleCode.trim();
    if (normalized.isEmpty()) {
      throw new BadCredentialsException("roleCode is required");
    }
    List<String> dbRoles = roleRepository.findActiveRoleCodesByUserId(userId);
    if (!dbRoles.contains(normalized)) {
      throw new AccessDeniedException("RoleEntity is not assigned to this user");
    }
    String oldRole =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring("ROLE_".length()))
            .findFirst()
            .orElse(null);

    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

    roleSwitchAuditRepository.save(
        new RoleSwitchAuditEntity(null, userId, oldRole, normalized, Instant.now()));

    List<String> sorted = new ArrayList<>(dbRoles);
    Collections.sort(sorted);
    try {
      String token =
          jwtIssuer.issueAccessToken(
              user.getId(), user.getUsername(), sorted, normalized, ACCESS_TTL_SECONDS);
      return new LoginResponseDto(
          token,
          null,
          ACCESS_TTL_SECONDS,
          userId,
          user.getUsername(),
          sorted,
          normalized,
          profileImageUrl(user));
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not issue token", e);
    }
  }

  /** Stub delivery: OTP is logged; wire email/SMS providers behind {@link com.gov.ac.feature.notification}. */
  @Transactional
  public void requestMfaChallenge(String username, String channel) {
    AppUserEntity user =
        appUserRepository
            .findByUsernameAndDeletedAtIsNull(username.trim())
            .orElseThrow(() -> new BadCredentialsException("Unknown user"));
    if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
      throw new BadCredentialsException("MFA not enabled for user");
    }
    String code = String.format("%06d", (int) (Math.random() * 1_000_000));
    MfaOtpChallengeEntity ch = new MfaOtpChallengeEntity();
    ch.setUser(user);
    ch.setChannel(channel.trim().toUpperCase());
    ch.setCodeHash(sha256Hex(code));
    ch.setExpiresAt(Instant.now().plusSeconds(mfaOtpTtlSeconds));
    ch.setConsumed(false);
    mfaOtpChallengeRepository.save(ch);
    log.warn("MFA OTP (dev) user={} channel={} code={}", user.getUsername(), channel, code);
  }

  @Transactional
  public LoginResponseDto verifyMfaAndLogin(String username, String password, String code) {
    AppUserEntity user = loadUserForPasswordCheck(username, password);
    if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
      throw new BadCredentialsException("MFA not enabled for user");
    }
    String hash = sha256Hex(code.trim());
    Instant now = Instant.now();
    List<MfaOtpChallengeEntity> matches =
        mfaOtpChallengeRepository.findAll().stream()
            .filter(c -> c.getUser().getId().equals(user.getId()))
            .filter(c -> !c.isConsumed())
            .filter(c -> c.getExpiresAt().isAfter(now))
            .filter(c -> c.getCodeHash().equals(hash))
            .toList();
    if (matches.isEmpty()) {
      throw new BadCredentialsException("Invalid or expired OTP");
    }
    matches.forEach(
        c -> {
          c.setConsumed(true);
          mfaOtpChallengeRepository.save(c);
        });
    return issueSession(user, true);
  }

  private AppUserEntity loadUserForPasswordCheck(String username, String password) {
    AppUserEntity user =
        appUserRepository
            .findByUsernameAndDeletedAtIsNull(username.trim())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    String hash = user.getPasswordHash();
    if (hash == null || hash.isBlank()) {
      throw new BadCredentialsException("Invalid credentials");
    }
    if (!passwordEncoder.matches(password, hash)) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return user;
  }

  private LoginResponseDto issueSession(AppUserEntity user, boolean includeRefresh) {
    List<String> roles = roleRepository.findActiveRoleCodesByUserId(user.getId());
    if (roles.isEmpty()) {
      throw new BadCredentialsException("Invalid credentials");
    }
    List<String> sorted = new ArrayList<>(roles);
    Collections.sort(sorted);
    String current = sorted.contains("SYS_ADMIN") ? "SYS_ADMIN" : sorted.get(0);
    try {
      String access =
          jwtIssuer.issueAccessToken(
              user.getId(), user.getUsername(), sorted, current, ACCESS_TTL_SECONDS);
      String refreshStr = null;
      if (includeRefresh) {
        refreshTokenRepository.deleteByUser_Id(user.getId());
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setUser(user);
        rt.setJti(UUID.randomUUID());
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        refreshTokenRepository.save(rt);
        refreshStr = rt.getJti().toString();
      }
      return new LoginResponseDto(
          access,
          refreshStr,
          ACCESS_TTL_SECONDS,
          user.getId(),
          user.getUsername(),
          sorted,
          current,
          profileImageUrl(user));
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not issue token", e);
    }
  }

  private static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String profileImageUrl(AppUserEntity user) {
    if (user.getProfileImagePath() == null || user.getProfileImagePath().isBlank()) {
      return null;
    }
    long version = user.getUpdatedAt() != null ? user.getUpdatedAt().toEpochMilli() : System.currentTimeMillis();
    return "/api/v1/profile/me/avatar?v=" + version;
  }
}
