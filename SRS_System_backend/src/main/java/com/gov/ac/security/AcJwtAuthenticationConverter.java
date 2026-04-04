package com.gov.ac.security;

import com.gov.ac.persistence.RoleRepository;
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
 * Principal is {@link UUID} app_user id (JWT {@code sub}); authorities are {@code ROLE_}{@code
 * <role.code>} for each active assignment from {@code user_role}.
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
    List<SimpleGrantedAuthority> authorities =
        roleRepository.findActiveRoleCodesByUserId(userId).stream()
            .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
            .toList();
    return new UsernamePasswordAuthenticationToken(userId, jwt.getTokenValue(), authorities);
  }
}
