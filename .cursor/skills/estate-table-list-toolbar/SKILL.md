---
name: estate-table-list-toolbar
description: >-
  Property table toolbar above lists — directory-search, 40px filter/date fields,
  estate-filter-select, status chips, rms-icon-btn actions, export inline. Use when
  building or fixing filters above data tables across RMS/Estate apps.
---

# Estate Table List Toolbar

**Reference:** `Property_Managments/property-frontend/src/styles.scss` (`.directory-toolbar`, `.directory-search`, `.estate-filter-btn`)  
**Canonical SCSS:** `my-skills/estate-table-list-toolbar/estate-table-toolbar.scss`  
Combine with: `estate-card-system`, `estate-icon-tooltips`, `estate-workspace-filter-strip`, `rms-dates-property-style`, `estate-excel-export-toolbar`.

## DOM skeleton (inside `app-card table-card` / `estate-card directory-table-card`)

```html
<div class="estate-table-toolbar directory-toolbar">
  <div class="directory-toolbar-top table-list-toolbar">
    <div class="directory-search">
      <span class="material-icons">search</span>
      <input [(ngModel)]="search" (keyup.enter)="onSearch()" [placeholder]="'COMMON.SEARCH' | translate">
    </div>
    <!-- optional mat-form-field date / select with class filter-field -->
    <select class="estate-filter-select" [(ngModel)]="statusFilter">...</select>
    <rms-icon-btn icon="search" tooltipKey="COMMON.SEARCH" variant="primary" (clicked)="onSearch()"></rms-icon-btn>
    <app-table-export-toolbar [inline]="true" ...></app-table-export-toolbar>
  </div>
  <div class="estate-filter-tray" *ngIf="showChips">
    <div class="estate-filter-row">
      <button type="button" class="estate-filter-btn" [class.active]="!filter" (click)="setFilter('')">...</button>
    </div>
  </div>
</div>
```

Page header actions (add/refresh) stay in `app-page-header` — **not** mixed into table toolbar.  
See **`estate-page-header-actions`**: one horizontal `.page-actions` row; never stack Add/Export as sibling blocks; one export only (toolbar **or** table `exportable`, not both).

## Scale (mandatory — Property)

| Control | Size |
|---------|------|
| `.directory-search` | **40px** height, `flex: 0 1 260px`, `min-width: 180px` |
| `.estate-search-inline` | **40px** min-height, `max-width: 420px`, radius **12px** |
| `.estate-filter-select` | **40px** height, radius **10px**, `font-size: 13px` |
| Toolbar `mat-form-field` / `.filter-field` | **40px** wrapper, hide subscript |
| Datepicker in toolbar | same **40px** density; use `rmsDate` pipe in table cells |
| Toolbar icon buttons | **`rms-icon-btn`** **36×36** (primary search = `variant="primary"`) |
| `.clear-filters-btn` | **36×36** icon-only |
| `.estate-filter-btn` chips | `padding: 6px 12px`, radius **10px**, active = navy + brass text |
| Toolbar row gap | **8px**, `flex-wrap: wrap` |

## Import in each frontend

```scss
@import './styles/admin.filters'; // or scss/admin.filters for ERP
```

Copy/sync from `my-skills/estate-table-list-toolbar/estate-table-toolbar.scss` → `<app>/styles/admin.filters.scss`.

## Rules

1. Filters live **inside** `table-card` / `directory-table-card`, not floating above stats.
2. Filter/search change → reset `pageIndex` to 0.
3. No text `mat-button` in toolbar — `rms-icon-btn` + export toolbar.
4. Dates: Material Datepicker + `DateFormatAdapter`; display with `rmsDate` / `erpDate` / `srsDate`.
5. Use `COMMON.ALL` / `COMMON.ACTIVE` / `COMMON.INACTIVE` for native `<select>` status filters.

## Do not

- 46px+ search bars in toolbar (Restaurant legacy `estate-search-inline`)
- Full-width mat-form-fields breaking toolbar row on desktop
- `datetime-local` in filter rows
- Duplicate toolbar CSS per component — use shared `admin.filters.scss`
