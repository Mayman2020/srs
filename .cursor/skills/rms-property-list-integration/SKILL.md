---
name: rms-property-list-integration
description: >-
  Wire admin list pages end-to-end like Property_Managments — ListLoadController,
  PagedResponse, server search/pagination, app-empty-state, app-table-pager,
  route↔API verification, business gaps, and user-story test docs. Use after
  estate-card-system when fixing list API binding, filters, or audit parity with
  Property contract-list / user-management.
---

# Property-Style List Integration

Reference UI:
- **Stats + filters + pager:** `Property_Managments/property-frontend/src/app/features/contracts/contract-list/`
- **Directory variant:** `Property_Managments/property-frontend/src/app/features/users/user-management/`

Combine with: **estate-card-system** (SCSS/classes), **rms-fullstack-integration** (smoke test), **rms-dates-property-style** (table dates).

## Page skeleton (full stack)

```html
<div class="app-page">
  <app-page-header [showBack]="true" (backClick)="goBack()" ...></app-page-header>

  <div *ngIf="listLoad.showInitialSpinner" class="loading-wrap">
    <mat-spinner diameter="40"></mat-spinner>
  </div>

  <app-empty-state
    *ngIf="listLoad.showSurface && rows.length === 0 && !hasActiveFilters()"
    icon="..."
    [title]="'...' | translate"
    [message]="'...' | translate">
  </app-empty-state>

  <div class="app-list-surface" [class.is-refreshing]="listLoad.refreshing"
    *ngIf="listLoad.showSurface && (rows.length > 0 || hasActiveFilters())">
    <div class="list-refresh-spinner" *ngIf="listLoad.refreshing">
      <mat-spinner diameter="32"></mat-spinner>
    </div>

    <section class="list-stats">
      <article class="stat-pill">
        <span class="stat-label">{{ 'COMMON.ALL' | translate }}</span>
        <strong>{{ totalElements }}</strong>
      </article>
    </section>

    <section class="app-card table-card">
      <div class="estate-table-toolbar">
        <label class="estate-search-inline">...</label>
        <div class="estate-filter-tray">...</div>
      </div>
      <div class="app-table-wrap">
        <table class="app-data-table source-table">...</table>
      </div>
      <app-table-pager
        [length]="totalElements"
        [pageSize]="pageSize"
        [pageIndex]="pageIndex"
        (pageIndexChange)="onPageChange($event)"/>
    </section>
  </div>
</div>
```

## TypeScript flow

```typescript
import { ListLoadController } from '../../shared/utils/list-load.util';
import { withPageParams } from '../../core/utils/pagination.util';

listLoad = new ListLoadController();
pageIndex = 0;
pageSize = 10;
totalElements = 0;
rows: Row[] = [];
searchTerm = '';
statusFilter = '';

load(): void {
  this.listLoad.begin();
  this.service.getAll(
    withPageParams(this.pageIndex, this.pageSize, {
      q: this.searchTerm,
      status: this.statusFilter
    })
  ).subscribe({
    next: (res) => {
      this.rows = res.data?.content ?? [];
      this.totalElements = res.data?.totalElements ?? 0;
      this.listLoad.end();
    },
    error: () => {
      this.rows = [];
      this.totalElements = 0;
      this.listLoad.end();
    }
  });
}

onPageChange(i: number): void {
  this.pageIndex = i;
  this.load();
}

hasActiveFilters(): boolean {
  return !!(this.searchTerm?.trim() || this.statusFilter);
}
```

## Shared utilities (port from Property)

| File | Purpose |
|------|---------|
| `shared/utils/list-load.util.ts` | `ListLoadController` — initial spinner vs soft refresh |
| `core/utils/pagination.util.ts` | `withPageParams`, `DEFAULT_TABLE_PAGE_SIZE` |
| `shared/components/table-pager/` | Server-side pager UI |
| `shared/components/empty-state/` | Empty list (no DEMO fallback) |
| `shared/pipes/table-row-index.pipe.ts` | Row # column |

## API contract

### Frontend

```typescript
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
```

### Backend (Spring Boot)

- `GET /entities?page=0&size=10&sort=createdAt,desc&q=...&status=...`
- Return `Page<T>` wrapped in `ApiResponse`
- Search: `@RequestParam(required = false) String q` on repository query
- **Never** client-filter paged results (audit logs, branches at scale)

### Service layer

```typescript
getAll(params: Record<string, string | number | boolean>): Observable<ApiResponse<PagedResponse<T>>> {
  return this.http.get<ApiResponse<PagedResponse<T>>>(`${base}/entities`, { params });
}
```

## Integration verification checklist

For each list route:

- [ ] Route in `admin.routes.ts` (or portal routes) with `permissionGuard`
- [ ] Frontend service passes `page`, `size`, `q`, filter params
- [ ] Backend controller accepts same params; repository implements search
- [ ] Response shape: `content` + `totalElements` (not flat `List`)
- [ ] `ListLoadController` + `is-refreshing` overlay
- [ ] `app-empty-state` when empty and no filters
- [ ] `app-table-pager` bound to `totalElements`
- [ ] stat-pill counts from API (`totalElements` or status breakdown endpoint)
- [ ] Dates: `rmsDate` pipe
- [ ] Smoke test includes endpoint + optional `?q=` variant
- [ ] No `DEMO_*` fallback on error

## Business QA workflow

1. **Document gaps** — `docs/business-gaps-ar.md` (Business B* + Technical T*)
2. **User stories** — `docs/user-stories-full-system-ar.md` (Epic → US → Route → API → Given/When/Then)
3. **Fix gaps** before test run
4. **Run tests:**
   - `npm run test:api` (backend must be running)
   - Manual US checklist per list
   - Optional: `node scripts/visual-audit-all.mjs {app}`
5. **Results** — `docs/user-stories-test-results-ar.md`
6. **Final docs** — `CODE-INTEGRATION-ar.md` + `BUSINESS-SPEC-ar.md`

Reference doc template: `Property_Managments/docs/user-stories-full-system-ar.md`

## Do not

- Client-side filter on server-paged data (misleading totals)
- Hardcode `size=50` without pager UI
- `unwrapPage()` that drops `totalElements`
- Inline `<p class="empty-state">` instead of `app-empty-state`
- Smoke test paths that don't match controllers
- stat-pill counts from `rows.length` when server-paged (use `totalElements`)

## Per-app notes

| App | Port | List doc path |
|-----|------|---------------|
| Clinic | 8086 | `Clinic System/docs/` |
| Vzeeta | 8081 | `Vzeeta Project/docs/` (4 portals) |
| Property | 8089 | Reference only |
