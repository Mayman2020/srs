---
name: process-restart-health-check-handling
description: >-
  When starting Spring Boot backends locally: one instance per port, use run-backend.ps1,
  verify /actuator/health 200, ignore Maven exit_code 1 after successful Tomcat start
  caused by port cleanup/restart. Use when managing dev server lifecycle.
---

# Process Restart & Health-Check Handling

## Rules

1. **One backend per port** — use `run-backend.ps1` only (it calls `Stop-ListenerOnPort` first).
2. **Success criteria** — Tomcat started + `GET /api/v1/actuator/health` → **200 UP**.
3. **Do not treat as failure** — Maven `exit_code: 1` on `spring-boot:run` after successful start when process was killed by a later restart/port cleanup.
4. **Do not pause** for process-management discussion unless health or smoke tests fail.
5. **Continue implementation** when health is 200; run `npm run test:api` to validate.

## Workflow

```powershell
cd *-backend
.\run-backend.ps1          # sets JWT_SECRET, SPRING_PROFILES_ACTIVE=dev
# verify once:
Invoke-WebRequest http://localhost:8081/api/v1/actuator/health  # Vzeeta
Invoke-WebRequest http://localhost:8086/api/v1/actuator/health  # Clinic
cd *-frontend
npm run test:api
```

## Ports

| App | API port |
|-----|----------|
| Vzeeta | 8081 |
| Clinic | 8086 |
