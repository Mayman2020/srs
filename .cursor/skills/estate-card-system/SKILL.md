---
name: estate-card-system
description: >-
  Apply Property_Managments contracts-style list UI (app-page, stat-pill, app-card
  table-card, estate-table-toolbar, entity-card) to admin list pages. Use when
  building or upgrading list screens, staff rosters, data tables, floor maps, or
  when the user asks for card system, estate cards, or Property-style UI.
---

# Estate Card System (Property Contracts Pattern)

Reference: `Property_Managments/property-frontend/src/app/features/contracts/contract-list/`

Combine with: `rms-premium-shell` (shell, width, icons), `rms-fullstack-integration` (API data), `estate-table-list-toolbar` (filters/dates above tables).

## Page skeleton

```html
<div class="app-page">
  <app-page-header titleKey="...">
    <rms-icon-btn icon="add" tooltipKey="COMMON.ADD" variant="primary" (clicked)="openDialog()"></rms-icon-btn>
    <rms-icon-btn icon="refresh" tooltipKey="COMMON.REFRESH" (clicked)="load()"></rms-icon-btn>
  </app-page-header>

  <div class="loading-wrap" *ngIf="loading">...</div>

  <div class="app-list-surface" *ngIf="!loading">
    <section class="list-stats">
      <article class="stat-pill stat-pill--purple">
        <span class="stat-label">{{ '...' | translate }}</span>
        <strong>{{ count }}</strong>
      </article>
    </section>

    <section class="app-card table-card">
      <div class="estate-table-toolbar">
        <label class="estate-search-inline">...</label>
        <rms-icon-btn ...></rms-icon-btn>
        <div class="estate-filter-tray">...</div>
      </div>
      <div class="app-table-wrap">...</div>
      <app-table-pager ...></app-table-pager>
    </section>
  </div>
</div>
```

## Staff / roster variant

`staff-card-grid` + `entity-card` with `entity-card__avatar`, `role-chip`, `status-badge`.

## Floor map variant (dashboard / tables)

```html
<div class="floor-map-grid">
  <article class="floor-table-card" *ngFor="let t of tables" [attr.data-status]="t.status">
    <span class="floor-table-num">{{ t.tableNumber }}</span>
    <span class="floor-table-cap">{{ t.capacity }}p</span>
  </article>
</div>
```

SCSS: `grid-template-columns: repeat(auto-fill, minmax(72px, 1fr)); width: 100%`

## SCSS source of truth

| Project | File |
|---------|------|
| Restaurant | `styles/rms-estate-cards.scss` |
| Restaurant dashboard | `styles/sf-restro-dashboard.scss` |
| Property | `property-frontend/src/styles.scss` |
| List aliases | `styles/pages/list-pages.scss` |

## Content width

Pages use full available width (sidebar collapsed = more space):

```scss
--content-max-w: none;
--content-pad-inline: 24px;
```

No `margin-inline: auto` + `max-width: 1400px` on `.app-page`.

## Business binding rules

1. **stat-pill counts** from real API data only — empty shows `0`.
2. **No DEMO_* fallbacks** on error; show `empty-state` instead.
3. **Filters** affect displayed list (client or API `q` param).
4. **CRUD** opens Material dialogs (`RmsDialogService`), not inline forms.
5. **Dates** in tables: `{{ row.date | rmsDate:'datetime' }}`.

## Card styling (dark SF Restro)

- Radius **18px**, border `rgba(255,255,255,0.07)`
- Hover: purple border tint + lift
- stat-pill tones: `--purple`, `--orange`, `--cyan`, `--pink`

## Do not

- Orphan classes without SCSS (`glass-card` without definitions)
- Filters outside `app-card table-card`
- Text Material buttons in headers (use `rms-icon-btn`)
- `datetime-local` in forms (use datepicker + time)
- Demo data masking API failures

## Visual audit

```bash
node scripts/visual-audit-all.mjs restaurant
```
