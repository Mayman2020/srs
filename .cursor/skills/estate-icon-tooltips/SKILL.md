---
name: estate-icon-tooltips
description: >-
  Property-style icon tooltips — matTooltip (Material apps) or ngbTooltip (ERP)
  on every icon-only control; rms-icon-btn with tooltipKey; i18n keys not native
  title. Use when porting toolbar/row/topbar icon affordances across RMS apps.
---

# Estate Icon Tooltips

**Reference:** `Property_Managments/property-frontend` — `app-icon-btn` + `[matTooltip]="'ACTIONS.EDIT' | translate"`  
Combine with: `estate-table-row-actions`, `estate-table-list-toolbar`, `estate-topbar-user-chrome`, `rms-premium-shell`.

## Rule (mandatory)

Every **icon-only** control MUST expose a hover tooltip **and** `aria-label`:

| Stack | Tooltip | Component |
|-------|---------|-----------|
| Angular Material (Clinic, POS, RMS, Vzeeta, Mazaad, AromaFlow, SRS) | `[matTooltip]="key \| translate"` | `rms-icon-btn` or `app-icon-btn` |
| ERP (ng-bootstrap) | `[ngbTooltip]="key \| translate"` placement="top" container="body" | `erp-icon-button` |

**Never** use native `title` / `[attr.title]` for UX icons — it ignores Material theme, RTL overlay, and language switch.

## `rms-icon-btn` (canonical toolbar / header)

Copy from `Clinic System/clinic-frontend/src/app/shared/components/rms-icon-btn/`.

```html
<rms-icon-btn icon="add" tooltipKey="COMMON.ADD" variant="primary" (clicked)="onCreate()"></rms-icon-btn>
<rms-icon-btn icon="refresh" tooltipKey="COMMON.REFRESH" (clicked)="load()"></rms-icon-btn>
<rms-icon-btn icon="search" tooltipKey="COMMON.SEARCH" variant="primary" (clicked)="onSearch()"></rms-icon-btn>
```

Variants: `default` | `primary` | `gold` | `warn` | `danger`  
Always set `tooltipKey` (or `tooltip` for dynamic text). `matTooltipPosition="below"`, show delay 200ms.

## Table row actions

```html
<td class="action-cell">
  <div class="row-actions">
    <button type="button" class="app-icon-btn"
      [matTooltip]="'COMMON.EDIT' | translate"
      [attr.aria-label]="'COMMON.EDIT' | translate"
      (click)="onEdit(row)">
      <span class="material-icons">edit</span>
    </button>
    <button type="button" class="app-icon-btn danger"
      [matTooltip]="'COMMON.DELETE' | translate"
      [attr.aria-label]="'COMMON.DELETE' | translate"
      (click)="onDelete(row)">
      <span class="material-icons">delete</span>
    </button>
  </div>
</td>
```

`mat-icon-button` rows: same `[matTooltip]` + `[attr.aria-label]` on every button.

## Collapsed sidebar

```html
[matTooltip]="collapsed ? (item.labelKey | translate) : null"
[matTooltipDisabled]="!collapsed"
matTooltipPosition="before"
[matTooltipShowDelay]="200"
```

## ERP `erp-icon-button`

```html
<button type="button" class="erp-icon-button"
  [ngbTooltip]="'COMMON.REFRESH' | translate"
  placement="top" container="body"
  [attr.aria-label]="'COMMON.REFRESH' | translate"
  (click)="refresh()">
  <mat-icon>refresh</mat-icon>
</button>
```

`NgbTooltipModule` is in `shared.module.ts` — no per-page import needed.

## i18n keys (prefer)

| Action | Key |
|--------|-----|
| Add | `COMMON.ADD` or domain `*.NEW` |
| Edit | `COMMON.EDIT` / `ACTIONS.EDIT` |
| Delete | `COMMON.DELETE` / `ACTIONS.DELETE` |
| Refresh | `COMMON.REFRESH` |
| Search | `COMMON.SEARCH` |
| Back | `COMMON.BACK` |
| Toggle active | `USERS.DEACTIVATE` / `USERS.ACTIVATE` or `COMMON.ACTIVATE` |

## Rollout checklist

1. Port `rms-icon-btn` to app `shared/components/` if missing.
2. Replace `[title]` / `title="…"` / `[attr.title]` with `matTooltip` or `ngbTooltip`.
3. Add `MatTooltipModule` to standalone component `imports` when using inline `matTooltip`.
4. Audit lists: `rg "mat-icon-button|app-icon-btn|rms-icon-btn|erp-icon-button" src` — every hit needs tooltip.
5. Sidebar collapsed rail + topbar action buttons.

## Do not

- Hardcoded Arabic/English in `title="تعديل"`
- Icon-only buttons without `aria-label`
- Mix `title` and `matTooltip` on the same control
