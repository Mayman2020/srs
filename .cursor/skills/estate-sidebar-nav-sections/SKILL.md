---
name: estate-sidebar-nav-sections
description: >-
  Property sidebar sections — collapsible NAV_SECTION groups, brass active glow,
  ambient light, brand mark, centered collapsed logo (no chevron beside mark),
  user footer strip, collapse/expand, nav badges, markFromMenu on click. Use when
  upgrading sidebar chrome beyond basic shell.
---

# Estate Sidebar Nav Sections

**Reference:** `Property_Managments/property-frontend/src/app/layout/sidebar/`  
Combine with: `rms-premium-shell` (widths, scroll, glow icon tones), `estate-i18n-rtl-parity`.

Shell owns 248 / collapsed width + scroll structure. This skill owns **sections + footer identity + collapse UX**.

## Structure

```
sidebar-brand (logo + APP.NAME / TAGLINE_SHORT + collapse chevron when expanded)
sidebar-nav
  for each visible section:
    nav-section-toggle (label NAV_SECTION.* + expand_less/more)  // hidden when collapsed
    nav-section-items → nav-item links
sidebar-user (avatar + name + ROLE.*)                              // strip hides labels when collapsed
sidebar-logout
```

## Collapse / expand (“قفل وفتح” UX)

Property implements **toggle collapse** (not a pin-lock):

| State | Behavior |
|-------|----------|
| Expanded | Labels, section toggles, badges, user strip text, logout label; collapse chevron visible in brand row |
| Collapsed | Icon-only items + tooltips; hide section toggles/badges/user text; logout icon-only |
| Toggle | Topbar `menu` / `menu_open` always; brand chevron **only when expanded** |

Always call `NavigationHistoryService.markFromMenu()` on nav click (smart back).

## Sidebar widths (mandatory — Property scale)

| Token | Value | Role |
|-------|-------|------|
| `--sidebar-w` / `--sidebar-width` | **248px** | Expanded |
| `--sidebar-w-collapsed` / `--sidebar-collapsed-width` | **76px** | Collapsed (icon rail) |

**Do not use 68px** for collapsed — it looks cramped next to 36–40px icon pills (Clinic/Vzeeta bug). Alias both naming styles to the same numbers:

```scss
:root {
  --sidebar-w: 248px;
  --sidebar-w-collapsed: 76px;
  --sidebar-width: var(--sidebar-w);
  --sidebar-collapsed-width: var(--sidebar-w-collapsed);
}
```

Keep expanded at ~248px (Property). Verticals may keep a slightly wider expanded (e.g. 260px) **only if** product already ships that; collapsed **must** stay **76px** everywhere for parity.

**Icon scale on collapsed rail:** glowing `.nav-icon-wrap` pills stay **38×38** (not 34px). Shrinking icons for a 68px rail made the menu look tiny; with 76px there is room — keep full scale.

## Brand mark when collapsed (mandatory)

Like Property — **logo centered, no arrow beside it**:

1. When `.sidebar.collapsed`: **hide** `.collapse-btn` (`display: none`). Do not leave a chevron next to the logo.
2. Center the brand row: `.sidebar-brand { justify-content: center; gap: 0; }` and `.brand-icon { margin-inline: auto; }`.
3. Hide `.brand-text` when collapsed (name/tagline only when expanded).
4. Re-expand via the **topbar** hamburger (`menu` / `menu_open`) — not a sidebar chevron stuck beside the mark.
5. Keep each app’s own logo asset (NABD, Clinic cross, etc.) — do not copy Property sunburst into other brands.

```scss
.sidebar.collapsed .sidebar-brand {
  justify-content: center;
  gap: 0;
}
.sidebar.collapsed .brand-icon {
  margin-inline: auto;
}
.sidebar.collapsed .collapse-btn {
  display: none;
}
```

Optional HTML pattern (same effect):

```html
<button *ngIf="!collapsed" class="collapse-btn" type="button" (click)="collapseToggle.emit()">
  <span class="material-icons">{{ collapsed ? 'chevron_right' : 'chevron_left' }}</span>
</button>
```

## Section chrome (“نور المنيو”)

1. Collapsible `NAV_SECTION.*` groups; accent on section labels (`::before`) — remap brass to app tokens on verticals.
2. Active item: accent gradient fill + icon `drop-shadow` glow + start rail (flip for RTL).
3. Ambient: soft radial accent glow on `.sidebar::before` — restrained, not neon spam.
4. `nav-badge` counts when expanded, permission-filtered sections only.

## Brand + footer

- Brand mark + translated name/tagline when expanded (no hardcoded English).
- User footer mirrors topbar identity (avatar/photo or initials + bilingual display name + role).
- Logout uses `AUTH.LOGOUT`.

## Do not

- Flat unsectioned 30-item lists
- Skip `markFromMenu` on sidebar routes
- Leave active rail LTR-only in Arabic
- Invent a “locked sidebar” unless the product actually persists pin state
- Show collapse chevron next to the logo while the sidebar is collapsed
- Off-center the logo in collapsed mode
