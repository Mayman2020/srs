---
name: estate-lov-picker-system
description: >-
  Property LOV searchable-select pattern — readonly field opens dialog picker
  (toolbar vs form variants), clearable, server-side search, estate-lov-select
  with showAll. Use when building filters, form FK pickers, or replacing long
  mat-select / native select lists.
---

# Estate LOV Picker System

**Reference:** `Property_Managments/property-frontend/src/app/shared/components/`  
- `searchable-select/searchable-select.component.ts` → `app-searchable-select`  
- `lov-select-dialog/lov-select-dialog.component.ts`  
- `estate-lov-select/estate-lov-select.component.ts` → toolbar wrapper  
- Used in topbar property scope, filter bars, dialogs

Combine with: `estate-card-system` (toolbar placement), `estate-i18n-rtl-parity` (`LOV.*` keys).

## Rule

**Never** use native `<select>` or long scrolling `mat-select` for domain LOVs (properties, units, tenants, employees, lookups…).

Use readonly text field → opens Material dialog with searchable rows.

## Variants

| Variant | Height / look | Where |
|---------|---------------|--------|
| `toolbar` | ~40px, compact | `estate-table-toolbar`, filter strips |
| `form` | ~42px+, labeled | Dialogs / create-edit forms |

```html
<app-searchable-select
  variant="form"
  [label]="'FIELD.PROPERTY' | translate"
  [options]="propertyOptions"
  formControlName="propertyId"
  [clearable]="true"
  [required]="true">
</app-searchable-select>
```

Toolbar convenience wrapper:

```html
<app-estate-lov-select
  [options]="properties"
  [(ngModel)]="propertyId"
  [showAll]="true"
  [allLabelKey]="'COMMON.ALL'">
</app-estate-lov-select>
```

## Dialog behavior

1. Search input (`LOV.SEARCH_PLACEHOLDER`) filters client list **or** server-side with debounce when `serverSide=true`.
2. Rows show label (+ optional subtitle); selected row has check icon.
3. Confirm/select closes dialog and writes value via CVA (`NG_VALUE_ACCESSOR`).
4. Clear (X) only when `clearable` and value present.
5. Search button + click/Enter on field open picker.

## i18n keys

| Key | Purpose |
|-----|---------|
| `LOV.SELECT_PLACEHOLDER` | Empty field |
| `LOV.SEARCH_PLACEHOLDER` | Dialog search |
| `LOV.CLEAR` / `LOV.OPEN` | Aria labels |
| `COMMON.REQUIRED` | Required error |

## Do not

- Inline 50+ options in `mat-select`
- Build one-off picker dialogs per screen — reuse `app-searchable-select`
- Forget CVA (`formControlName` / `ngModel` must work)
