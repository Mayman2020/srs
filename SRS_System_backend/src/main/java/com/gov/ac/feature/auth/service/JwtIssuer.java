package com.gov.ac.feature.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtIssuer {

  private final byte[] secretBytes;

  public JwtIssuer(@Value("${ac.security.jwt.secret}") String rawSecret) {
    this.secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException(
          "ac.security.jwt.secret must be at least 32 UTF-8 bytes (use AC_JWT_SECRET).");
    }
  }

  /**
   * HS256 access token. Claims: {@code sub} / {@code userId}, {@code username}, {@code roles},
   * {@code currentRole}. {@code active_role} mirrors {@code currentRole} for backward compatibility.
   */
  public String issueAccessToken(
      UUID userId,
      String username,
      List<String> roleCodes,
      String activeRoleCode,
      long ttlSeconds)
      throws JOSEException {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .claim("userId", userId.toString())
            .claim("username", username)
            .claim("roles", roleCodes)
            .claim("currentRole", activeRoleCode)
            .claim("active_role", activeRoleCode)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(secretBytes));
    return jwt.serialize();
  }
}
