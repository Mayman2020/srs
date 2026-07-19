---
name: estate-kpi-drilldown-workspace
description: >-
  Property contracts-dashboard pattern — selectable KPI cards that drive a
  paged drilldown table, column headers by card, quick-action tiles. Use for
  domain dashboards where KPI click reveals related rows.
---

# Estate KPI Drilldown Workspace

**Reference:** `Property_Managments/property-frontend/src/app/features/contracts/contracts-dashboard/`

Combine with: `estate-dashboard-kpi-motion` (visual KPIs), `estate-srs-table-pager`, `estate-card-system`.

## Pattern

```
page-header (+ create CTA)
estate-stat-grid  → cards are SELECTABLE (not only routerLink)
dashboard-drilldown table-card  → rows for selected KPI
quick-actions tiles  → view-all / open related screens
```

## Rules

1. Each card has `key`, `variant`, `icon`, `labelKey`, `value`; click → `selectCard` sets `selectedCardKey` + builds `DrillRow[]`.
2. Selected card gets `.selected` chrome; tooltip “click to view list”.
3. Normalize rows: `{ ref, primary, secondary, meta, detailRoute }` — column 2–4 headers switch by card key.
4. Client-page drill with `app-table-pager` + small `drillPageSize` (e.g. 5); reset `drillPageIndex` when card changes.
5. Aggregate with `forkJoin` across APIs; prefer dedicated dashboard stats endpoint, fall back to filtered lists.
6. Empty drill → `app-empty-state` inside the drill card; end page with **quick-action tiles** (not more KPIs).

## Do not

- Navigate away on every KPI click when in-page drill is the UX
- Reuse admin SVG chart skill for this — charts optional; drill table is the point
