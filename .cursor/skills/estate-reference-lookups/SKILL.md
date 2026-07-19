---
name: estate-reference-lookups
description: >-
  Property/Clinic reference lookups — /admin/lookups admin, type+code bilingual
  rows, locked seeds, by-type API + cache, forms must use lookups not static
  lists. Use when replacing hardcoded enums/selects or porting lookup management.
---

# Estate Reference Lookups

**Rule:** Domain options (types, statuses, methods, job titles…) live in **DB lookups** and are edited from `/admin/lookups`. Forms and filters **never** hardcode static option arrays in TypeScript/HTML.

**Reference:**  
- Property FE: `features/lookups/`, `core/services/lookup.service.ts`, `lookup-cache.service.ts`  
- Property BE: `modules/lookup/`  
- Clinic FE (simpler tabs + `lookup-select`): `features/lookups/`, `shared/components/lookup-select/`

Combine with: `estate-settings-security-roles` (route + permissions + Flyway), `estate-lov-picker-system` (entity FK pickers — different job), `estate-i18n-rtl-parity`, `estate-excel-export-toolbar`, `estate-srs-table-pager`.

## Not the same as LOV picker

| Concern | Skill |
|---------|--------|
| Reference classifications (`UNIT_TYPE`, `PAYMENT_METHOD`, …) | **This skill** |
| Picking a property / unit / tenant / employee row | **estate-lov-picker-system** |

Short classification lists may use `mat-select` / `app-lookup-select`. Long entity lists must use searchable LOV.

## Data model

| Field | Role |
|-------|------|
| `type` | Bucket key (`LookupType` enum) |
| `code` | Stable value stored on business rows (prefer over numeric id in FKs-as-string) |
| `nameAr` / `nameEn` | Display labels |
| `sortOrder` | Admin + select order |
| `active` | Hide from runtime `by-type` when false |
| `locked` | Seeded system rows — **no delete** (edit labels/order OK per product) |
| `parentId` | Hierarchy (country → city / governorate → city) |

API split (Property):

- `GET /lookups/by-type?type=` — **active-only** for forms/lists  
- `GET /lookups/classifications?type=` — **all** (admin screen; SUPER_ADMIN)  
- Geo: `/lookups/countries`, `/lookups/cities?countryId=`  
- Admin mutate: POST classifications/countries/cities, PUT/DELETE `/{id}`

## Admin screen `/admin/lookups`

### Layout patterns

| Style | When |
|-------|------|
| **Property** | Tabs/sections (location, property, contract, status, finance, HR, inventory) + expansion lists + local pager (`pageSize` 5) + Excel export |
| **Clinic / vertical** | One `mat-tab` per `LookupType` + table + add/edit dialog |

### Behaviors

1. Page header + refresh; gate with `lookups` / `settings` permission (or SUPER_ADMIN).  
2. Add/edit via dialog: code (create), nameAr, nameEn, sortOrder, active.  
3. Delete blocked when `locked`; confirm named delete otherwise.  
4. After save/delete → reload that type only (not full page).  
5. Location tab: parent filter for cities (governorate/country).  
6. i18n: `LOOKUPS.*`, `LOOKUPS.LIST_*` / `LOOKUPS.TAB_*`, `NAV.LOOKUPS`.

### Seed policy

- Flyway seeds **locked** rows for codes the backend/workflows require.  
- Admins add optional codes later; never rely on a frontend-only enum list mirroring seeds forever without DB.

## Runtime: kill static lists

### Forbidden

```ts
// ❌ static domain lists in components
readonly unitTypes = ['APARTMENT', 'VILLA', 'SHOP'];
paymentMethods = [{ value: 'CASH', label: 'Cash' }, …];
```

```html
<!-- ❌ hard-coded options -->
<mat-option value="CASH">Cash</mat-option>
```

### Required

1. Load via `LookupService.getByType(type)` or `LookupCacheService.preload(...)`.  
2. Bind form control to **`code`** (default).  
3. Display `nameAr`/`nameEn` from current lang (fallback other lang → code).  
4. Filter inactive out client-side only if using admin payload; prefer `by-type`.

Clinic-style CVA:

```html
<app-lookup-select
  lookupType="PAYMENT_METHOD"
  labelKey="FIELD.PAYMENT_METHOD"
  formControlName="paymentMethod"
  valueField="code">
</app-lookup-select>
```

Property-style (inline / searchable-select fed by lookups):

```ts
this.lookupSvc.getByType('UNIT_TYPE').subscribe(res => {
  this.unitTypes = res.data ?? [];
});
// options: { value: item.code, label: localized name }
```

Label resolution in grids/details:

```ts
lookupCache.preload('CONTRACT_STATUS').subscribe(() => {
  this.statusLabel = this.lookupCache.label('CONTRACT_STATUS', row.status);
});
```

Invalidate or avoid stale cache after admin edits in the same session (reload type, or clear cache map).

## New lookup type checklist

1. Add enum value BE + FE `LookupType`.  
2. Flyway seed rows (`is_locked` for required codes).  
3. Register tab/list on lookup-management.  
4. Replace any static select with `by-type` / `lookup-select`.  
5. Optional: map codes to CSS `data-status` via `estate-status-data-badges` (codes stay lookup-driven).  
6. `ar.json` / `en.json` list/tab keys.

## Do not

- Ship new screens with hardcoded Arabic/English option arrays  
- Delete locked seeds from UI  
- Store display names on business tables instead of `code` (except denormalized search columns by design)  
- Confuse reference lookups with entity LOVs (properties/units)
