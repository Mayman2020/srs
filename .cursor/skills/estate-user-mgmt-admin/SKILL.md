---
name: estate-user-mgmt-admin
description: >-
  Property users admin screen — list CRUD, activate/deactivate toggle action,
  per-id busy, user dialog with role and optional password, no invent ban/lock.
  Use when building /admin/users like Property.
---

# Estate User Mgmt Admin

**Reference:** `Property_Managments/property-frontend/src/app/features/users/user-management/`  
Dialog: `features/users/user-dialog/`  
API: `UserService` + `PATCH /users/{id}/toggle-active`

Combine with: `estate-settings-security-roles` (auth/RBAC), `estate-table-row-actions`, `estate-identity-media-uploads`, `estate-my-profile-page`.

## What Property does (and does not)

| Feature | Property |
|---------|----------|
| Create / edit user | Yes — dialog |
| Activate / deactivate | Yes — `toggle` permission + toggle-active API |
| Ban / blacklist UI | **No** — do not invent |
| Admin unlock account | **No** (login brute-force is server-side only) |
| Email password reset from users list | **No** — optional password in edit dialog |

## List + actions

```html
<button class="app-icon-btn"
  [ngClass]="user.isActive ? 'danger' : 'success'"
  *ngIf="permissions.can('users', 'toggle')"
  [disabled]="togglingIds.has(user.id)"
  (click)="toggleActive(user)"
  [matTooltip]="(user.isActive ? 'USER_MGMT.DEACTIVATE' : 'USER_MGMT.ACTIVATE') | translate">
  <span class="material-icons">{{ user.isActive ? 'block' : 'check_circle' }}</span>
</button>
```

Rules:

1. Gate with `users` + action `toggle` (not only `edit`).
2. Track `togglingIds` / busy map per user id.
3. Show status badge Active/Inactive via `estate-status-data-badges`.
4. Edit/delete/view use standard row icons + `*appCan`.
5. Soft-delete (if any) is separate from deactivate.

## Create / edit dialog

1. Role select from allowed roles (not free text).
2. Create → generate/default password + `mustChangePassword=true`.
3. Edit → optional new password fields (admin reset without mail).
4. Never deactivate the currently logged-in user without a safe warning (or block).
5. Reuse identity media when person profile image is required.

## After save

- Refresh list via `ListLoadController` soft refresh.
- If permissions/role changed for another user, they need re-login / `loadMine` on next session.

## Do not

- Add Ban/Lock screens “because other SaaS has them” without backend support
- Use deactivate as a substitute for soft-delete without product decision
- Forget i18n `USER_MGMT.ACTIVATE|DEACTIVATE`
