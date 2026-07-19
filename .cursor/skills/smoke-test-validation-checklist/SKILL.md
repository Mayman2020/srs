---
name: smoke-test-validation-checklist
description: >-
  Run and extend npm run test:api smoke tests after backend fixes. Checklist for
  Vzeeta/Clinic full-stack validation before sprint sign-off.
---

# Smoke Test Validation Checklist

## Prerequisites

1. Backend running (`.\run-backend.ps1` — one instance per port; see **process-restart-health-check-handling** skill).
2. Health: `GET /api/v1/actuator/health` → 200 before smoke tests.
3. PostgreSQL up with Flyway migrations applied.
4. Default dev credentials: `Dev@Local2026!`

## Commands

```powershell
# Vzeeta (port 8081)
cd vzeeta-backend; .\run-backend.ps1
cd vzeeta-web; npm run test:api; npm run build

# Clinic (port 8086)
cd clinic-backend; .\run-backend.ps1
cd clinic-frontend; npm run test:api; npm run build
```

## After each sprint fix, verify

- [ ] `mvn compile` passes
- [ ] Unit tests (`mvn test -Dtest=...`) pass
- [ ] API smoke: all checks green
- [ ] `ng build` passes
- [ ] New endpoints added to `scripts/api-integration-test.mjs`

## Extend smoke tests when adding

| Feature | Endpoint to add |
|---------|-----------------|
| File upload | `POST /files/upload` (multipart) |
| Attachments | GET/POST/DELETE `/patient/attachments` |
| Forgot password | `POST /auth/forgot-password` (no auth) |
| CORS | Manual browser preflight or integration test |

## Expected counts (update after extending)

- Vzeeta: **86** checks (incl. file upload, attachments, forgot-password)
- Clinic: **27** checks (incl. forgot-password)

## Reference scripts

- `Vzeeta Project/vzeeta-web/scripts/api-integration-test.mjs`
- `Clinic System/clinic-frontend/scripts/api-integration-test.mjs`
