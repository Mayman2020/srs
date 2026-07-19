---
name: estate-page-header-actions
description: >-
  Property page-header composition — title/subtitle left, primary actions
  (Add / Refresh / Export) in one horizontal .page-actions row; never stack
  buttons vertically. Use when fixing ERP/RMS list headers that wrap or stack.
---

# Estate Page Header Actions

**Reference:** `Property_Managments/property-frontend`  
- `shared/components/page-header/page-header.component.ts` → `.page-actions`  
- Users: `features/users/user-management/user-management.component.html`  
Combine with: `estate-table-list-toolbar`, `estate-excel-export-toolbar`, `estate-icon-tooltips`.

## Composition (mandatory)

```
┌─────────────────────────────────────────────────────────────┐
│  Eyebrow / Title / Subtitle          [↻] [+ Add] [Excel]   │  ← page-header ONE row
├─────────────────────────────────────────────────────────────┤
│  [🔍 Search…………] [filter] [clear]              [Excel?]   │  ← table toolbar ONE row
├─────────────────────────────────────────────────────────────┤
│  table…                                                      │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Contains | Must NOT contain |
|-------|----------|------------------|
| `app-page-header` → `.page-actions` | Add, Refresh, optional primary CTAs | Search field, long filters |
| `.estate-table-toolbar` / `.erp-table-toolbar` / `.erp-admin-toolbar__actions` | Search, filters, **one** export | Duplicate Add already in header |

## Rules

1. **Horizontal only** — `.page-actions` / `[header-actions]` / toolbar `__actions` = `display: flex; align-items: center; gap: 8–10px; flex-wrap: wrap`.
2. **One Add** — either in page-header **or** section toolbar, never both stacked as separate blocks.
3. **One Export** — either `app-table-export-toolbar [inline]` **inside** the same flex row as search, **or** data-table `exportable` — never both.
4. **No orphan export row** — never place `app-table-export-toolbar` as a sibling block under the toolbar (causes vertical stacking).
5. **ERP** — always wrap projected actions: `<div header-actions class="erp-toolbar-icons">…</div>`.
6. **Title size** — keep page title ~20–26px (Property density). Giant display titles (`clamp(28px, 4vw, 42px)`) force actions onto a second line.

## ERP admin section toolbar

```scss
.erp-admin-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.erp-admin-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.erp-admin-inline-input {
  min-height: 40px;
  width: min(260px, 100%);
}
```

```html
<div class="erp-admin-toolbar">
  <div class="erp-admin-toolbar__copy">
    <h3>{{ '…TITLE' | translate }}</h3>
    <p>{{ '…HINT' | translate }}</p>
  </div>
  <div class="erp-admin-toolbar__actions">
    <input class="erp-input erp-admin-inline-input" … />
    <app-table-export-toolbar [inline]="true" …></app-table-export-toolbar>
    <button type="button" class="erp-button erp-button--primary" …>Add</button>
  </div>
</div>
<app-data-table [exportable]="false" [filterable]="false" …></app-data-table>
```

## Do not

- Stack Add above Export as separate block siblings
- Duplicate search (admin input + data-table `filterable`) on the same list
- `flex-direction: column` on page-header except very small phones
- Native `title` on icon actions — use `matTooltip` / `ngbTooltip`
