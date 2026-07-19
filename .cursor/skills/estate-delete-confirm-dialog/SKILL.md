---
name: estate-delete-confirm-dialog
description: >-
  Property delete/destructive confirm dialog — MatDialog confirm with warning
  icon, danger button, i18n keys, blocked-delete alert. Use before every delete,
  archive, deactivate, or irreversible action across RMS/Estate apps.
---

# Estate Delete Confirm Dialog

**Reference:** `Property_Managments/property-frontend`  
- `core/services/delete-confirm.service.ts`  
- `shared/components/confirm-dialog/confirm-dialog.component.ts`  
Combine with: `rms-premium-shell` (dialog panel), `estate-icon-tooltips`, `estate-settings-security-roles`.

## Rule (mandatory)

Every **destructive** action MUST open a confirm dialog **before** calling the API:

| Action | Examples |
|--------|----------|
| Delete | users, products, lookups, attachments |
| Archive | patients, records |
| Deactivate | users, providers, promotions |
| Cancel irreversible | orders, reservations, contracts |

**Never** use native `window.confirm()` / `confirm()` — use Material dialog (or ERP/ng-bootstrap equivalent).

## Service API

```typescript
// Material apps — DeleteConfirmService
this.deleteConfirm.openDeleteConfirm({
  messageKey: 'DIALOG.DELETE_GENERIC',
  // or message: `Delete "${name}"?`
}).subscribe((ok) => {
  if (!ok) return;
  this.api.delete(id).subscribe({
    next: () => this.snack.success(...),
    error: (err) => this.deleteConfirm.handleDeleteError(err, this.snack)
  });
});
```

| Method | Use |
|--------|-----|
| `openDeleteConfirm(opts)` | Standard delete/archive — `danger: true`, warning icon |
| `openDeleteBlocked(detail?)` | FK/constraint failure — `alertOnly`, error icon |
| `handleDeleteError(err, snack)` | Route constraint errors to blocked dialog |

## Dialog UI (Property)

- Width **440px**, `panelClass: 'app-dialog-panel'`
- Warning icon + title + message (pre-wrap for multi-line)
- Cancel (stroked) + Confirm (flat, **danger** red for delete)
- `alertOnly: true` → single Close button (blocked delete)

## i18n keys (add to ar.json + en.json)

```json
"DIALOG": {
  "DELETE_TITLE": "Confirm deletion",
  "DELETE_GENERIC": "Are you sure you want to delete this record? This action cannot be undone.",
  "DELETE_NAMED": "Are you sure you want to delete \"{{name}}\"?",
  "DELETE_BLOCKED_TITLE": "Cannot delete",
  "DELETE_BLOCKED_GENERIC": "This item cannot be deleted because it is linked to other data.",
  "DELETE_BLOCKED_WITH_DETAIL": "This item cannot be deleted because it is linked to other data.\n\n{{detail}}"
}
```

Entity-specific: `PATIENTS.ARCHIVE_CONFIRM`, `USERS.DEACTIVATE_CONFIRM`, `LOOKUPS.DELETE_CONFIRM`, etc.

## Per stack

| App type | Service / pattern |
|----------|-------------------|
| Clinic, RMS, POS, Vzeeta, Mazaad | `DeleteConfirmService` + `ConfirmDialogComponent` |
| AromaFlow | `ConfirmService.ask({ danger: true, ... })` + `<app-confirm-dialog />` in root |
| ERP | `ConfirmDialogService.confirmByKey({ danger: true })` |
| SRS | `DialogService.openConfirm({ confirmButton: { color: 'warn' } })` |

## RmsDialogService shortcut

```typescript
confirmDelete(messageKey = 'DIALOG.DELETE_GENERIC', titleKey = 'DIALOG.DELETE_TITLE')
```

## Rollout checklist

1. Port `delete-confirm.service.ts` + Property-style `confirm-dialog` if missing.
2. Add `DIALOG.*` keys to i18n.
3. Grep `confirm(` and `window.confirm` — replace all.
4. Grep `.delete(` / `deactivate` / `archive` in components — ensure dialog precedes API call.
5. Use `handleDeleteError` on delete API errors.

## Do not

- `window.confirm()` / browser `confirm()`
- Delete without user confirmation
- Hardcoded Arabic/English strings in dialog (use i18n keys)
- Snack-only for FK constraint errors (use blocked dialog)
