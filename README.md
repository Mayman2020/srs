# SRS Administrative Communications System

Government administrative correspondence platform: registry, workflow (Camunda), lookups, and Angular UI. Backend is the source of truth for data and migrations; the frontend consumes HTTP APIs only.

## Tech stack

| Layer | Technology |
|--------|------------|
| API | Java 17, Spring Boot 3, Spring Security (JWT resource server) |
| Database | PostgreSQL |
| Migrations | Flyway (`SRS_System_backend/src/main/resources/db/migration/`) |
| Workflow | Camunda 7 (embedded, BPMN under `SRS_System_backend/src/main/resources/processes/`) |
| UI | Angular (see `SRS_System_frontend/`) |

## Repository layout

```
admin-communications-main/
├── README.md                 # This file
├── .gitignore
├── SRS_System_backend/       # Spring Boot API + Flyway + Camunda
│   ├── pom.xml
│   └── src/main/
│       ├── java/             # com.gov.ac.* (features, domain, persistence, config)
│       └── resources/
│           ├── application.yml
│           ├── application-local.example.yml
│           ├── db/migration/ # Flyway only (no DB logic in frontend)
│           └── processes/    # BPMN definitions
└── SRS_System_frontend/      # Angular SPA (API client only)
```

## Prerequisites

- **JDK 17** and **Maven 3.9+**
- **PostgreSQL** (database created, e.g. `ac_communications`)
- **Node.js 20+** and **npm** (for the frontend)

## Configuration and secrets

Default `application.yml` does **not** commit real passwords or JWT secrets. You must either:

### Option A — Environment variables (recommended)

Set before starting the backend (examples for PowerShell):

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "your-db-password"
$env:CAMUNDA_BPM_ADMIN_PASSWORD = "your-camunda-admin-password"
$env:AC_JWT_SECRET = "at-least-32-bytes-long-secret-for-hs256!!"
```

`SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` are optional; defaults point at `localhost:5432/ac_communications` and user `postgres`.

### Option B — Local profile file (gitignored)

1. Copy `SRS_System_backend/src/main/resources/application-local.example.yml` to `application-local.yml` (same folder).
2. Adjust values as needed; **do not commit** `application-local.yml`.
3. Run with Spring profile `local`:

```powershell
cd SRS_System_backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

### JWT and API calls

APIs expect `Authorization: Bearer <jwt>`. The JWT must use **HS256**; the claim **`sub`** must be the UUID of an existing `app_user` (see Flyway seeds, e.g. system user in migrations).

## Backend setup

### Option C — PowerShell runner (Windows)

From `SRS_System_backend`:

- **Double-click:** `run-backend.cmd` (runs PowerShell with bypass execution policy; window stays open at the end with `pause`).
- **Or in PowerShell / Terminal:**

```powershell
cd SRS_System_backend
# Optional: run-backend.secrets.ps1 (gitignored) to override DB password / JWT for special setups
.\run-backend.ps1
.\run-backend.ps1 -SkipBuild
.\run-backend.ps1 -Port 8081   # only if Oracle TNSLSNR blocks 8080
```

If `.\run-backend.ps1` says scripts are disabled, run once:  
`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

**LOCAL defaults:** `SPRING_PROFILES_ACTIVE=local`, **`application-local.yml`** (committed dev settings: CORS, Swagger, Actuator, JWT, DB password), **port 8080** (matches `SRS_System_frontend/proxy.conf.json`). `spring.profiles.default=local` in `application.yml` so `mvn spring-boot:run` without env also loads local config. Use **`-Port 8081`** if 8080 is taken. The repo includes **Maven Wrapper** (`mvnw.cmd`).

### Manual Maven

```powershell
cd SRS_System_backend
# Set env vars (Option A) or use application-local.yml (Option B)
mvn spring-boot:run
```

- API base: `http://localhost:8080/api/v1` with default `run-backend.ps1` (port **8080**)
- OpenAPI / Swagger UI: `http://localhost:8080/swagger-ui.html` (same port unless you override `-Port`)

Ensure PostgreSQL is running and Flyway can apply migrations on startup.

## Frontend setup

```powershell
cd SRS_System_frontend
npm install
npm start
# or: .\run-frontend.ps1  → ng serve --configuration=development
```

`npm start` runs **`ng serve --configuration=development`**. `proxy.conf.json` forwards `/api` to **`http://localhost:8080`** (backend default).

## Architecture overview

- **Modular backend**: Feature-oriented packages (e.g. `com.gov.ac.correspondence` for create flow), shared **domain** entities, **persistence** repositories, **lookup** resolution for master data codes, REST **web** layer.
- **Data**: PostgreSQL schema owned by Flyway; **no PostgreSQL ENUMs** — dynamic values use lookup tables (`code`, `name_ar`, `name_en`).
- **Workflow**: Camunda runs process definitions keyed by correspondence type (e.g. inbound vs outbound); `workflow_instance` bridges Camunda to correspondence; **`workflow_history`** records the timeline (including create events).
- **Security**: Stateless JWT; authenticated `sub` maps to `app_user.id`.
- **Frontend**: Angular consumes REST only; translations and UI strings use i18n keys in the app (no DB lookups in the browser).

## GitHub

Remote name: `origin`  
Repository: **`srs`** under your GitHub user (example: `Mayman2020`).

```bash
git remote add origin https://github.com/Mayman2020/srs.git
```

If `origin` already exists, update it:

```bash
git remote set-url origin https://github.com/Mayman2020/srs.git
```

Push (after GitHub credentials or SSH are configured):

```bash
git push -u origin main
```

## License / compliance

Add your organization’s license and policies as required.
