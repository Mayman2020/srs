---
name: environment-variable-hardening
description: >-
  Harden Spring Boot apps with required env vars (JWT_SECRET, CORS_ALLOWED_ORIGINS),
  fail-fast validators, dev profile defaults in run scripts only, .env.example docs.
  Use for production readiness in RMS/Vzeeta/Clinic projects.
---

# Environment Variable Hardening

## Required production vars

| Variable | Validator | Dev fallback location |
|----------|-----------|----------------------|
| `JWT_SECRET` (≥32 chars) | `JwtSecretValidator` | `run-backend.ps1` only |
| `CORS_ALLOWED_ORIGINS` | `CorsOriginValidator` (prod) | `application-dev.yml` |
| `PASSWORD_RESET_BASE_URL` | prod yml `${...}` no default | `application-dev.yml` |
| `POSTGRES_PASSWORD` | docker-compose `${VAR:?msg}` | `.env.example` |
| `MAIL_ENABLED` | optional | `management.health.mail.enabled` tied to mail.enabled |

## Profile strategy

- **`dev`** — `run-backend.ps1` sets `SPRING_PROFILES_ACTIVE=dev`; localhost CORS, optional mail off.
- **`prod`** — no secret defaults in `application.yml`; compose/k8s injects all secrets.
- **`test`** — `application-test.yml` supplies test JWT + CORS; `@ActiveProfiles("test")`.

## Fail-fast pattern

```java
@Component
public class XxxValidator implements InitializingBean {
  public void afterPropertiesSet() {
    if (isProd() && valueMissing()) throw new IllegalStateException("ENV required");
  }
}
```

## Deliverables per hardening task

1. Remove inline defaults from `application.yml` for secrets.
2. Add `.env.example` with placeholders (no real secrets).
3. Update `run-backend.ps1` to set dev-only fallbacks.
4. Update `docker-compose.yml` with `${VAR:?error}` syntax.

## Reference

- Vzeeta: `.env.example`, `JwtSecretValidator`, `CorsOriginValidator`
- Clinic: `.env.example`, `docker-compose.yml`
