---
name: estate-dashboard-kpi-motion
description: >-
  Property admin dashboard — estate-stat-card KPI grid with tones, SVG charts
  (area/donut), kpi-link drill routes, ListLoadController refresh, property
  scope LOV. Use when building professional dashboards like Property.
---

# Estate Dashboard KPI Motion

**Reference:** `Property_Managments/property-frontend`  
- `features/dashboard/dashboard/`  
- `styles/pages/dashboard.page.scss`  
- `core/services/dashboard.service.ts`

Combine with: `estate-card-system` (list `stat-pill` ≠ these cards), `estate-lov-picker-system`, `rms-property-list-integration` (`ListLoadController`).

## Composition

```
page-header (+ optional property LOV showAll)
estate-stat-grid
  estate-stat-card.[navy|teal|gold|danger|purple] (+ .kpi-link)
charts / progress panels (permission-gated)
recent tables (optional)
```

## KPI cards

1. Value uses `--font-display` (~large, tabular nums).
2. Variants: `navy | teal | gold | danger | purple` — Estate tones, not generic purple SF pills.
3. Foot line = muted context or “view …” + `arrow_forward`.
4. Hover: slight `translateY(-2px)` + brass border via `--t-med` / `--e-out`.
5. Navigable cards get `.kpi-link` + `routerLink` and/or `queryParams` (e.g. pending maintenance).
6. Progress bars (occupancy) use token fill width `%`.

## Charts

Prefer **pure SVG** (area path, donut `stroke-dashoffset`) matching Property — avoid dropping Chart.js defaults unless the app already depends on it.

## Load / scope

- Soft refresh: `app-list-surface` + `listLoad.refreshing` spinner overlay.
- Property LOV in header scopes all dashboard API calls.
- Gate panels with permissions (`maintenance`, `finance`, `inventory`, …).
- Counts from live APIs only — never DEMO.

## Do not

- Confuse list-page `stat-pill` with dashboard `estate-stat-card`
- Non-clickable hero KPIs when the product expects drill routes
