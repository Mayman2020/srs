---
name: spring-boot-production-cors
description: >-
  Configure Spring Boot CORS for production using CORS_ALLOWED_ORIGINS env var,
  dev profile localhost defaults, and prod fail-fast validation. Use when fixing
  CORS for RMS/Vzeeta/Clinic Angular apps or hardening cross-origin API access.
---

# Spring Boot Production CORS

## Pattern

1. **`CorsProperties`** — `@ConfigurationProperties(prefix = "app.cors")` with comma-separated `allowedOriginPatterns` from `${CORS_ALLOWED_ORIGINS}`.
2. **`CorsConfig`** — `setAllowedOriginPatterns()` (not hardcoded origins); fail if list empty at bean creation.
3. **`CorsOriginValidator`** — `InitializingBean` throws if `prod` profile active and origins empty.
4. **`application-dev.yml`** — localhost defaults only:
   ```yaml
   app.cors.allowed-origin-patterns: ${CORS_ALLOWED_ORIGINS:http://localhost:[*],http://127.0.0.1:[*]}
   ```
5. **`application-prod.yml`** — no defaults:
   ```yaml
   app.cors.allowed-origin-patterns: ${CORS_ALLOWED_ORIGINS}
   ```
6. **`run-backend.ps1`** — set `SPRING_PROFILES_ACTIVE=dev` when unset.
7. **`.env.example`** — document `CORS_ALLOWED_ORIGINS` with example patterns (no real prod domains in code).

## Rules

- Never hardcode production domains in Java or default `application.yml`.
- Use origin **patterns** (`https://*.example.com`) when subdomains vary.
- `allowCredentials(true)` requires explicit origins (not `*`).
- Test profile: add localhost patterns in `application-test.yml`.
- Dev: `SPRING_PROFILES_ACTIVE=dev` via `run-backend.ps1`; prod requires `CORS_ALLOWED_ORIGINS`.

## Reference

- Vzeeta: `vzeeta-backend/src/main/java/com/vzeeta/config/Cors*.java`
- Clinic: `clinic-backend/src/main/java/com/clinicmanagement/config/Cors*.java`
