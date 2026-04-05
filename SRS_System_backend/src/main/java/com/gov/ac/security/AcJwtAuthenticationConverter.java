package com.gov.ac.security;

import com.gov.ac.persistence.RoleRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Principal is {@link UUID} app_user id (JWT {@code sub}). Exactly one authority {@code
 * ROLE_&lt;currentRole&gt;} is granted, validated against live {@code user_role} assignments.
 *
 * <p>Tokens without {@code currentRole}/{@code active_role} (legacy) resolve to a single role: prefer {@code SYS_ADMIN}
 * if assigned, else lexicographically first active role — still one authority.
 */
@Component
@RequiredArgsConstructor
public class AcJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final RoleRepository roleRepository;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String sub = jwt.getSubject();
    if (sub == null || sub.isBlank()) {
      throw new BadCredentialsException("JWT must contain sub (user id)");
    }
    final UUID userId;
    try {
      userId = UUID.fromString(sub.trim());
    } catch (IllegalArgumentException ex) {
      throw new BadCredentialsException("JWT sub must be a UUID");
    }
    List<String> dbRoles = roleRepository.findActiveRoleCodesByUserId(userId);
    if (dbRoles.isEmpty()) {
      throw new BadCredentialsException("User has no active roles");
    }
    List<String> sorted = new ArrayList<>(dbRoles);
    Collections.sort(sorted);

    String active = jwt.getClaimAsString("currentRole");
    if (active == null || active.isBlank()) {
      active = jwt.getClaimAsString("active_role");
    }
    if (active == null || active.isBlank()) {
      active = sorted.contains("SYS_ADMIN") ? "SYS_ADMIN" : sorted.get(0);
    }
    if (!dbRoles.contains(active)) {
      throw new BadCredentialsException("Active role is not assigned to this user");
    }
    return new UsernamePasswordAuthenticationToken(
        userId,
        jwt.getTokenValue(),
        List.of(new SimpleGrantedAuthority("ROLE_" + active)));
  }
}
