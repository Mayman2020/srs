---
name: rms-fullstack-integration
description: >-
  Wire Angular frontend to Spring Boot backend for Restaurant/Property-style apps —
  API config, PageResponse, repository search fixes, no demo fallbacks, smoke tests,
  readiness checklist. Use when connecting APIs, fixing 500s on list endpoints,
  verifying full stack, or assessing project readiness %.
---

# RMS Full-Stack Integration

Reference project: `resturant system/restaurant-management-system`

## Ports & config (must align)

| Layer | Default |
|-------|---------|
| Backend | `8083`, context `/api/v1` |
| Frontend dev | `4501` |
| `runtime-config.js` | `window.__RMS_API_URL__ = 'http://localhost:8083/api/v1'` |
| `environment.ts` | same URL as fallback |

`run-backend.ps1` writes `runtime-config.js`. `run-frontend.ps1` reads launcher state.

CORS must include frontend port (`CorsConfig.java`).

## ApiService chain

```
runtime-config.js → AppConstants.API.baseURL → ApiService
```

All domain services use path constants in `app-constants.ts`, not hardcoded URLs.

## No demo fallbacks (production rule)

**Remove** `DEMO_*` constants and fake data on API error/empty.

```typescript
// ✅ Correct
this.svc.getAll().subscribe({
  next: (r) => { this.rows = r.data?.content ?? []; },
  error: () => { this.rows = []; }
});

// ❌ Wrong
error: () => { this.rows = DEMO_ROWS; }
```

Empty UI: `empty-state` + `COMMON.NO_DATA` in template.

Exception: QR menu alias `demo` → real seed code `QR-T01` (not fake menu data).

## Backend pagination (PageResponse)

Spring `Page<>` inside `ApiResponse` works but prefer explicit DTO:

```java
// shared/response/PageResponse.java
public static <T> PageResponse<T> from(Page<T> page) { ... }
```

Controllers return `ApiResponse<PageResponse<T>>` for list endpoints.

## Repository search bug (null `q` param)

**Broken pattern** — JPQL `:q IS NULL OR ... CONCAT('%', :q, '%')` fails when `q` omitted (500 on PostgreSQL).

**Fix** — split in service:

```java
if (q == null || q.isBlank()) {
  return repo.findAll(pageable);
}
return repo.searchFiltered(q.trim(), pageable);
```

Apply to `CustomerRepository`, `UserRepository`, and similar search endpoints.

## Notifications module (if frontend calls `/notifications`)

Backend needs: entity + `V6__notifications.sql` + `NotificationController`:

- `GET /notifications`
- `GET /notifications/unread-count`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/read-all`

Frontend `NotificationService` falls back to localStorage only on API error.

## ReportService date params

Backend requires explicit params:

| Endpoint | Required |
|----------|----------|
| `/reports/sales/daily` | `branchId`, `date` |
| `/reports/sales/monthly` | `branchId`, `year`, `month` |
| `/reports/top-items` | `branchId`, `from`, `to` |
| `/reports/branch-performance` | `from`, `to` |

Frontend `ReportService` must default `from`/`to`/`date` via `defaultRange()` helper.

## Dashboard revenue chart

Load 12 months via `forkJoin` + `getMonthlySales(branchId, year, month)` — never hardcode chart values.

## API smoke test

Script: `restaurant-management-system/scripts/api-integration-test.mjs`

```bash
node scripts/api-integration-test.mjs
# or from frontend:
npm run test:api
```

Tests login + all major GET endpoints. Expect 24/24 pass before declaring "connected".

## Visual audit (screens)

```bash
node scripts/visual-audit-all.mjs restaurant
```

17 routes → `restaurant-frontend/ui-review/*.png`

## Readiness % template

| Area | Typical RMS score |
|------|-------------------|
| Core features + API wired | 95% |
| UI shell (premium + estate cards) | 92% |
| Automated tests (smoke only) | 35% |
| Production hardening | 70% |
| **Overall dev/demo ready** | **~87%** |
| **Production deploy** | **~72%** |

## Integration checklist

- [ ] `environment.ts` port = backend port
- [ ] No `DEMO_*` fallbacks in feature components
- [ ] List endpoints work without `q` param
- [ ] `PageResponse` on paginated APIs
- [ ] Dates: `rms-dates-property-style` skill applied
- [ ] `npm run build` passes
- [ ] `npm run test:api` 24/24
- [ ] Visual audit all admin routes OK

## Login (dev seed)

- User: `admin`
- Password: `Dev@Local2026!` (after `V5__strengthen_dev_passwords.sql`)
