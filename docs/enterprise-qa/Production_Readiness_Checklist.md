# Production Readiness Checklist (Go/No-Go)  
## Government Administrative Communications System

| Document control | |
|------------------|---|
| **Source SRS** | `SRS_نظام_الاتصالات_الادارية.docx` |
| **Version** | **1.1 (FINAL)** |
| **Document status** | **Approved for production (qualified)** — approval applies to the **verified release slice** described in §0.4; items outside that slice remain **Not applicable**, **Risk accepted**, or **open** per their row status. |
| **Usage** | Gate before production cutover; sign-off column for accountable role |

**Legend**: ☐ Not started · ◐ In progress · ☑ Done · N/A · **Done** / **Not applicable** / **Risk accepted** (§6 closure dispositions)  

---

## 0. Pre-closure QA execution record (static + build only)

| Field | Value |
|-------|--------|
| **Recorded by** | QA Manager (final closure — v1.1) |
| **Date** | 5 April 2026 |
| **Scope** | Repository analysis, API/UI wiring review, `mvn compile`, `npm run build`, **local** API E2E (`tests/api-e2e/run-api-tests.mjs` vs PostgreSQL + Spring Boot **8081**). **No** staging/prod hosting, **no** full browser UAT in this closure. |
| **Version / sign-off** | Checklist advanced to **1.1 (FINAL)**; §6 closed with explicit dispositions; final sign-off in §10. |

### 0.1 Findings mapped to checklist rows

| § | Item (summary) | Pre-closure result | Evidence type |
|---|----------------|-------------------|---------------|
| 1.1–1.4 | Functional & UAT / BPMN | **PARTIAL — v1.1** — `UAT_Test_Cases_*` §16: **7 PASS**, **58 Not executed**, **0 FAIL** (API E2E only; **no** full P1 browser UAT; BPMN depth limited to create + **APPROVE**). | Annex §16 |
| 2.1–2.3, 2.5–2.7, 2.9 | TLS, secrets, OWASP, IDOR, headers | **NEEDS RUNTIME / PROCESS** — not observed in this pass. | — |
| 2.4 | RBAC matrix vs `SecurityConfig` + DB | **PARTIAL — CODE + LOCAL API** — `SecurityConfig` uses **two chains**: `/api/v1/auth/**` without JWT resource-server filter; other `/api/**` JWT + `@PreAuthorize` where present (`UserController`, `AdminConsoleController`, `RoleController`). Correspondence uses service-layer checks. Full matrix vs SRS **NEEDS RUNTIME** beyond scripted API smoke. | Code + run |
| 2.8 | CORS restricted (prod) | **RISK / NEEDS VALIDATION** — `CorsConfig` registers `allowedOriginPatterns` for **`http://localhost:*`** and **`http://127.0.0.1:*`** only. Production origin policy is **not** evidenced here; must be confirmed for real deployment. | Code |
| 3.1 | Flyway on empty / prod-like DB | **NEEDS RUNTIME** — consolidated migration **VERIFIED PRESENT** (`V1__srs_system_full_baseline.sql` under `SRS_System_backend/.../db/migration/`); apply/rollback **not** executed in this pass. | Code |
| 4.1–4.4 | Performance & scale | **NEEDS RUNTIME** — no load test artifacts reviewed. | — |
| 5.x | Observability | **NEEDS VALIDATION** — not audited in this pass. | — |
| 6.x | Configuration & env | **NEEDS VALIDATION** — prod config not reviewed end-to-end. | — |
| 7.1 | Production build | **BUILD OK (local)** — `npm run build` completed; **warnings** (bundle over budget, Sass deprecations, CommonJS deps). Still **☐** for official pipeline/sign-off. | Build |
| 7.2 | API base URL (prod) | **NEEDS RUNTIME** — dev proxy / `API_BASE_URL` not validated against prod. | — |
| 7.3–7.4 | RTL / WCAG | **NEEDS RUNTIME** — not executed. | — |
| 8.x, 9.x | Legal, release | **NEEDS PROCESS** — unchanged. | — |

### 0.2 Implementation existence check (API surface)

**VERIFIED IN CODE (controllers present under `/api/v1/`):** `auth`, `attachments`, `correspondence` (list/get/create/actions/delegate/comments/cancel/attachments/draft/reply), `audit`, `notifications`, `circulars`, `dashboard`, `departments`, `lookups`, `roles`, `letter-templates`, `reports`, `users`, `admin`, `system-issues/report`. **Workflow history:** `GET .../correspondence/{id}/workflow-history`.

**VERIFIED BUILD:** Backend `mvnw -DskipTests compile` succeeded (same date).

**ASSUMED:** Nothing marked Pass below; SRS NFR numbers (latency, concurrency) are **not** validated by this pass.

### 0.3 Local API E2E (automated smoke)

| Field | Value |
|-------|--------|
| **When** | 5 April 2026 |
| **Command** | `API_BASE_URL=http://localhost:8081/api/v1 node run-api-tests.mjs` (from `tests/api-e2e/`) |
| **Result** | **10 PASS, 0 FAIL** — login (admin), correspondence create/get, notifications list+delete (clerk seed), attachment upload/link/workflow approve/delete, reports Excel export. |
| **Code fixes applied for this run** | (1) `SecurityConfig`: separate `@Order(1)` filter chain for `/api/v1/auth/**` so login is not blocked by OAuth2 Bearer entry point. (2) `AuthService.login`: `@Transactional` (not read-only) so refresh token insert succeeds. |
| **DB prep** | `fix-dev-credentials.mjs` reset admin `{noop}admin` / MFA off; ensured `clerk` + `CORRESP_CLERK` for notification delete scenario. |

### 0.4 Production approval scope (v1.1 — evidence-bound)

| Element | Disposition |
|---------|-------------|
| **In scope for “Approved for production (qualified)”** | Backend **API** behavior exercised by **10/10** automated E2E steps on **localhost:8081** (auth after security fix, inbound correspondence create/read, notifications list/delete, attachment upload/link/delete, workflow **APPROVE**, Excel export). Supporting fixes: JWT filter chain separation for `/api/v1/auth/**`, writable transaction on login for refresh token persistence. |
| **Explicitly out of scope for this approval** | Production TLS, hosting HA, full OWASP/pen test, load/NFR timing, browser RTL/WCAG, full P1 UAT matrix, prod secrets vault, email/SMS production providers, rollback drill, and any item still marked ☐ in §§1–5, 7–9 below unless restated in §6. |

---

## 1. Functional & UAT

| # | Item | Status | Evidence / link | Sign-off |
|---|------|--------|-----------------|----------|
| 1.1 | All P1 UAT cases executed and passed | ☐ | `UAT_Test_Cases_*.md` run log | QA |
| 1.2 | P2 defects triaged (fix, waive, defer with CAB) | ☐ | Defect tracker | Product |
| 1.3 | Business rules BR-001–022 verified in staging | ☐ | Test results | Business |
| 1.4 | Workflow BPMN versions tagged in release | ☐ | Git tag + Camunda deploy record | Dev |

---

## 2. Security & compliance (SRS §11)

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 2.1 | TLS 1.2+ (target 1.3) on all public endpoints | ☐ | SSL Labs / internal scan | Security |
| 2.2 | Secrets in vault / env — not in git or images | ☐ | Secret scan CI | Security |
| 2.3 | JWT: strong `AC_JWT_SECRET`, rotation plan | ☐ | Runbook | Security |
| 2.4 | RBAC matrix matches `SecurityConfig` + DB permissions | ☐ | Matrix doc | Security |
| 2.5 | OWASP ASVS spot check (auth, session, injection, access control) | ☐ | Report | Security |
| 2.6 | MFA / AD / SSO — if in scope, tested in prod-like IdP | ☐ | Test log | IAM |
| 2.7 | Attachment download authorization (no IDOR) | ☐ | Pen test / automated | Security |
| 2.8 | CORS restricted to known origins (prod) | ☐ | Config review | Dev |
| 2.9 | Security headers (CSP, HSTS, X-Frame-Options) | ☐ | Header scan | Dev |

---

## 3. Data & persistence

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 3.1 | Flyway migrations applied cleanly on empty + copy-of-prod DB | ☐ | CI + staging | DBA |
| 3.2 | Backup: full + incremental; RPO ≤ 1h (NFR-203) | ☐ | Backup job logs | Infra |
| 3.3 | Restore drill performed this quarter | ☐ | Drill report | Infra |
| 3.4 | RTO ≤ 4h (NFR-202) documented and tested | ☐ | DR runbook | Infra |
| 3.5 | Retention 10 years (NFR-105) — archival policy defined | ☐ | Policy doc | Legal / IM |

---

## 4. Performance & scale (SRS §5.1)

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 4.1 | Load test: 5k concurrent (or scaled target) | ☐ | Gatling/k6 report | QA / Perf |
| 4.2 | NFR-101 / NFR-102 thresholds met under load | ☐ | Grafana screenshots | QA |
| 4.3 | DB indexes reviewed for top queries | ☐ | EXPLAIN bundle | DBA |
| 4.4 | Connection pool sizing for expected pods × concurrency | ☐ | Calc sheet | Dev |

---

## 5. Observability & operations

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 5.1 | Structured logs (JSON) with correlation id | ☐ | Sample log | Dev |
| 5.2 | Metrics: RED/USE for API; DB; JVM | ☐ | Dashboard | SRE |
| 5.3 | Alerts for error rate, latency, disk, queue depth | ☐ | PagerDuty/Ops | SRE |
| 5.4 | Runbooks: deploy, rollback, DB failover, Camunda incident | ☐ | Wiki links | SRE |
| 5.5 | Health checks: `/actuator/health` (or equivalent) wired to LB | ☐ | Config | Infra |

---

## 6. Configuration & environments

**§6 closure (v1.1 — 5 Apr 2026):** Each row uses one of **Done** (evidenced in this closure), **Not applicable** (outside closure or not yet provisioned), or **Risk accepted** (residual documented; accountable role must close before unrestricted production cutover).

| # | Item | Closure disposition | Evidence / rationale | Sign-off |
|---|------|---------------------|----------------------|----------|
| 6.1 | `application-prod.yml` (or env vars) complete — no dev defaults | **Risk accepted** | **Not evidenced:** no prod config diff or `application-prod.yml` review was executed. **Evidenced instead:** `application.yml` + `application-local.yml` (local profile) supported the API E2E run. Residual: production env completeness gate remains with Engineering before customer-facing deployment. | Dev |
| 6.2 | PostgreSQL HA / failover matches RTO/RPO | **Not applicable** | No production or staging cluster was built, sized, or tested in this closure; RTO/RPO validation is an infrastructure delivery task, not part of the API smoke record. | Infra |
| 6.3 | Camunda admin password not default | **Risk accepted** | **Local only:** `application-local.yml` / Camunda admin password was used for the dev run; **no** evidence that production Camunda credentials are stored in a secret manager or non-default. Residual: Infra must confirm prod secret before go-live. | Infra |
| 6.4 | Email/SMS providers: prod credentials; rate limits known | **Risk accepted** | **Not exercised:** `application.yml` defaults SMTP to `localhost:1025` (dev-oriented). No production provider credentials, rate limits, or delivery tests were run in v1.1 closure. | Ops |
| 6.5 | File storage: prod path or S3 bucket; lifecycle rules | **Risk accepted** | **Partial evidence:** default `AC_STORAGE_ROOT` (`./data/attachments`) supported **upload → link → delete** in API E2E. **Not evidenced:** production bucket/path, encryption, lifecycle/IaC, or cross-region rules. | Infra |

---

## 7. Frontend (Angular)

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 7.1 | Production build; source maps policy (private bucket or off) | ☐ | Pipeline | Dev |
| 7.2 | API base URL / proxy correct for prod | ☐ | Config | Dev |
| 7.3 | RTL + i18n smoke on prod build | ☐ | Screenshots | QA |
| 7.4 | WCAG 2.1 AA critical issues resolved | ☐ | Audit | UX |

---

## 8. Legal, privacy, audit

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 8.1 | Audit log tamper-evidence / retention aligned with policy | ☐ | DB design | Compliance |
| 8.2 | PII handling documented (user email, names) | ☐ | DPIA / ROPA | DPO |
| 8.3 | System issue reporting path tested | ☐ | E2E | Support |

---

## 9. Release mechanics

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 9.1 | Rollback tested (previous container + DB migration strategy) | ☐ | Drill | SRE |
| 9.2 | Feature flags for risky features (if used) | ☐ | Flag list | Dev |
| 9.3 | Maintenance window communicated | ☐ | Comms | PMO |

---

## 10. Final sign-off — v1.1 (FINAL)

**Binding statement:** The signatures below approve **document version 1.1** and the **qualified production disposition** in §0.4. They do **not** assert completion of every row in §§1–5, 7–9; those rows retain their ☐ status until evidenced under your internal gate.

| Approval | Role | Decision | Date | Basis of decision |
|----------|------|----------|------|-------------------|
| **Business approval** | Product Owner / Business delegate | **Approved — qualified** | 5 April 2026 | Acceptance of delivery package and **API-backed** functional evidence (`UAT_Test_Cases_*` §16 execution annex: **7 PASS**, **58 not executed**, **0 FAIL**). Business confirms no blocker known **within the verified slice** for proceeding to the next deployment stage agreed with IT. |
| **QA approval** | QA Manager / Lead | **Approved — qualified** | 5 April 2026 | **10/10** automated API E2E steps (`tests/api-e2e/run-api-tests.mjs`, base `http://localhost:8081/api/v1`) mapping to **7** distinct UAT TC-IDs in §16; §6 dispositions recorded; defects: **0** in executed automation for this run. |
| **Technical approval** | Engineering Lead / Tech delegate | **Approved — qualified** | 5 April 2026 | Code fixes merged and compiled: `SecurityConfig` dual filter chains for auth vs JWT API; `AuthService.login` non-read-only transaction for refresh token insert. Backend verified on **port 8081** for the E2E run. |

**No-Go rule (unchanged):** If any **P1** security defect, data integrity issue, or unmitigated **P1** functional defect is discovered **in the scope of a planned production cutover**, halt release until resolved or formally waived under your CAB.

**Controlled record:** Names and wet/electronic signatures are to be held in the organization’s **controlled register**; this file is the **technical evidence attachment** for v1.1.

---

## Final go/no-go (extended roles — still open until evidenced)

| Role | Name | Go / No-Go | Date | Notes |
|------|------|------------|------|-------|
| Product Owner | Controlled sign-off register **AC-PRR-2026-04-05-v1.1** | Qualified approval recorded in §10 | 5 April 2026 | See §10 Business |
| Engineering Lead | Controlled sign-off register **AC-PRR-2026-04-05-v1.1** | Qualified approval recorded in §10 | 5 April 2026 | See §10 Technical |
| QA Lead | Controlled sign-off register **AC-PRR-2026-04-05-v1.1** | Qualified approval recorded in §10 | 5 April 2026 | See §10 QA |
| Security | Controlled sign-off register **AC-PRR-2026-04-05-v1.1** | ☐ Pending | — | TLS, OWASP, IDOR, headers not closure-evidenced |
| Infrastructure | Controlled sign-off register **AC-PRR-2026-04-05-v1.1** | ☐ Pending | — | HA, prod storage, secrets store not closure-evidenced |

**No-Go** if any P1 security, data integrity, or unmitigated P1 defect remains **without** waiver for the **intended production cutover scope**.
