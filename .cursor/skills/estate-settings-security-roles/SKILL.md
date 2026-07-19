---
name: estate-settings-security-roles
description: >-
  Port Property/Estate (Property_Managments) settings, security, and RBAC patterns
  to Clinic, Restaurant, and similar Spring Boot + Angular apps. Use when implementing
  or verifying JWT auth, role permissions matrix, user management, clinic/branch
  settings, must-change-password, lookups admin, or applying the estate playbook
  to a new vertical app.
---

# Estate Settings, Security & Roles Playbook

Apply patterns from `Property_Managments` to vertical apps (Clinic, Restaurant RMS, etc.).

## Reference source

| Layer | Path |
|-------|------|
| Property backend | `Property_Managments/property-backend` |
| Property frontend | `Property_Managments/property-frontend` |
| API base pattern | `/api/v1` context path |

## Backend checklist (Spring Boot + Flyway + PostgreSQL)

### Security stack (copy/adapt)

- `SecurityConfig` — stateless JWT, `/auth/**` public, CSRF off
- `JwtUtil` — access + refresh tokens; claims: `role`, `userId`, `mustChangePassword`
- `JwtAuthFilter` — Bearer token → `SecurityContext`
- `MustChangePasswordFilter` — block APIs when `mustChangePassword=true` except `/auth/**` and `/users/me/change-password`
- `TokenBlacklistService` + `revoked_tokens` table — logout revocation
- `LoginAttemptService` — brute-force lockout (optional: persist to DB for production)
- `AuthController` — `POST /auth/login`, `/auth/refresh`, `/auth/logout`

### Roles & permissions

- Fixed `UserRole` / `RoleType` enum on `users.role`
- `role_permissions` table — JSON per module: `view`, `create`, `edit`, `delete` (+ `menu`, `enabled`, `export`, `approve` for richer apps)
- `@RequiresPermission(module, action)` + `PermissionAspect` + `PermissionEvaluatorService`
- **ADMIN bypass** — hardcoded in evaluator and frontend `PermissionService.can()`
- `RolePermissionController` — `GET /role-permissions` or `/permissions`, `PUT /{role}`

### Settings

- Key-value settings table (`clinic_settings`, `branch_settings`, etc.)
- `SettingsController` — list, get by key, upsert; gate with `settings.view` / `settings.edit`
- Mask secret keys (`password`, `secret`, `token`, `api_key`) on read
- **Restaurant note:** `taxRate` / `serviceCharge` must sync with `branches` columns — billing reads branches, not settings KV

### Lookups (reference data — Property/Estate style)

Full playbook: **estate-reference-lookups** (admin screen + no static lists + cache). Summary:

Backend module `lookup/`:

- Flyway migration — **PostgreSQL only** (`BIGSERIAL`, `CONSTRAINT uk_lookup_type_code UNIQUE`, `SET search_path`)
- Entity `Lookup` — `type`, `code`, `name_ar`, `name_en`, `sort_order`, `is_active`, `is_locked`
- `LookupController` — `/lookups/by-type`, `/lookups/admin/*` gated by `settings.view/edit`
- Seed rows per vertical (Clinic: `SPECIALTY`, `DEPARTMENT`, `LAB_TEST_TYPE`, …; Restaurant: `MENU_CATEGORY`, `ORDER_CHANNEL`, …)

Frontend:

- `lookup.service.ts`, `lookup-select` / cache, `lookup-dialog`, `lookup-management` at `/admin/lookups`
- Wire forms via lookups **instead of hardcoded enums**
- Sidebar entry + i18n `LOOKUPS.*`

**Migration pitfall:** After editing SQL, run `mvn clean compile` — Flyway reads `target/classes`, not `src` directly.

### User management

- `UserController` — CRUD, `PATCH /{id}/toggle-active`, `GET/PUT /users/me`, `POST /users/me/change-password`
- New users: BCrypt hash, default password, `mustChangePassword=true`
- **Property does NOT have** separate Ban / admin Unlock / email-reset endpoints — use **activate/deactivate (`toggle`)** only; optional password fields in edit dialog as admin reset

### Permission evaluation order (frontend `PermissionService.can`)

```
1. SUPER_ADMIN / isSuperAdmin() → allow
2. screen_settings[module] !== false     (global screen visibility)
3. property_modules[module] !== false    (per-property; SUPER_ADMIN skips)
4. role JSON module exists
5. module.enabled !== false
6. module[action] === true
```

`loadMine()` = `forkJoin` of role-permissions-me + screen-settings + property-modules-me.

### Migrations (PostgreSQL only)

Never use MySQL syntax in Flyway scripts:

```sql
-- WRONG (MySQL)
id BIGINT AUTO_INCREMENT PRIMARY KEY
UNIQUE KEY uk_x (a, b)
ON UPDATE CURRENT_TIMESTAMP

-- CORRECT (PostgreSQL)
id BIGSERIAL PRIMARY KEY
CONSTRAINT uk_x UNIQUE (a, b)
updated_at TIMESTAMP DEFAULT NOW()
SET search_path TO schema_name;
```

After editing migrations: `mvn clean compile` so `target/classes` picks up changes.

## Frontend checklist (Angular standalone)

### Services

- `AuthService` — login/logout, token storage, `mustChangePassword()`, `clearMustChangePassword()`
- `PermissionService` — `loadMine()` from API (not login-only), `can(module, action)`
- `RolePermissionService` — `getAll()`, `update(role, permissions)`
- `UserService` — full CRUD + `toggleActive`
- `UserProfileService` — `getMyProfile()`, `changeMyPassword()`
- `SettingsService` — branch/clinic settings HTTP

### Guards & interceptors

- `authGuard`, `guestGuard`, `adminGuard`
- `mustChangePasswordGuard` — redirect to `/admin/profile?changePassword=1`
- `permissionGuard` — `route.data.permission` + `permissionAction`
- `authInterceptor` — `Authorization: Bearer`
- `APP_INITIALIZER` — `permissions.loadMine()` on startup

### Admin routes (minimum)

| Route | Purpose |
|-------|---------|
| `/admin/settings` | General settings |
| `/admin/lookups` | Reference data |
| `/admin/users` | User CRUD |
| `/admin/permissions` | Role permission matrix |
| `/admin/profile` | Change password |

### UI patterns (estate card style)

#### Users admin (`/admin/users`)

- List + `user-dialog` create/edit (role select, optional identity media)
- Row action **activate/deactivate**: `permissions.can('users','toggle')` → `PATCH .../toggle-active`
  - Active user → danger icon + `USER_MGMT.DEACTIVATE`
  - Inactive → success icon + `USER_MGMT.ACTIVATE`
  - Per-id busy set (`togglingIds`) so one row doesn’t lock the table
- Soft-delete if present is separate from deactivate — don’t invent Ban
- Create: default password + force `mustChangePassword`; edit may set a new password (admin reset without email)

#### Permissions matrix UX (`/admin/permissions`)

Not a flat endless checkbox page in Property:

1. Role **cards** list → open **edit dialog** per role  
2. Dialog summary: enabled modules count / enabled actions count  
3. Per module box: master `enabled` slide-toggle + action toggles  
4. Actions beyond CRUD: `view`, `create`, `edit`, `delete`, `menu`, `export`, `approve`, `toggle`, `manage`, …  
5. Save → `PUT /role-permissions/{role}` → tell users to re-login / reload `loadMine`

#### Nav & route gating

| Layer | Rule |
|-------|------|
| Sidebar item | Prefer `can(permissionKey, 'menu')` (Property); many items use `bypassPermission` + role list + `isPropertyModuleEnabled` |
| Route | `permissionGuard` with `data.permission` + `permissionAction` (default `view`) |
| Buttons | `*appCan="'module'; action: 'create'"` / `*appCannot` |
| Role switch | Rebind `*appCan` on `activeRoleChanged` |

Do **not** show a nav item solely because `view=true` if product uses distinct `menu` bits.

#### Must-change password UX variants

| App style | Flow |
|-----------|------|
| Property | Dedicated `/change-password` route → save → **logout** (re-login) |
| Verticals | Often `/admin/profile?changePassword=1` (see `estate-my-profile-page`) |

`MustChangePasswordFilter` blocks APIs except auth + change-password endpoint.

Also: `*appCan` on buttons; sidebar filters by permissions + optional screen/module gates; login redirects when `mustChangePassword`.

### i18n keys to add

`USERS.*`, `USER_MGMT.*`, `PERMISSIONS.*`, `PROFILE.*`, `NAV.USERS`, `NAV.PERMISSIONS`, `NAV.SCREENS`, `NAV.CLIENT_MODULES`, `ROLE.*`, `SCREENS.*`, `MODULES.*`

## Satellite skills (Property complexity — optional)

| Skill | When to apply |
|-------|----------------|
| **estate-reference-lookups** | `/admin/lookups` + replace static lists with by-type / cache |
| **estate-screen-module-visibility** | Global screens + per-property modules admin UIs |
| **estate-user-access-multi-role** | Extra roles, effective-permissions inspector, `X-Active-Role` |
| **estate-user-mgmt-admin** | Concrete users-list activate/deactivate + dialog checklist |

## Verification steps

1. `mvn clean compile` + start backend — Flyway migrations succeed
2. `npm run build` — frontend compiles
3. `mvn test` — security/permission unit tests pass
4. Manual: login → settings save → users CRUD → permissions matrix save → re-login as non-admin → menu respects permissions

## App-specific status

| App | Settings | Security | Roles UI | Lookups |
|-----|----------|----------|----------|---------|
| Property | Full (modules + screens) | Full | Full | `/admin/lookups` |
| Clinic | `clinic_settings` + page | JWT + blacklist + must-change | `/admin/permissions` + `/admin/users` | `/admin/lookups` V13 |
| Restaurant | `branch_settings` + branch tax sync | JWT + blacklist + must-change | `/admin/permissions` + `/admin/users` + `/admin/profile` | `/admin/lookups` V7 |

## What Property has that vertical apps may skip

- `screen_settings` — global UI toggles
- `property_module_settings` — per-property feature flags
- `user_property_access` — multi-property scope
- Multi-portal routes (`/tenant`, `/officer`, etc.)
- `X-Active-Role` / `X-Selected-Property-Id` headers

Only port these when the vertical app needs the same complexity.
