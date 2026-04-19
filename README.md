# SRS Administrative Correspondence System

**Domain:** **Government ERP System** — institutional resource management with administrative correspondence, Camunda-backed workflow, reference data (lookups), classification/secrecy, delegations, attachments, and audit-friendly actions. The backend is the source of truth for data and migrations; the Angular UI consumes HTTP APIs only.

*Branding reflects a government ERP aligned with digital governance and Saudi Vision 2030; extend lookups and BPMN processes to match your organization’s procedures.*

## Tech stack

| Layer | Technology |
|--------|------------|
| API | Java 17, Spring Boot 3, Spring Security (JWT resource server) |
| Database | PostgreSQL (catalogue `postgres`, dedicated `srs_system` schema — same pattern as `erp_system` in DBeaver) |
| Migrations | Flyway — single consolidated baseline; see [docs/database-enterprise.md](docs/database-enterprise.md) |
| Workflow | Camunda 7 (embedded, BPMN under `SRS_System_backend/src/main/resources/processes/`) |
| UI | Angular (`SRS_System_frontend/`) |

## Repository layout

```text
srs-project/
|- README.md
|- SRS_System_backend/       # Spring Boot API + Flyway + Camunda
|  |- pom.xml
|  `- src/main/
|     |- java/               # com.gov.ac.* (features, domain, persistence, config)
|     `- resources/
|        |- application.yml
|        |- application-local.example.yml
|        |- db/migration/    # Single consolidated Flyway baseline (`V1__…`, same idea as erp-system-backend)
|        `- processes/       # BPMN definitions
|- SRS_System_frontend/      # Angular SPA (API client only)
`- deploy/                   # Docker compose, Dockerfiles, env examples
```

### Where to look in the backend (`SRS_System_backend/src/main/java/com/gov/ac/`)

| Area | Role |
|------|------|
| `config/` | Spring Security, CORS, OpenAPI |
| `domain/` | JPA entities (`domain/lookup`, `domain/org`, `domain/correspondence`, …) |
| `persistence/` | Spring Data repositories |
| `feature/` | Vertical slices: `lookups` (bundle + admin), `delegation`, `leave`, `admin`, `users`, `roles`, … |
| `correspondence/`, `attachment/`, `modules/` | Correspondence flows, files, auth/notifications |
| `security/` | JWT, RBAC expressions |
| `common/audit/` | Resolving `app_user` display names for audit fields on API DTOs |

**Rule of thumb:** business lists that appear in dropdowns/filters are **database tables** (see the lookup section inside `V1__srs_system_full_baseline.sql`), exposed via `/api/v1/lookups` and managed under `/api/v1/admin/lookup-tables` when applicable—not hardcoded in Java except stable **codes** used in logic.

**Row audit (who / when):** Tables use PostgreSQL names `created_at`, `updated_at` (timestamps) and `created_by`, `updated_by` (UUID → `app_user.id`), with FKs to `app_user` where enforced in the consolidated Flyway baseline (indexes + audit FK sections in `V1__srs_system_full_baseline.sql`). That matches the usual CREATED_ON / MODIFIED_ON / CREATED_BY / MODIFIED_BY intent. Entities extending `AuditableEntity` get `created_by` / `updated_by` filled from the JWT principal via `AuditUserListener`. For JSON responses, expose actor display data as `UserAuditRefDto` (`id`, `fullNameAr`, `fullNameEn`) and let the UI choose the label from the active language—see `UserAuditResolutionService` and list enrichment in `CorrespondenceListService`.

**Default list ordering:** Paged APIs default to **newest first** (`sort=createdAt,desc`) for correspondence (`CorrespondenceController` + `CorrespondenceListPageables`), users, and notifications. Other operational lists use `created_at DESC` in queries where appropriate (e.g. delegations, circular inbox, leave requests). **Master/reference** UIs (lookup admin `sort_order`, letter templates, permissions, navigation) keep their configured order, not creation time.

### Where to look in the frontend (`SRS_System_frontend/src/app/`)

| Area | Role |
|------|------|
| `core/api/` | HTTP clients (`*.service.ts`), `api-types.ts` |
| `core/i18n/` | Language files: `public/assets/i18n/ar.json` & `en.json` — **all user-visible UI strings** use keys and the `t` pipe |
| `core/lookup/` | `LookupLabelsService` — resolves lookup **codes** from the API bundle to Arabic/English labels |
| `layout/` | Shell: sidebar, topbar, chat bubble |
| `features/*` | One folder per screen or feature area (lazy-loaded routes in `app.routes.ts`) |
| `shared/` | Reusable dialogs, table helpers |
| `services/` | Legacy/feature-specific facades (e.g. `transaction.service`) |

**Rule of thumb:** do not embed Arabic/English sentences in TypeScript/HTML; add keys under `public/assets/i18n/{ar,en}.json`. Chat assistant copy and search synonyms live under `chat.*` (including `chat.intentTokens.*`).

### Tests and ops

| Path | Role |
|------|------|
| `srs-project/tests/api-e2e/` | API smoke / E2E |
| `srs-project/tests/load/` | k6 load scripts |
| `srs-project/deploy/` | Compose, env samples |

## Prerequisites

- JDK 17 and Maven 3.9+
- PostgreSQL (catalogue `postgres`; app tables live in schema `srs_system`)
- Node.js 20+ and npm

## Configuration and secrets

Default `application.yml` does not commit real passwords or JWT secrets.

### Option A: environment variables

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "your-db-password"
$env:CAMUNDA_BPM_ADMIN_PASSWORD = "your-camunda-admin-password"
$env:AC_JWT_SECRET = "at-least-32-bytes-long-secret-for-hs256!!"
```

`SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` are optional; defaults point at `localhost:5432/postgres?currentSchema=srs_system` and user `postgres`. Flyway and Hibernate use schema `srs_system`.

### Option B: local profile file

1. Copy `SRS_System_backend/src/main/resources/application-local.example.yml` to `application-local.yml`.
2. Adjust values as needed and do not commit the local file.
3. Run with profile `local`:

```powershell
cd SRS_System_backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

### JWT and API calls

APIs expect `Authorization: Bearer <jwt>`. The JWT must use HS256, and claim `sub` must be the UUID of an existing `app_user`.

## Backend setup

### PowerShell runner

```powershell
cd SRS_System_backend
.\run-backend.ps1
.\run-backend.ps1 -SkipBuild
.\run-backend.ps1 -Port 8081
```

If PowerShell blocks scripts once, run:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### Manual Maven

```powershell
cd SRS_System_backend
mvn spring-boot:run
```

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Frontend setup

```powershell
cd SRS_System_frontend
npm install
npm start
```

`npm start` runs Angular dev server and proxies `/api` to the target in `SRS_System_frontend/proxy.conf.json` (commonly `http://localhost:8080`, or `http://localhost:8081` when the backend runner auto-moves off a busy 8080).

## Docker

Root-level wrappers now match the ERP repo style:

```powershell
docker compose --env-file deploy/env/.env -f docker-compose.yml config
docker compose --env-file deploy/env/.env -f docker-compose.yml up --build

.\run-docker.bat
```

Staging and production overlays:

```powershell
docker compose --env-file deploy/env/staging.env -f docker-compose.staging.yml up -d --build
docker compose --env-file deploy/env/prod.env -f docker-compose.prod.yml up -d --build
```

## Architecture overview

- Modular backend with feature-oriented packages, shared domain entities, repositories, and REST controllers.
- PostgreSQL catalogue is `postgres`; business tables and sequences live in schema `srs_system`.
- Flyway owns the schema; no PostgreSQL ENUMs are used for dynamic business values.
- Camunda runs process definitions keyed by correspondence type; `workflow_history` records the timeline.
- Security is stateless JWT; authenticated `sub` maps to `app_user.id`.
- Angular consumes REST only; no browser-side DB logic.

## GitHub

```bash
git remote add origin https://github.com/Mayman2020/srs.git
git push -u origin main
```

If `origin` already exists:

```bash
git remote set-url origin https://github.com/Mayman2020/srs.git
```
