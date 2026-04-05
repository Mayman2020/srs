# Production Hardening Closeout Report  
## Administrative Communications System

| Field | Value |
|-------|--------|
| **Date** | 5 April 2026 |
| **Scope** | Deployment (Docker/Nginx/Compose), security hardening (CORS, headers, JWT/CORS prod rules, actuator, Swagger in prod off), k6 load-test package |
| **Roles** | DevOps + Security + Performance engineering |

---

## 1. What was changed

### 1.1 Deployment artifacts (`deploy/`)

| Artifact | Description |
|----------|-------------|
| `deploy/docker/backend/Dockerfile` | Multi-stage Maven build from repo root; JRE 17 non-root user `ac`; `curl` for health checks. |
| `deploy/docker/frontend/Dockerfile` | Node 20 `npm ci` + `ng build`; Nginx serves `dist/admin-communications/browser`. |
| `deploy/docker/frontend/nginx-spa.conf` | SPA `try_files` + gzip. |
| `deploy/docker/nginx/gateway.conf` | Nginx reverse proxy: `/api/` → Spring Boot, `/` → Angular container; 55m body; baseline security headers. |
| `deploy/compose/docker-compose.yml` | PostgreSQL (healthcheck), backend (healthcheck), frontend, gateway; volumes `pgdata` + `attachments`; `restart: unless-stopped`. |
| `deploy/compose/docker-compose.staging.yml` | Sets `SPRING_PROFILES_ACTIVE=staging`, disables Spring HSTS (edge TLS). |
| `deploy/compose/docker-compose.prod.yml` | Sets `SPRING_PROFILES_ACTIVE=prod`, `AC_SECURITY_HSTS_ENABLED=false` for TLS-terminated stacks. |
| `deploy/env/.env.example`, `staging.env.example`, `prod.env.example` | Required variables documented (no committed secrets). |
| `deploy/README.md` | Runbook for Compose + profiles + TLS notes. |
| `deploy/SECURITY_HARDENING.md` | Security decisions, IDOR review summary, residual risks. |

### 1.2 Backend (Spring Boot)

| Area | Change |
|------|--------|
| **Dependencies** | `spring-boot-starter-actuator` added (`pom.xml`). |
| **Config** | `application.yml` — `management.endpoints.web.exposure` (health), `ac.security.cors.*`, `ac.security.headers.hsts-enabled`. |
| **Profiles** | New `application-staging.yml`, `application-prod.yml` (Swagger off in prod, CORS from env only, health probes). |
| **CORS** | `CorsConfig` reads comma-separated patterns; **prod** validates non-empty and **rejects localhost/127.0.0.1** patterns. |
| **Security headers** | `SecurityConfig` — frame deny, nosniff, XSS header, CSP (API-tight), Referrer-Policy, optional HSTS from config. |
| **Actuator** | `/actuator/health/**` permitted anonymously; Swagger `permitAll` **disabled** when `prod` profile active. |

**Logic preserved:** Dual filter chains for `/api/v1/auth/**` vs JWT API unchanged in behavior; only headers and authorization rules for Swagger/actuator extended.

### 1.3 Load testing (`tests/load/k6/`)

| File | Description |
|------|-------------|
| `ac-api-load.js` | k6 script: login, list, detail, create+approve workflow, multipart upload, Excel export; ramping VUs; thresholds `p(95)<3000ms`, `http_req_failed<5%`. |
| `README.md` | How to run, map to NFR-101/NFR-102, server metrics note. |

### 1.4 Repository hygiene

| Change | Reason |
|--------|--------|
| `.gitignore` — `!deploy/env/.env.example` | Allow tracked example env file despite `.env.*` ignore rule. |

---

## 2. What passed (verified in this environment)

| Check | Result |
|-------|--------|
| `mvnw compile -DskipTests` (backend) | **PASS** |
| `mvnw test` (backend) | **PASS** |
| `npm run build` (frontend) | **PASS** (existing budget warnings only) |
| Static review: correspondence + attachment download use `CorrespondenceViewAuthorization` / mutation services | **PASS** (no code change required for IDOR in reviewed paths) |

---

## 3. What failed or was not executed here

| Item | Result | Notes |
|------|--------|-------|
| `docker compose build` / `up` | **Not executed** | Docker CLI not available in the automation environment (PATH). Configs are syntactically structured for Compose v2 from repo root context. |
| `k6 run` | **Not executed** | k6 binary not installed on the host. Scripts and thresholds are ready for CI or perf workstation. |
| NFR-103 (5,000 concurrent users) | **Not validated** | Script peaks at 20 VUs; scale requires infrastructure + longer plan from `Performance_Risks_Register.md`. |
| NFR-102 (10k-row list ≤ 5s) | **Not validated** | Script uses `size=20`; needs seeded volume + dedicated scenario. |
| Server-side CPU/memory during load | **Not captured** | Requires `docker stats`/APM while k6 runs. |

---

## 4. What still blocks unconditional “full production approval”

| Blocker | Owner | Close-out action |
|---------|-------|------------------|
| **Secrets in real vault** | DevOps / Security | Replace every `change-me` / `REPLACE_*` with managed secrets; restrict `.env` file permissions. |
| **TLS + HSTS at edge** | Infra | Terminate TLS on Nginx/Ingress; add `Strict-Transport-Security` there when `AC_SECURITY_HSTS_ENABLED=false` on JVM. |
| **Production CORS allow-list** | Security + App | Set `AC_CORS_ALLOWED_ORIGIN_PATTERNS` to **only** production HTTPS origins (validated at startup in `prod`). |
| **Rate limiting / WAF** | Security | No app-level login throttling; enforce at reverse proxy or WAF. |
| **Load + soak proof** | Performance | Run k6 (or JMeter) on staging-like hardware; document p95/p99, throughput, error rate; extend for NFR-102/NFR-103. |
| **Docker image build in CI** | DevOps | Add pipeline jobs to build/push images and scan (Trivy, etc.). |
| **SMTP production** | Ops | Configure real mail relay; current defaults are dev-oriented. |

---

## 5. Sign-off recommendation

**Technical readiness:** **Improved** — repeatable container build paths, prod-oriented Spring profiles, CORS/JWT/Swagger/actuator posture documented, k6 package delivered.

**Go-live:** **Still conditional** on running **Docker** + **k6** (or equivalent) in your environment, completing **secrets/TLS/WAF** work, and recording **results** against NFRs.

---

## 6. References

- `deploy/README.md`
- `deploy/SECURITY_HARDENING.md`
- `docs/enterprise-qa/Performance_Risks_Register.md`
- `tests/load/k6/README.md`
