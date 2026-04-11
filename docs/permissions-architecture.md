# Permissions architecture (RBAC)

## 1. How capabilities work

- **Roles** are assigned to users in `user_role` (with validity windows).
- Each role grants **permissions** via `role_permission`.
- **`GET /api/v1/me/capabilities`** returns:
  - `roles`: codes for all **currently valid** role assignments (sorted).
  - `permissions`: permission **codes** for the user’s **effective** set (see below).
  - `screens`: metadata rows from `ui_screen` the user may access (filtered by required permission).

Effective permissions are computed only from the database, not from the JWT “active role” alone.

## 2. Navigation logic

- **`GET /api/v1/profile/me/navigation`** returns shell sidebar items from `ui_screen` where `show_in_shell_nav` is true, filtered by the **same effective permission union** as capabilities.
- The Angular app loads this via `ProfileNavigationApiService` when the session changes (login, refresh, role switch in the token).
- Route access uses **`CapabilitiesService.can()`** (data from `/me/capabilities`) and `permissionCanMatch` in `app.routes.ts`.

## 3. Source of truth

| Concern | Authority |
|--------|-----------|
| Who has which permission | `role`, `permission`, `role_permission`, `user_role` |
| Which routes appear in the shell | `ui_screen` (`required_permission_id`, `route_path`, `show_in_shell_nav`) |
| API authorization for admin/critical actions | Backend (`@PreAuthorize` with `effectivePermission`, and service checks on correspondence create/cancel) |
| Hiding buttons / route guards | Frontend (`CapabilitiesService`) — **UX only**; must not be the only control |

## 4. Decision: union of all active roles

- **Effective permissions** = union of permissions from **every** role assignment that is valid now (`valid_from` / `valid_to`) for active roles.
- There is **no** “active role only” filter for capabilities or navigation. The JWT still carries a **current role** for workflows and auditing (e.g. role switch), but capability and navigation resolution use the **union** model.

## 5. How to add a new permission

1. Insert a row into `permission` (code, names, `is_active`, optional `ui_screen_id`).
2. Grant it to the right roles in `role_permission`.
3. If it should gate a shell link, set `ui_screen.required_permission_id` (or add a row in `ui_screen`).
4. In Angular, use the same string in route `data.permission` and in templates: `cap.can('YOUR_CODE')`.

Administrative HTTP APIs should use `@PreAuthorize("@effectivePermission.has(authentication, 'your.code')")` so enforcement stays in the backend.

## 6. Scenario validation (expected behavior)

| Scenario | Expected |
|----------|----------|
| **USER only** | Capabilities and sidebar include only permissions granted to USER (e.g. dashboard + list if seeded that way). Routes without permission redirect to `/dashboard`; missing permission → `/login` on capability load failure. |
| **ADMIN only** | Full set of permissions assigned to ADMIN in DB; sidebar and `can()` align with those codes. |
| **USER + ADMIN** | Effective permissions = **union** (e.g. create/cancel appear if ADMIN grants them), regardless of which role is selected in the JWT for workflow. |
| **Direct URL without permission** | `permissionCanMatch` denies → **redirect to `/dashboard`** (or `/login` if capabilities cannot load). |
| **Menu vs route mismatch** | If a screen appears in the shell but the Angular route uses a **different** permission code than `ui_screen` + DB grants, the user may see the link but be redirected when opening the route (or the reverse). **Cause:** `data.permission` in `app.routes.ts` not aligned with `ui_screen.required_permission_id` / `permission.code`. Fix by aligning codes in DB and routes. |
