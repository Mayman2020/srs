---
name: estate-excel-export-toolbar
description: >-
  Property Excel export toolbar — app-table-export-toolbar with ExportColumn
  map, permissionKey export gate, optional loadRows for full dump, inline mode.
  Use when adding Excel export to list headers or finance/HR workspaces.
---

# Estate Excel Export Toolbar

**Reference:** `Property_Managments/property-frontend/src/app/shared/components/table-export-toolbar/table-export-toolbar.component.ts`  

Combine with: `estate-settings-security-roles` (export permission), `estate-card-system`, `estate-page-header-actions`, `estate-table-list-toolbar`.

**Placement:** put `[inline]="true"` **inside** the same flex toolbar row as search/filters — never as a lone block under the toolbar (ERP stacking bug).

## Usage

```html
<app-table-export-toolbar
  [inline]="true"
  [title]="'TENANTS.TITLE' | translate"
  fileName="tenants"
  permissionKey="tenants"
  [columns]="exportColumns"
  [rows]="rows"
  [loadRows]="loadAllForExport">
</app-table-export-toolbar>
```

```ts
exportColumns: ExportColumn<Tenant>[] = [
  { header: 'Name', value: 'fullName' },
  { header: 'Phone', value: (r) => r.phone },
];
```

## Behavior

1. Renders stroked **Excel** button with `table_view` icon.
2. Visible only if `permissionKey` unset **or** `permissions.can(key, 'export')`.
3. `loadRows?: () => Promise<T[]>` exports **full** dataset (beyond current page); else uses `rows`.
4. Filename = `{safeName}-{yyyy-mm-dd}.xlsx`.
5. Tooltip from `COMMON.EXPORT_EXCEL`.
6. `inline` drops vertical padding when sitting in a header/action row.

## Rules

1. Reuse component — no ad-hoc SheetJS snippets in feature TS.
2. Column headers should be translated before pass (or map i18n in parent).
3. Disable while loading / when no rows and no `loadRows`.
4. Gate with real `export` permission action on the resource.

## Do not

- Export only current page when product expects full filter result (use `loadRows`)
- Show export without permission check on sensitive lists
