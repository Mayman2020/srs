---
name: estate-owner-approval-inbox
description: >-
  Property owner contract-approvals inbox — badge tabs per queue, contract-card
  grids, per-tab client pager, approve/reject/amend with per-id busy maps. Use
  for owner portal pending draft/terminate/renew queues (lease + maintenance).
---

# Estate Owner Approval Inbox

**Reference:** `Property_Managments/property-frontend/src/app/features/owner/contract-approvals/`

Combine with: `estate-srs-table-pager`, `estate-status-data-badges`, `estate-in-app-notifications` (deep links often land here).

## Pattern

```
page-header
global empty  → “no pending approvals”
approval-tabs  → one tab per queue with count badge
tab panels
  per-tab empty message OR
  contracts-grid of contract-card (+ decide actions)
  app-table-pager (client, own page index)
```

## Queues (Property)

Lease: drafts | terminations | renewals  
Maintenance: drafts | terminations | renewals  

## Rules

1. Tabs carry **count badges**; active tab styled distinctly.  
2. Cards show key info-rows + primary actions (approve / reject / request amend).  
3. Each tab has independent `*Page` index + `paginatedSlice`.  
4. Busy maps keyed by id (`activating[id]`, `busyReject[id]`) — one card busy ≠ whole page.  
5. “View details” routes to contract detail; keep decide actions on the card.  
6. Distinguish global empty vs per-tab empty copy.

## Do not

- One mixed table of all queue types without tabs
- Disable all buttons when a single approve is in flight
