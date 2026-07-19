---
name: estate-user-access-multi-role
description: >-
  Property user-access workspace — primary + extraRoles from JOB_TITLE lookups,
  effective-permissions inspector, X-Active-Role switcher sync. Use when users
  need more than one role beyond simple users CRUD.
---

# Estate User Access Multi-Role

**Reference:** `Property_Managments/property-frontend/src/app/features/users/user-access-management/`  
API: role / `extraRoles` patch on users; lookups `JOB_TITLE` as role codes  
Topbar: `estate-topbar-user-chrome` role menu + `AuthService.activeRoleChanged`

Combine with: `estate-settings-security-roles`, `estate-user-mgmt-admin`, `estate-lov-picker-system`.

Vertical apps with a **single role per user** can skip this.

## Workspace vs users list

| Screen | Job |
|--------|-----|
| `/admin/users` | CRUD account, activate/deactivate |
| `/admin/user-access` | Assign primary + extra roles; inspect effective permissions |

## Rules

1. Role options come from **JOB_TITLE** (or equivalent) lookups — `code` ∈ `UserRole` enum — not a hardcoded dropdown only.  
2. Persist primary role + `extraRoles[]` via dedicated PATCH.  
3. Effective-permissions dialog: read-only pills per module/action for the selected user’s roles (merge for display).  
4. Topbar role switcher lists `getEffectiveRoles()`; on switch → update active role header (`X-Active-Role`) → `PermissionService.loadMine()` → navigate home.  
5. `*appCan` re-evaluates on `activeRoleChanged`.  
6. SUPER_ADMIN may still bypass checks — document product rules for impersonation vs real multi-role.

## Optional property scope (related)

`user_property_access` + `X-Selected-Property-Id` is **orthogonal** to multi-role — scope which properties a user sees. Port only for multi-property ops; don’t mix into the RBAC matrix dialog.

## Do not

- Cram multi-role editor into the dense users table row without a workspace  
- Switch role without reloading permissions  
- Invent ban/lock here
