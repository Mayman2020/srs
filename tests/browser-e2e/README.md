# Browser E2E & UAT (Playwright)

Automated smoke and role-based UAT specs for the SRS frontend. **The backend must be running** (`local` profile with `V900__demo_data.sql`) for login and API-backed screens to work.

## Prerequisites

| Service | Command | URL |
|---------|---------|-----|
| Backend | `cd SRS_System_backend && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local` | `http://localhost:8080` |
| Frontend | `cd SRS_System_frontend && npm start` | `http://localhost:4205` |

Apply Flyway **V29** before testing business-gap routes.

## Run tests

```powershell
cd tests/browser-e2e
npm install
$env:E2E_BASE_URL = "http://localhost:4205"
$env:E2E_SKIP_WEB_SERVER = "1"   # if frontend already running
npm test
```

### Role credentials (demo)

| Role | User | Password |
|------|------|----------|
| Clerk | `clerk` | `clerk` |
| Dept manager | `deptmgr` | `deptmgr` |
| Auditor | `auditor` | `auditor` |
| Admin | `admin` | `admin` |

Override via `E2E_CLERK_USER`, `E2E_DEPTMGR_USER`, `E2E_AUDITOR_USER`, etc.

## Spec files

| File | Purpose |
|------|---------|
| `specs/smoke.spec.ts` | Login + correspondence list |
| `specs/business-gaps.spec.ts` | Registration desk, outbound, circular report, workflow |
| `specs/uat-clerk.spec.ts` | Clerk role dashboard + intake screens |
| `specs/uat-deptmgr.spec.ts` | Dept manager workflow + reports |
| `specs/uat-auditor.spec.ts` | Auditor compliance screens |
| `specs/responsive.spec.ts` | Viewport shell tests (375 / 768 / 1280) |

### Responsive E2E viewports

`responsive.spec.ts` exercises the app shell at **375px**, **768px**, and **1280px**:

- No unintended horizontal scroll on dashboard
- Mobile drawer + backdrop open/close
- Desktop sidebar in normal document flow

## Manual UAT checklist

See **[docs/uat-checklist-ar.md](../../docs/uat-checklist-ar.md)** for step-by-step Arabic checklists per role (`clerk`, `deptmgr`, `auditor`).
