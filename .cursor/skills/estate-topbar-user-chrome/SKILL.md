---
name: estate-topbar-user-chrome
description: >-
  Property topbar composition — menu toggle, property-scope LOV, lang flag menu,
  theme, role switcher, notify badge, user chip + profile/logout menu. Use when
  building or porting admin header chrome beyond basic shell notes.
---

# Estate Topbar User Chrome

**Reference:** `Property_Managments/property-frontend/src/app/layout/topbar/`  
Combine with: `rms-premium-shell`, `estate-i18n-rtl-parity`, `estate-lov-picker-system`, `estate-in-app-notifications`.

Shell owns sticky bar + theme service; this skill is the **concrete control order** and user/role menus.

## Control order (RTL/LTR flex)

```
[start] menu toggle
[end]   property-scope LOV? → language (flag+label) → theme → role switcher → notify bell → user chip
```

| Control | Behavior |
|---------|----------|
| Menu toggle | `menu` / `menu_open` icons; emits `sidebarToggle` |
| Property scope | `app-estate-lov-select` when role is property-scoped |
| Language | Flag + `nativeLabel`; mat-menu of `I18nService.languages` |
| Theme | sun/moon; `ThemeService.toggle()`; tooltip LIGHT/DARK |
| Role | `ROLE.*` label; switch via `getEffectiveRoles()` → reload permissions + home |
| Notify | Badge + navigate inbox (see notifications skill) |
| User | Avatar/photo or initials + display name + role |

## User menu panel

```
header: avatar | displayName | role chip | email
divider
→ Profile (portal-aware route)
→ Logout
```

Display name: prefer AR name when lang=ar else EN (same rule as sidebar/profile).

## Profile route by portal

Map `auth.getRole()` → `/admin/profile` | `/owner/profile` | `/tenant/profile` | … — never hardcode one path for all roles.

## Role switch

1. Menu lists switchable roles; active role shows check.
2. On switch: persist active role → `PermissionService.loadMine()` → navigate dashboard.
3. Labels from `ROLE.*` i18n only.

## Do not

- Drop role/profile into sidebar only and leave topbar empty of identity
- Forget unread badge sync when adding a custom notify menu
- Put long labels without `lang-name` collapse at ≤900px
