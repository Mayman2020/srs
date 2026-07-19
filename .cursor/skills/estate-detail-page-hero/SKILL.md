---
name: estate-detail-page-hero
description: >-
  Property detail page composition — breadcrumb, detail-head hero chips,
  timeline steps/rail, detail-panel cards, smart back from lists. Use when
  building request/contract/inspection detail screens beyond simple tables.
---

# Estate Detail Page Hero

**Reference:** `Property_Managments/property-frontend`  
- `src/styles/pages/request-detail.page.scss`  
- Features: `maintenance/request-detail`, `contracts/contract-detail`, `inspections/inspection-detail`  
- Header: `shared/components/page-header`

Combine with: `rms-premium-shell` (smart back), `estate-status-data-badges`, `estate-entity-audit-trail`.

## Skeleton

```
breadcrumb trail (+ optional back)
detail-head
  detail-copy → eyebrow + title (display font) + hero-chips (status/priority)
  detail-actions → primary/secondary buttons
body
  app-card detail-panel sections…
  optional timeline / related tables
  app-audit-trail footer
```

## Hero chips

```html
<div class="hero-chips">
  <span class="status-badge" [attr.data-status]="item.status">…</span>
  <span class="priority-badge" [ngClass]="item.priority">…</span>
</div>
```

Entity codes use `--font-mono`; title uses `--font-display`.

## Timeline

Two patterns from request-detail:

1. **Horizontal** `.timeline-step` for short lifecycle (4–6 states).  
2. **Vertical** `.timeline-rail` with filled spine `%` from current status index.

## Smart back

```html
<app-page-header
  [title]="…"
  [showBack]="true"
  …>
```

- From **list navigation**: show back when `canGoBack()`.  
- From **sidebar menu**: call `NavigationHistoryService.markFromMenu()` so back stays hidden.

## Detail panels

Nest related data in `app-card` / `.detail-panel` sections (meta grid, attachments, history).  
Do not cram detail hero layout into list `table-card` pages.

## Do not

- Flat walls of unbound fields without head/chips
- Fake timelines with static CSS and wrong status
- Mixing list toolbar patterns into detail heroes
