# Permissions architecture (RBAC)

## 1. How effective permissions are computed

- **Roles** are assigned to users in `user_role` (with `valid_from` / `valid_to` windows).
- Each role grants **permissions** via `role_permission`.
- The user's **effective permission set** is the **union** of every permission granted by every
  role assignment that is currently valid AND active AND non-deleted. Concretely, the union is
  computed by a **single SQL join** in
  `UserRoleRepository.findEffectivePermissionIdsByUserId` that filters on **all** of the
  following predicates simultaneously:

  | Source                | Predicate                                                |
  |-----------------------|----------------------------------------------------------|
  | `user_role.valid_from`| `<= current_timestamp` (future-dated rows ignored)       |
  | `user_role.valid_to`  | `IS NULL OR > current_timestamp` (expired rows ignored)  |
  | `role.deleted_at`     | `IS NULL` (soft-deleted roles drop out)                  |
  | `role.is_active`      | `= true`  (deactivated roles drop out)                   |
  | `permission.deleted_at` | `IS NULL` (soft-deleted permissions drop out)          |
  | `permission.is_active`  | `= true`  (deactivated permissions drop out)            |

- **Acting manager expansion (Slice 4):** the same effective set is **unioned** with permissions
  of users for whom the principal currently holds an active `acting_assignment` row when the row’s
  department scope is open (`department_id IS NULL`) or matches the actor’s department. The query
  lives in `UserRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing`; callers such as
  `EffectiveUserPermissionService` use it so an acting user temporarily inherits the absent user’s
  role grants without editing `user_role`.

- This contract is pinned by two layers of tests:
  - `EffectivePermissionUnionPostgresTest` (Testcontainers Postgres, real Flyway): proves every
    predicate at the database level.
  - `EffectiveUserPermissionServiceTest`: pins the SQL string itself so a refactor that drops
    one predicate fails compile-time.

## 2. The capabilities + navigation endpoints

- **`GET /api/v1/me/capabilities`** returns:
  - `roles`: codes for all currently valid role assignments (sorted, alphabetical, case-insensitive).
  - `permissions`: canonical permission codes for the effective union (filtered to active + non-deleted).
  - `screens`: rows from `ui_screen` the user may access (filtered by required permission).
- **`GET /api/v1/profile/me/navigation`** returns shell sidebar items from `ui_screen` with
  `show_in_shell_nav = true`, filtered by the **same effective permission union** as
  `/me/capabilities`. The Angular shell loads this via `ProfileNavigationApiService` whenever the
  session changes (login, token refresh, role switch).
- Both endpoints are gated by class-level `@PreAuthorize("isAuthenticated()")` on
  `MeCapabilitiesController`. That contract is pinned by ArchUnit
  (`ModuleBoundaryArchTest.capabilitiesControllerCarriesClassLevelPreAuthorize`).

## 3. activeRole vs effective permissions

> **The JWT `currentRole` / `active_role` claim is _never_ the source of authorization.**

- `currentRole` exists for workflow and audit context (which role a user is acting as for the
  current task) and for `SecurityUtils.requireCurrentRoleCode()`.
- It is validated at JWT decoding time (`AcJwtAuthenticationConverter`) against the live set of
  active DB role assignments, so a stale or tampered claim is rejected.
- Capability resolution (`EffectiveUserPermissionService`, `UserCapabilitiesService`,
  `ShellNavigationService`, `EffectivePermissionExpressions`) reads **only** the user id. The
  active role is intentionally ignored.
- This is locked in by `EffectivePermissionExpressionsTest.answerIsSameRegardlessOfJwtActiveRole`
  and `UserCapabilitiesServiceTest.activeRoleSwitchDoesNotReduceCapabilities`.

Concretely, switching from `ROLE_STAFF` to `ROLE_SYS_ADMIN` in the JWT does **not** widen or
narrow what the backend will allow; both clients see the same union. The frontend reloads
`/me/capabilities` and `/profile/me/navigation` on every session change purely for cache
freshness, not because the answer could change.

## 4. Source of truth

| Concern                                          | Authority                                                                                                                                |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Who has which permission                         | `role`, `permission`, `role_permission`, `user_role`                                                                                     |
| Which routes appear in the shell sidebar         | `ui_screen` (`required_permission_id`, `route_path`, `show_in_shell_nav`)                                                                |
| API authorization for admin/critical actions     | Backend: `@PreAuthorize("@effectivePermission.has('CODE')")` + service-level checks for cross-cutting rules (confidentiality, ownership) |
| Hiding buttons / route guards in Angular         | Frontend `CapabilitiesService.can()` / `permissionCanMatch` — **UX only**, never the only control                                        |

The frontend is allowed to call `cap.can()`, `canAny()`, and `canAll()` for UX hiding. The
backend remains the source of truth: every protected endpoint must carry `@PreAuthorize` and is
asserted to do so by `ModuleBoundaryArchTest.publicEndpointsCarryPreAuthorize`.

## 5. Permission aliases (legacy code bridge)

`permission_alias` maps legacy dotted codes (`correspondence.view`, `user.manage`,…) and V30
SCREAMING_SNAKE codes (`VIEW_TRANSACTIONS`, `CANCEL_TRANSACTION`,…) to canonical permissions.
Backend `@PreAuthorize` resolves both via `PermissionRepository.findByCanonicalOrAliasCode`,
so legacy expressions in old controllers keep working.

> The **frontend** only receives canonical codes from `/me/capabilities`. Templates and route
> guards must use the **canonical** code (e.g. `cap.can('CORRESPONDENCE_DELETE')`, not
> `cap.can('CANCEL_TRANSACTION')`). Mixing the two silently disables the gate. The
> `npm run check:routes` diagnostic flags any such drift.

## 6. How to add a new permission safely

1. **Seed it** in a new Flyway migration:

   ```sql
   INSERT INTO srs_system.permission (code, name_ar, name_en, description, sort_order, is_active)
   VALUES ('FEATURE_X_VIEW', 'عرض ميزة X', 'View feature X', '...', 950, TRUE);
   ```

2. **Grant it** to the appropriate roles in the same migration:

   ```sql
   INSERT INTO srs_system.role_permission (role_id, permission_id)
   SELECT r.id, p.id
   FROM srs_system.role r CROSS JOIN srs_system.permission p
   WHERE r.code IN ('SYS_ADMIN', 'AUDITOR') AND p.code = 'FEATURE_X_VIEW'
     AND r.deleted_at IS NULL AND p.deleted_at IS NULL
     AND NOT EXISTS (
       SELECT 1 FROM srs_system.role_permission rp
       WHERE rp.role_id = r.id AND rp.permission_id = p.id
     );
   ```

3. **(Optional) Bind it to a UI screen** if it gates a sidebar entry:

   ```sql
   INSERT INTO srs_system.ui_screen
     (code, route_path, name_ar, name_en, description, sort_order, is_active,
      icon_key, show_in_shell_nav, required_permission_id)
   VALUES
     ('feature_x', '/feature-x', 'ميزة X', 'Feature X', '…', 99, TRUE, 'star', TRUE,
      (SELECT id FROM srs_system.permission WHERE code = 'FEATURE_X_VIEW' AND deleted_at IS NULL));
   ```

4. **(Optional) Add a `permission_alias` row** if you are retiring a legacy code so old
   `@PreAuthorize` strings continue to resolve until callers are migrated.

5. **Use the canonical code** in the Angular route:

   ```ts
   data: { titleKey: 'featureX.pageTitle', permission: 'FEATURE_X_VIEW' }
   ```

6. **Annotate every new controller method** with
   `@PreAuthorize("@effectivePermission.has('FEATURE_X_VIEW')")` (or `isAuthenticated()` for
   self-scoped endpoints). This is enforced by
   `ModuleBoundaryArchTest.publicEndpointsCarryPreAuthorize`.

7. **Run the gates**:
   - `mvnw -B test` (Mockito + ArchUnit + Testcontainers union test)
   - `npm run check:i18n`
   - `npm run check:routes`  ← cross-checks the new code with the route table and `ui_screen` seed.

## 7. Scenario validation (expected behavior)

| Scenario                              | Expected                                                                                                                                                                                                |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **USER only**                         | `/me/capabilities` returns only USER's permissions; sidebar matches; `permissionCanMatch` allows USER routes; ADMIN-only routes redirect to `/dashboard`.                                              |
| **ADMIN only**                        | Full ADMIN set; sidebar matches.                                                                                                                                                                       |
| **USER + ADMIN**                      | Effective permissions = **union**; both USER and ADMIN routes are reachable regardless of which role is currently active in the JWT.                                                                   |
| **Expired `user_role` row**           | Ignored at the DB layer (predicate on `valid_to`); union recomputes immediately, no cache invalidation needed.                                                                                         |
| **Future-dated `user_role` row**      | Ignored until `valid_from <= now()`; verified by `EffectivePermissionUnionPostgresTest.futureDatedUserRoleIsIgnored`.                                                                                  |
| **Deactivated / soft-deleted role**   | Permissions granted by that role drop out of the union immediately; verified by `softDeletedRoleRemovesCapability` and `deactivatedRoleRemovesCapability`.                                              |
| **activeRole switch**                 | Capabilities are identical; only the audit/workflow `currentRole` claim changes. Verified by `UserCapabilitiesServiceTest.activeRoleSwitchDoesNotReduceCapabilities`.                                  |
| **Direct URL without permission**     | `permissionCanMatch` returns `UrlTree('/dashboard')`. Verified by `permission.guard.spec.ts`. The backend still rejects the corresponding API call with 403 (frontend hiding is never the only gate).  |
| **Menu vs route mismatch**            | `npm run check:routes` reports `menu-route-mismatch` when a `ui_screen` row's `required_permission_id` resolves to a different canonical code than the Angular route's `data.permission`.              |
| **Capabilities load failure (401/5xx)** | `permissionCanMatch` returns `UrlTree('/login')`; the sidebar logs a `console.warn` (no silent failure).                                                                                              |

## 8. Diagnostics

Run `npm run check:routes` from `SRS_System_frontend/` (also runnable in CI). It cross-checks
three sources of truth:

1. `src/app/app.routes.ts`: every `data.permission` value.
2. `SRS_System_backend/.../db/migration/*.sql`: every canonical `permission` row and every
   `permission_alias` row.
3. `SRS_System_backend/.../db/migration/*.sql`: every `ui_screen` row with `route_path` and
   `required_permission_id`.

It reports:

- `missing-permission`: an Angular route's `data.permission` is neither a canonical nor an alias
  code (route would be permanently locked).
- `route-uses-alias`: an Angular route uses a legacy alias code that the FE will never see in
  `/me/capabilities` (route would be permanently locked on the FE).
- `menu-route-mismatch`: a `ui_screen` and its corresponding Angular route disagree on the
  required permission (sidebar shows the link but `permissionCanMatch` redirects, or vice versa).
- `dangling-ui-screen`: a `ui_screen` with `show_in_shell_nav = true` whose `route_path` has no
  matching Angular route (broken sidebar link).

The script exits non-zero on any mismatch and is part of the standard gate set.
