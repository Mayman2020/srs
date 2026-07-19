---
name: estate-contract-lifecycle-workspace
description: >-
  Property contract detail ops — money hero grid, status action bar, pending
  banners (overdue/renew/terminate), section-tile nav, permission-gated CTAs,
  handover ops, complaints bridge. Use when porting deep contract/ops detail.
---

# Estate Contract Lifecycle Workspace

**Reference:** `Property_Managments/property-frontend/src/app/features/contracts/contract-detail/`  
Dialogs: terminate / cancel-draft / renewal / maintenance-contract-* / type-choice  
Bridge: `complaints-list`, `complaint-detail-dialog`

Combine with: `estate-detail-page-hero`, `estate-status-data-badges`, `estate-entity-audit-trail`, `estate-srs-table-pager`.

## Anatomy

```
page-header (contract number + party/unit subtitle + smart back)
optional banners (overdue / renewal pending / termination pending)
contract-hero-grid  → rent / annual / deposit / days-left stats
action-bar  → status chip + period/property meta + lifecycle CTAs
contract-section-nav tiles  → info | schedule | complaints | annexes | …
section panels (each may have own pager)
app-audit-trail
```

## Hero money grid

`.contract-stat-card` tones (`navy|gold|teal|danger` when expiry ≤30 days). Values numeric + currency foot.

## Action bar + banners

1. Status chip + date range + property·unit meta.  
2. CTAs gated (`canTerminate`, `canRenew`, …) with `actionLoading` / per-id busy.  
3. Overdue banner: warn icon + i18n params + send-reminder + dismiss.  
4. Pending owner decisions → full banners, not only chips.

## Section tile nav

`.contract-section-nav` / `.csn-tile` switches local section (not separate routes unless intentional). Nested lists (payments schedule, annexes, complaint rows) use **own** pager state.

## Terminal / handover

Ended contracts: deposit return, damages report/confirm, unit clear — permission-gated.

## Complaints bridge

From contract: resolve complaint ± create maintenance request; show linked badge in list/detail.

## Do not

- Dump all sections as one endless page without tile nav
- Ungated destructive terminate/cancel
- Replace lifecycle banners with a toast-only UX
