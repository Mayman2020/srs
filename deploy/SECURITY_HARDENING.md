# Security hardening — Administrative Communications API

## Changes implemented (code + deployment)

### CORS

- **Before:** Hard-coded `http://localhost:*` and `http://127.0.0.1:*`.
- **After:** `ac.security.cors.allowed-origin-patterns` from **`AC_CORS_ALLOWED_ORIGIN_PATTERNS`** (comma-separated Ant-style patterns). Default in base `application.yml` keeps localhost for developer workstations.
- **Production:** `application-prod.yml` sets patterns **only** from env (no localhost default). `CorsConfig` **fails startup** if `prod` profile has empty patterns or patterns containing `localhost` / `127.0.0.1`.

### JWT (HS256)

- **Requirement:** `AC_JWT_SECRET` / `ac.security.jwt.secret` — **≥ 32 UTF-8 bytes** (enforced at `JwtDecoder` bean creation).
- **Rotation plan (operational):** Store secret in vault; rotate by deploying a new value (all issued JWTs invalidate immediately). For zero-downtime dual-key signing, a code change would be required (not implemented). Recommended: rotate during maintenance window; communicate forced re-login.

### HTTP security headers (Spring Security)

Applied to **both** filter chains (auth + API):

- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'` (JSON API baseline)
- `Referrer-Policy: strict-origin-when-cross-origin`
- **HSTS:** `ac.security.headers.hsts-enabled` / `AC_SECURITY_HSTS_ENABLED` — default **false** in `application.yml`; **true** in `application-prod.yml` unless overridden. When TLS terminates at Nginx/Ingress, set **`AC_SECURITY_HSTS_ENABLED=false`** on the JVM and send **Strict-Transport-Security** only at the TLS edge (`deploy/docker/nginx` TLS variant or cloud LB).

### Nginx gateway (`deploy/docker/nginx/gateway.conf`)

- `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, minimal `Permissions-Policy`
- `client_max_body_size 55m` (aligned with Spring multipart limits)

### Actuator / health

- Dependency: `spring-boot-starter-actuator`.
- Exposure: **health** (plus **liveness/readiness** subgroup when probes enabled in prod profile).
- **Security:** `GET /actuator/health` and `/actuator/health/**` **permitAll**; other actuator endpoints not exposed in prod config.

### OpenAPI / Swagger

- **Production profile:** `springdoc.swagger-ui.enabled=false`, `springdoc.api-docs.enabled=false`.
- **Security:** Swagger paths are **not** `permitAll` when `prod` is active (`SecurityConfig`).

---

## Access control review (IDOR / OWASP basics)

### Correspondence

- **Detail:** `CorrespondenceDetailService.getById` → `CorrespondenceViewAuthorization.assertCanView` (same department as owner, workflow participant, or privileged view role).
- **List:** `CorrespondenceListService` filters by department unless privileged role.
- **Workflow actions:** `CorrespondenceWorkflowActionService` checks assignee / Camunda task ownership after `assertCanView`.

### Attachments

- **Download:** `AttachmentDownloadService.download` loads attachment with correspondence, then **`correspondenceViewAuthorization.assertCanView`** before streaming bytes.
- **Upload:** Authenticated users may upload files to storage; linking to correspondence is a **separate** authorized mutation (`CorrespondenceAttachmentMutationService`). **Residual risk:** orphaned uploads in storage if never linked — mitigate with retention job / max orphan policy (operational, not coded here).
- **Delete:** `AttachmentDeletionService` enforces correspondence mutation rules.

### Auth

- **Session:** Stateless JWT; OAuth2 resource server on `/api/**` except dedicated auth chain for `/api/v1/auth/**` (avoids Bearer entry point blocking login).
- **Brute force / rate limiting:** Not implemented at application layer — **recommend** edge rate limit (Nginx, WAF, API gateway).

---

## Residual risks (not closed in this delivery)

| Risk | Owner | Mitigation path |
|------|-------|-----------------|
| No distributed rate limit on `/auth/login` | SecOps | Nginx `limit_req` / WAF / cloud shield |
| JWT theft (XSS on SPA) | FE + Sec | CSP on HTML shell (Nginx for static), secure cookie if moving token storage |
| 5k concurrent users (NFR-103) | Perf | Horizontal scale + DB tuning + load proof |
| Secrets in shell history | DevOps | Vault inject; never `echo` secrets |
| Camunda / Postgres default passwords in examples | All | Replace all `change-me` before any internet-facing deploy |

---

## References

- OWASP ASVS (authentication, access control, data protection).
- SRS security section (Arabic SRS document referenced by QA pack).
