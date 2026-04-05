package com.gov.ac.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS from configuration. Default allows local Angular dev; {@code prod} profile requires explicit
 * non-localhost patterns via {@code AC_CORS_ALLOWED_ORIGIN_PATTERNS}.
 */
@Configuration
public class CorsConfig {

  private final Environment environment;

  @Value("${ac.security.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
  private String allowedOriginPatternsRaw;

  public CorsConfig(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void validateProdCors() {
    List<String> profiles = Arrays.asList(environment.getActiveProfiles());
    if (!profiles.contains("prod")) {
      return;
    }
    List<String> patterns = parsePatterns(allowedOriginPatternsRaw);
    if (patterns.isEmpty()) {
      throw new IllegalStateException(
          "Production profile requires non-empty AC_CORS_ALLOWED_ORIGIN_PATTERNS (maps to ac.security.cors.allowed-origin-patterns).");
    }
    for (String p : patterns) {
      String lower = p.toLowerCase();
      if (lower.startsWith("http://localhost")
          || lower.startsWith("http://127.0.0.1")
          || lower.startsWith("https://localhost")
          || lower.startsWith("https://127.0.0.1")) {
        throw new IllegalStateException(
            "Production CORS must not include localhost/127.0.0.1 patterns; got: "
                + p
                + ". Remove dev origins from AC_CORS_ALLOWED_ORIGIN_PATTERNS.");
      }
    }
  }

  private static List<String> parsePatterns(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    List<String> patterns = parsePatterns(allowedOriginPatternsRaw);
    if (patterns.isEmpty()) {
      if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
        throw new IllegalStateException(
            "Production requires non-empty ac.security.cors.allowed-origin-patterns (AC_CORS_ALLOWED_ORIGIN_PATTERNS).");
      }
      patterns = List.of("http://localhost:*", "http://127.0.0.1:*");
    }
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(patterns);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
