---
name: estate-table-row-actions
description: >-
  Property table entity cells and row action icons — app-table-entity-cell,
  app-icon-btn variants, table-actions/row-actions layout, permission-gated
  icon tooltips. Use when building data tables or porting Property list rows.
---

# Estate Table Row Actions

**Reference:** `Property_Managments/property-frontend`  
- `shared/components/table-entity-cell/table-entity-cell.component.ts`  
- `styles.scss` → `.app-icon-btn`, `.table-actions`, `.row-actions`  
Combine with: `estate-card-system`, `estate-icon-tooltips`, `estate-settings-security-roles` (`*appHasPermission`).

## Primary entity column

```html
<app-table-entity-cell
  [title]="row.fullName"
  [subtitle]="row.code"
  [imageUrl]="row.avatarUrl"
  [initial]="row.fullName | initials"
  icon="person">
</app-table-entity-cell>
```

Avatar **or** initial **or** icon + title/subtitle. One component for people, properties, contractors.

## Action column

```html
<td class="table-actions">
  <div class="row-actions">
    <button type="button" class="app-icon-btn" matTooltip="…" *appHasPermission="'tenants';'view'" (click)="view(row)">
      <span class="material-icons">visibility</span>
    </button>
    <button type="button" class="app-icon-btn" …>edit</button>
    <button type="button" class="app-icon-btn accent" …>vpn_key</button>
    <button type="button" class="app-icon-btn danger" …>delete</button>
  </div>
</td>
```

### `app-icon-btn` variants

| Class | Meaning |
|-------|---------|
| (default) | Neutral view/edit |
| `accent` | Secondary highlight (reset password, soft promote) |
| `success` | Positive confirm |
| `warning` | Caution |
| `danger` | Delete / destructive |

36px hit target; prefer **icon + tooltip** over text buttons in dense tables.

## Layout rules

1. `.table-actions` / `.row-actions` → flex, centered, nowrap.
2. Align Material `mat-icon-button` hit size with `app-icon-btn` when mixed.
3. Max ~4 icons; overflow to a `mat-menu` if more.
4. Gate every mutating action with permission directive / `cannot`.
5. Row index via pager-aware pipe (`table-row-index` + offset) when shown.

## Do not

- Text-only “Edit / Delete” links jammed into narrow columns
- Ungated destructive actions
- Different icon styles per list page
