---
name: estate-screen-module-visibility
description: >-
  Property screen_settings + property_module_settings — global screen toggles,
  per-property module catalog/presets, PermissionService triple-gate with RBAC.
  Use when porting /admin/screens and /admin/module-settings visibility controls.
---

# Estate Screen & Module Visibility

**Reference:** `Property_Managments/property-frontend`  
- `features/permissions/screen-management/` → `/admin/screens`  
- `features/permissions/module-management/` → `/admin/module-settings`  
- `core/services/permission.service.ts` (`isScreenEnabled`, `isPropertyModuleEnabled`, `can`)

Backend: `screen_settings`, `property_module_settings`, module catalog definitions/presets.

Combine with: `estate-settings-security-roles` (role JSON still required). Vertical apps **may skip** this package.

## Two layers (orthogonal to role matrix)

| Layer | Scope | Effect |
|-------|--------|--------|
| **Screens** | Global per `screenKey` | If `globallyEnabled=false` → hidden for everyone (except evaluator bypasses only for super-admin where coded) |
| **Property modules** | Per `propertyId` + `moduleKey` | Feature pack off for that property’s users; SUPER_ADMIN bypasses module check |

Role matrix (`role_permissions`) is a **third** gate — all must pass in `can()`.

## Admin UIs

1. Screens page: list screen keys + enabled toggles; save via PUT by key.  
2. Module settings: pick property → catalog definitions + presets → bulk enable map → PUT modules.  
3. Summary counts (enabled / total) in headers/dialogs.  
4. Sidebar entry gated to SUPER_ADMIN / GENERAL_MANAGER style roles.

## Runtime gates

```ts
can(module, action):
  superAdmin → true
  !isScreenEnabled(module) → false
  !isPropertyModuleEnabled(module) → false  // superAdmin skipped earlier
  !roleModule || enabled===false → false
  return roleModule[action] === true
```

Sidebar also: `isPropertyModuleEnabled(permissionKey)` before showing the item.

`loadMine()` must refresh screens + property modules together with role permissions.

## Do not

- Treat screen off as “role denied” only — fix screens admin, not only matrix  
- Forget SUPER_ADMIN bypass on property modules  
- Port this to Clinic/Restaurant unless multi-property feature packs are required
