---
name: rms-premium-shell
description: >-
  RMS/Restaurant premium UI shell — navy+gold login split, glowing sidebar icons (68px
  collapsed), full-width content, dark/light theme, Property lang toggle, icon buttons,
  Material dialogs, theme contrast. Use when upgrading Restaurant RMS UI, layout,
  login, topbar, sidebar, POS, or admin shell.
---

# RMS Premium Shell

Combine with: `estate-card-system`, `rms-dates-property-style`, `rms-fullstack-integration`.

## Brand palette (login-inspired)

- Navy: `#1e3a5f`, `#152a45`
- Gold: `#d4a853`, `#e8c878`
- Dark surfaces: `#121c2e` app, `#1a2740` cards
- Light surfaces: `#eef2f7` app, `#ffffff` cards

Tokens: `restaurant-os-tokens.scss`

## Login (split screen)

```
[Hero 45%]                    [White panel 55%]
- assets/images/login-hero.jpg  - Welcome Back + subtitle
- logo-cloche.svg               - username/password fields
- RESTAURANT / MANAGEMENT SYS   - remember me, forgot password
- tagline                       - navy Sign In button
- lang + theme toolbar          - optional social row
```

Files: `login.component.html/scss` — hero path relative in SCSS: `../../../../assets/images/login-hero.jpg`

## Topbar (like Property)

- Flag + language mat-menu (ar/en)
- Theme toggle (sun/moon) with gold glow
- Notifications badge + user menu
- Branch select from API (no fake fallback branch)

Files: `topbar.component.html`, `rms-premium-shell.scss`

## Sidebar

- Expanded: `--sidebar-w: 248px`
- Collapsed: `--sidebar-w-collapsed: 68px` — **must** set `.sidebar.collapsed { width: var(--sidebar-w-collapsed) }` in `app-shell.scss`
- Icon-only + `matTooltip` when collapsed
- Missing nav items to include: Delivery, Loyalty, Notifications
- Logout with icon + tooltip when collapsed

### Sidebar scroll (like Property)

Structure: `sidebar-brand` (fixed) → `sidebar-nav` (scroll) → `sidebar-logout` (fixed).

```scss
.sidebar {
  height: 100dvh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-brand,
.sidebar-logout {
  flex-shrink: 0;
}

.sidebar-nav {
  flex: 1 1 auto;
  min-height: 0;           /* required for flex child scroll */
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(212, 168, 83, 0.5) transparent;
}

.sidebar-nav::-webkit-scrollbar { width: 8px; }
.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(212, 168, 83, 0.42);
  border-radius: 999px;
}
```

Reference: `Property_Managments/.../sidebar.component.scss` (gold thumb on dark navy).
File: `restaurant-frontend/src/styles/layout/app-shell.scss`

## Sidebar icons (colorful + glowing)

`nav-icon-wrap` with `data-tone`: `purple | orange | cyan | rose | green | gold | indigo | teal | slate`

White icon on gradient chip + colored box-shadow.

## Full-width content (after sidebar collapse)

Remove `max-width: 1400px` centering on pages. Use tokens:

```scss
:root {
  --content-max-w: none;
  --content-pad-inline: 24px;
  --content-pad-block: 28px;
}
```

Apply to: `.app-page`, `.page-shell`, `.sf-dashboard`, `.rms-dashboard`

```scss
.app-page {
  width: 100%;
  max-width: var(--content-max-w);
  margin-inline: 0;
  padding: var(--content-pad-block) var(--content-pad-inline) 48px;
}
.main-content { width: 100%; min-width: 0; }
```

Floor map grid: `width: 100%` not `justify-content: center`.

## POS cards

- `pos-product-card` + `pos-product-icon[data-tone]`
- `category-chip` gold active
- `cart-total-box` navy+gold gradient border
- `estate-filter-btn` for order type / payment

## Theme

`ThemeService`: `dark-theme` / `light-theme` on html, body, overlay. Default dark. Persist `rms_theme`.

## Theme contrast (`rms-theme-contrast.scss`)

- Surface text: `var(--ink-900)` / `--ink-700` / `--ink-500`
- Dark mode: no black text on surfaces
- Light mode: no white text on surfaces
- White text only on accents: `var(--text-on-accent)` on navy buttons, nav chips, KPI icons

## Icon action buttons

`rms-icon-btn` — 42px rounded, tooltip, variants: default, primary, gold, warn.

Use in page headers instead of text buttons. Optional `routerLink`.

## Estate dialogs (Property-style)

- Panel: `app-dialog-panel` via `RmsDialogService`
- Header: navy gradient + gold title + `DialogTitleCloseDirective`
- Footer: Cancel + Save / Confirm delete
- Styles: `rms-dialog.scss` — form grid gap **14×16px** (`rms-dialog-form`), auto-grid for fields
- Form dialogs in `shared/dialogs/` — no inline add/edit forms on list pages
- Export `APP_DIALOG_IMPORTS` from `shared/dialog-ui.ts` for consistent dialog imports

```typescript
this.dialogs.open(CustomerFormDialogComponent, { width: '520px' })
  .afterClosed().subscribe(ok => { if (ok) this.load(); });
this.dialogs.confirmDelete('...').subscribe(ok => { if (ok) this.delete(id); });
```

## Smart back button (screen-to-screen only)

Do **not** show back when user entered via sidebar menu.

1. `NavigationHistoryService` — `markFromMenu()` on sidebar nav clicks
2. `PageHeaderComponent` — icon-only `rms-icon-btn` back in `page-actions` (opposite title side)
3. `Location.back()` when `navHistory.shouldShowBack()` is true
4. RTL: flip back chevron in SCSS

Files: `core/services/navigation-history.service.ts`, `shared/components/page-header/`

## Clinic premium visual (optional TV/queue aesthetic)

For Clinic admin screens matching `/queue/tv`:

- `clinic-premium-visual.scss` — navy-teal gradient, glass cards, gold/teal accents
- `clinic-theme-contrast.scss` — Material table dark fixes, stat pills
- Import in `styles.scss` after shell tokens

## Style import order (`styles.scss`)

```
restaurant-os-tokens → platform-os-globals → app-shell → sf-restro-dashboard
→ rms-estate-cards → list-pages → rms-premium-shell → material dark/light
→ rms-theme-contrast → rms-dialog
```

## Visual audit

```bash
node scripts/visual-audit-all.mjs restaurant
```

Output: `restaurant-frontend/ui-review/`
