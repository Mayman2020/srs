package com.gov.ac.config;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtDecoder jwtDecoder, Converter<Jwt, AbstractAuthenticationToken> acJwtAuthenticationConverter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/v1/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o -> o.jwt(j -> j.decoder(jwtDecoder).jwtAuthenticationConverter(acJwtAuthenticationConverter)));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${ac.security.jwt.secret}") String rawSecret) {
    byte[] secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException(
          "ac.security.jwt.secret must be at least 32 UTF-8 bytes (use AC_JWT_SECRET).");
    }
    SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /**
   * Principal is the authenticated {@link UUID} of {@code app_user} (JWT {@code sub} claim).
   */
  @Bean
  Converter<Jwt, AbstractAuthenticationToken> acJwtAuthenticationConverter() {
    return jwt -> {
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
      return new UsernamePasswordAuthenticationToken(
          userId, jwt.getTokenValue(), AuthorityUtils.NO_AUTHORITIES);
    };
  }
}
