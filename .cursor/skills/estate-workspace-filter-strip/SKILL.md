---
name: estate-workspace-filter-strip
description: >-
  Property filter trays — status chip buttons under toolbar, finance-filter-strip
  property LOV, FilterBar densify, clear-filters resets page. Use for contracts
  list chips and multi-section workspaces.
---

# Estate Workspace Filter Strip

**Reference:**  
- Contracts list: `features/contracts/contract-list/` (`.estate-filter-tray` / `.estate-filter-btn`)  
- Shared: `shared/components/filter-bar/filter-bar.component.ts`  
- Workspaces: `finance-workspace`, `hr-workspace`, `vacancy-workspace`

Combine with: `estate-lov-picker-system`, `estate-card-system` (toolbar), `rms-property-list-integration`.

## Layers

1. **Toolbar row:** search + LOVs (`app-estate-lov-select`) + clear icon when active.  
2. **Chip tray under toolbar:** enum statuses as `.estate-filter-btn` (ACTIVE/DRAFT/…); active = navy/brass.  
3. **Workspace strip:** horizontal `.finance-filter-strip` = label + property LOV `showAll` only.  
4. **Dense FilterBar:** `app-filter-bar` + `FilterSpec` (`text|number|select|lov`) for complex directories.

## Rules

1. LOV status filter and chip tray share the **same** `filterStatus` model (stay in sync).
2. Any filter/search change → `pageIndex = 0`.
3. Clear-filters button only when `hasActiveFilters()`.
4. Match Material field density to toolbar (~40px) via global filter-bar densify.
5. Semantic chip modifiers (`fs-active`, `fs-draft`, …) optional for color accent.

## Do not

- Put status chips *outside* the `table-card` / list surface
- Duplicate LOV dialog logic — import LOV skill
- Forget clearing page index on filter change
