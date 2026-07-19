---
name: estate-srs-table-pager
description: >-
  Property SRS table pager — app-table-pager chrome, ellipsis window, server vs
  client mode, DEFAULT_TABLE_PAGE_SIZE=5, tableRowIndex, nested section pages.
  Use whenever adding estate-style pagination (beyond ListLoadController wire-up).
---

# Estate SRS Table Pager

**Reference:**  
- `shared/components/table-pager/table-pager.component.ts` → `app-table-pager`  
- `core/utils/pagination.util.ts` → `paginatedSlice`, `withPageParams`, `DEFAULT_TABLE_PAGE_SIZE`  
- Global SCSS: `.srs-pager-*` in `styles.scss`

List skill covers *when* to page server-side; this skill is the **pager chrome + dual modes**.

## Component API

```html
<app-table-pager
  [length]="totalElements"
  [pageSize]="pageSize"
  [pageIndex]="pageIndex"
  (pageIndexChange)="onPage($event)">
</app-table-pager>
```

- Hide when `length === 0`.
- Navy active page; ellipsis algorithm (≤7 pages full; else windowed with `…`).
- Default `pageSize` = **5** (`DEFAULT_TABLE_PAGE_SIZE`).

## Server vs client

| Mode | When | Binding |
|------|------|---------|
| **Server** | Large domain lists | `pageIndexChange` → API `page`/`size` via `withPageParams`; `[length]="totalElements"` |
| **Client** | Drilldowns, approval tabs, nested detail sections | Keep full array; `paged = paginatedSlice(all, pageIndex, pageSize)` |

## Nested sections

Contract detail schedule / annexes / logs / history → **each** owns its own `pageIndex` (+ optional pageSize). Do not share one pager state across sections.

## Row index

```html
{{ i | tableRowIndex:pageIndex:pageSize }}
```

## Do not

- Drop Material paginator as Estate default (use `app-table-pager`)
- Client-filter *after* server paging without documenting merge caveats
- Show pager chrome on empty lists
