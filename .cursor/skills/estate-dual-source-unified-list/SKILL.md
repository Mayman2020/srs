---
name: estate-dual-source-unified-list
description: >-
  Property contracts list — unify LEASE + MAINTENANCE rows, type LOV + status
  chips, forkJoin merge when ALL, type-choice dialog before create, source-
  aware detail routes. Use when one list must merge two contract-like APIs.
---

# Estate Dual-Source Unified List

**Reference:** `Property_Managments/property-frontend/src/app/features/contracts/contract-list/`  
Services: `contract.service.ts`, `maintenance-contract.service.ts`  
Create: `contract-type-choice-dialog`

Combine with: `rms-property-list-integration`, `estate-workspace-filter-strip`, `estate-lov-picker-system`.

## Unified row

```ts
interface UnifiedContractRow {
  source: 'LEASE' | 'MAINTENANCE';
  id: number;
  // shared display fields…
}
```

Map both APIs into this shape.

## Filter / page strategy

| Type filter | Behavior |
|-------------|----------|
| Single source | Native **server** page for that API |
| `ALL` | `forkJoin` both → merge → sort → slice; sum `totalElements` carefully (document overlap caveat) |

Status LOV + chip tray share `filterStatus`. Type LOV drives the strategy above.

## Navigation / create

1. Detail links branch: `/contracts/:id` vs `/contracts/maintenance/:id`.  
2. Create → `ContractTypeChoiceDialog` when multiple kinds exist.  
3. Row shows type chip + shared status chip; party/header copy may change by type.  
4. Always `ListLoadController` surface + `totalElements` for pills (not `rows.length` alone when server-paged).

## Do not

- Client-filter a **server** page result as if it were the full set
- Use one detail route for both sources
- Forget resetting page on type/status change
