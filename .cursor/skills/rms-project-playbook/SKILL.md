---
name: rms-project-playbook
description: >-
  Master playbook for Restaurant RMS / Property-style Angular 17 + Spring Boot 3
  projects — which skills to apply and in what order. Use when starting a new
  restaurant/estate admin app, auditing RMS UI+API completeness, or when the user
  asks to work like Property or RMS Restro.
---

# RMS Project Playbook

Reference implementations:  
- Restaurant: `resturant system/restaurant-management-system`  
- Clinic: `Clinic System/clinic-backend` + `clinic-frontend`  
- Also ported (skill 5 + nav back): `pos-cashier-system`, `aromaflow`, `Vzeeta Project`, `Mazaad App`

UI reference: `Property_Managments/property-frontend`

## Skills (apply in this order)

| # | Skill | When |
|---|-------|------|
| 1 | **rms-premium-shell** | Login split, sidebar, topbar, icon buttons, dialogs, smart back, contrast, full-width layout |
| 2 | **estate-card-system** | List pages, stat-pills, table-card, entity-card, floor map, toolbars |
| 2b | **rms-property-list-integration** | ListLoadController, PagedResponse, server search/pager, empty-state, route↔API audit, US docs |
| 3 | **rms-dates-property-style** | Datepicker dd/MM/yyyy, RmsDatePipe, no datetime-local |
| 4 | **rms-fullstack-integration** | API ports, PageResponse, no demo fallbacks, smoke test, readiness % |
| 5 | **estate-settings-security-roles** | JWT, RBAC matrix, users, settings, lookups, must-change-password, PostgreSQL migrations (+ satellites below for Property depth) |

## Property polish extensions (optional, after 1–5)

Extracted from `Property_Managments/property-frontend` — apply when the screen needs Property-level UI depth. **Do not** re-document shell/cards/lists/dates/API/RBAC here.

| Skill | When |
|-------|------|
| **estate-user-mgmt-admin** | `/admin/users` activate/deactivate toggle, dialog, no invent Ban |
| **estate-screen-module-visibility** | `/admin/screens` + `/admin/module-settings` triple-gate with RBAC |
| **estate-user-access-multi-role** | `/admin/user-access`, extraRoles, `X-Active-Role` switcher |
| **estate-reference-lookups** | `/admin/lookups` + forms via by-type/cache — no static option lists |
| **estate-os-design-tokens** | Brass/navy/paper tokens, Instrument Serif, dark CDK overlays, full-width tokens |
| **estate-i18n-rtl-parity** | Default AR, ar/en key parity, Latin digits, bilingual segments |
| **estate-lov-picker-system** | Searchable LOV dialog instead of long mat-select |
| **estate-status-data-badges** | `data-status` / priority chips matrix |
| **estate-table-row-actions** | Entity cell + `app-icon-btn` action columns |
| **estate-icon-tooltips** | `matTooltip` / `ngbTooltip` on every icon-only control; `rms-icon-btn` with `tooltipKey` |
| **estate-page-header-actions** | Title + Add/Refresh/Export in **one horizontal** `.page-actions` row; no stacked header buttons |
| **estate-delete-confirm-dialog** | MatDialog confirm before every delete/archive/deactivate; no `window.confirm()` |
| **estate-identity-media-uploads** | Profile + civil ID media fields / upload-zone |
| **estate-detail-page-hero** | Detail breadcrumb + hero chips + timeline panels |
| **estate-entity-audit-trail** | **Mandatory** admin strip معلومات التدقيق (created/modified/approved by+at) on edit dialogs & details |
| **estate-excel-export-toolbar** | Permission-gated Excel export button |
| **estate-in-app-notifications** | Bell badge poll, 14-day inbox tabs, mark-all, deep-link open |
| **estate-topbar-user-chrome** | Lang/theme/role/user menus + property scope + notify |
| **estate-sidebar-nav-sections** | Collapsible NAV_SECTION, glow, user footer, collapse toggle |
| **estate-my-profile-page** | Hero + details + password aside; reuse identity media |
| **rms-transactional-email** | Optional EmailService (Clinic ref) beside in-app notify; password-reset stays its own skill |
| **estate-dashboard-kpi-motion** | Admin dashboard KPI cards, SVG charts, kpi-link routes |
| **estate-kpi-drilldown-workspace** | Selectable KPI → paged drill table (contracts dashboard) |
| **estate-srs-table-pager** | app-table-pager chrome, server/client modes, page size 5 |
| **estate-workspace-filter-strip** | Status chip trays + workspace property strips |
| **estate-contract-lifecycle-workspace** | Contract detail hero/actions/banners/section tiles |
| **estate-owner-approval-inbox** | Owner portal badge-tab approval cards |
| **estate-dual-source-unified-list** | LEASE+MAINTENANCE merged contracts list |

## Quick start commands

```powershell
# Backend (port 8083)
cd restaurant-backend; .\run-backend.ps1

# Frontend (port 4501)
cd restaurant-frontend; .\run-frontend.ps1

# Verify API
npm run test:api          # from restaurant-frontend

# Screenshot all screens
node scripts/visual-audit-all.mjs restaurant
```

## Stack defaults

- Angular 17 standalone, Material, ngx-translate, Chart.js
- Spring Boot 3.2, Java 17, PostgreSQL, Flyway schema per app
- JWT auth, `@RequiresPermission` AOP
- Navy `#1e3a5f` + gold `#d4a853` brand

## Definition of done (dev-ready ~87%)

- [x] All 5 skills applied — **POS, AromaFlow, Vzeeta, Mazaad** (+ Clinic, Restaurant)
- [ ] `ng build` + API smoke 24/24 — run per app: `npm run test:api` (backend must be running)
- [ ] Visual audit 17 screens OK
- [ ] No `DEMO_*` in feature components — Mazaad catalog fallback removed; POS demo images dev-gated
- [ ] `environment.ts` = `runtime-config.js` = backend port
- [x] `/admin/users`, `/admin/permissions`, `/admin/lookups` wired (or app-specific routes)

## Per-app ports (smoke test)

| App | Backend | Frontend | test:api |
|-----|---------|----------|----------|
| Restaurant | 8083 | 4501 | `restaurant-frontend` |
| Clinic | 8086 | 4200 | `clinic-frontend` |
| POS Cashier | 8082 | 4200 | `pos-frontend` |
| AromaFlow | 8082 | varies | `aromaflow-frontend` |
| Vzeeta | 8081 | 4200 | `vzeeta-web` |
| Mazaad | 8081 | 4200 | `mazaad-frontend` |

## What remains for production (~72%)

- Unit/e2e tests
- Billing UI, menu modifiers UI
- WebSocket kitchen (optional)
- HTTPS, CI/CD, secrets management
