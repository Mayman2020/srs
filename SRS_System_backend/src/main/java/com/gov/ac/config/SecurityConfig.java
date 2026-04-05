package com.gov.ac.config;

import com.gov.ac.security.AcJwtAuthenticationConverter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final AcJwtAuthenticationConverter acJwtAuthenticationConverter;
  private final Environment environment;

  @Value("${ac.security.headers.hsts-enabled:false}")
  private boolean hstsEnabled;

  private boolean isProdProfile() {
    return List.of(environment.getActiveProfiles()).contains("prod");
  }

  private void applySecurityHeaders(HttpSecurity http) throws Exception {
    http.headers(
        headers -> {
          headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
          headers.contentTypeOptions(Customizer.withDefaults());
          headers.xssProtection(
              xss ->
                  xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
          headers.contentSecurityPolicy(
              csp ->
                  csp.policyDirectives(
                      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"));
          headers.referrerPolicy(
              ref ->
                  ref.policy(
                      ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
          if (hstsEnabled) {
            headers.httpStrictTransportSecurity(
                hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true));
          }
        });
  }

  /**
   * Auth endpoints must not use the OAuth2 resource-server JWT filter: without a Bearer token that
   * filter responds with 401 + {@code WWW-Authenticate: Bearer} before {@code permitAll} is applied,
   * so login/refresh/MFA never reach {@link com.gov.ac.modules.auth.controller.AuthController}.
   */
  @Bean
  @Order(1)
  SecurityFilterChain authEndpointsChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/v1/auth/**")
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    applySecurityHeaders(http);
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
      throws Exception {
    http.securityMatcher("/**")
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    applySecurityHeaders(http);
    http.authorizeHttpRequests(
        a -> {
          a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
          a.requestMatchers(HttpMethod.POST, "/api/v1/system-issues/report").permitAll();
          a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
          if (!isProdProfile()) {
            a.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/v1/api-docs/**")
                .permitAll();
          }
          a.anyRequest().authenticated();
        });
    http.oauth2ResourceServer(
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
}
