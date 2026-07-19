---
name: estate-status-data-badges
description: >-
  Property status/priority badge matrix — data-status chips, priority badges,
  app-status-badge, STATUS/PRIORITY i18n. Use when rendering lifecycle states in
  tables, detail heroes, or dialogs across RMS estate-style apps.
---

# Estate Status & Data Badges

**Reference:** `Property_Managments/property-frontend`  
- Global CSS: `src/styles.scss` (`.status-badge`, `.priority-badge`)  
- Component: `shared/components/status-badge/status-badge.component.ts`  
- Labels: `assets/i18n` → `STATUS.*`, `PRIORITY.*`

Combine with: `estate-card-system` (table cells), `estate-detail-page-hero` (hero chips).

## Usage

```html
<span class="status-badge" [attr.data-status]="row.status">
  {{ ('STATUS.' + row.status) | translate }}
</span>

<!-- or -->
<app-status-badge [status]="row.status" [label]="label"></app-status-badge>

<span class="priority-badge" [ngClass]="row.priority">
  {{ ('PRIORITY.' + row.priority) | translate }}
</span>
```

## Status matrix (`data-status`)

Map lifecycle enums to tokens — never invent per-page colors:

| Status | Semantic |
|--------|----------|
| `PENDING` | warn/pending |
| `ASSIGNED` / `SCHEDULED` / `IN_PROGRESS` | info / progress |
| `COMPLETED` / `ACTIVE` / `APPROVED` / `PAID` / `OK` | ok / green |
| `CANCELLED` / `INACTIVE` / `REJECTED` / `URGENT` / `BAD` | bad / red |
| `TENANT_ABSENT` / `NEEDS_REVISIT` | specialty warn |
| `LOW` / `NORMAL` / `HIGH` | soft tier chips |

Pill includes a leading `::before` color dot.

## Priority

`.priority-badge.LOW | .NORMAL | .HIGH | .URGENT`  
`URGENT` may use a subtle pulse — keep it restrained.

## Rules

1. Labels always from i18n (`STATUS.*` / `PRIORITY.*`) — never hardcoded English in templates.
2. SLA / breach markers use `--bad` beside status chips — do not replace status.
3. Dark theme: translucent `*-bg` from Estate tokens, not solid pastel light colors.
4. Same chip on list tables **and** detail hero — one visual language.

## Do not

- `mat-chip` with random hex per feature
- Encode meaning only by color (always show translated text)
