package com.gov.ac.feature.auth.service;

import com.gov.ac.domain.user.AppUser;
import com.gov.ac.feature.auth.dto.LoginResponse;
import com.gov.ac.persistence.AppUserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final long TOKEN_TTL_SECONDS = 8 * 3600L;

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtIssuer jwtIssuer;

  @Transactional(readOnly = true)
  public LoginResponse login(String username, String password) {
    AppUser user =
        appUserRepository
            .findByUsernameIgnoreCaseAndDeletedAtIsNull(username.trim())
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
    try {
      String token = jwtIssuer.issueAccessToken(user.getId(), TOKEN_TTL_SECONDS);
      return new LoginResponse(token, user.getId(), user.getUsername());
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not issue token", e);
    }
  }
}
