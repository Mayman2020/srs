---
name: estate-os-design-tokens
description: >-
  Property Estate OS design tokens — brass/navy/paper palette, Instrument Serif
  display typography, motion easings, radii, dark-theme overlays. Use when
  porting Property visual language, creating design tokens, or fixing brand
  colors/typography across RMS apps.
---

# Estate OS Design Tokens

**Reference:** `Property_Managments/property-frontend/src/styles/estate-os-tokens.scss`  
**Bridge:** `platform-os-globals.scss` + `styles.scss`  
Combine with: `rms-premium-shell` (structure), `estate-card-system` (list chrome).

Do **not** reinvent shell layout here — tokens + typography only.

## Core palette

```scss
:root {
  --navy-900: #0e1f33;
  --navy-800: #14283f;
  --navy-700: #1b3553;
  --brass-600: #8e6a2b;
  --brass-500: #b48a40;
  --brass-400: #cca55b;
  --brass-100: #f4e7c4;
  --paper: #f6f2ea;
  --paper-2: #efeae0;
  --surface: #fffdf8;
  --ink-900: #0e1f33;
  --ink-500: #6b7f96;
  --line: #dfd7c7;
  --ok / --warn / --bad / --info (+ *-bg);
}
```

Primary CTA = navy background + brass text (`--btn-primary-bg` / `--btn-primary-color`).  
Focus / accent border = brass.

## Typography

| Role | Token | Family |
|------|-------|--------|
| Page titles, KPI values, dialog titles | `--font-display` | Instrument Serif (+ Playfair fallback) |
| Body / UI | `--font-ui` | Inter + system |
| Arabic body | `--font-ar` | IBM Plex Sans Arabic, Cairo |
| Codes / mono | `--font-mono` | JetBrains Mono |

Titles use `--font-display`; never Inter/Roboto for hero headings on Estate pages.

## Motion & density

```scss
--e-out: cubic-bezier(0.22, 1, 0.36, 1);
--e-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
--t-fast: 160ms; --t-med: 320ms; --t-slow: 520ms;
--r-xs…xl; --pad-card; --pad-row;
--sidebar-w: 248px; --sidebar-w-collapsed: 76px; /* Property uses 76, Restaurant skill uses 68 */
--topbar-height: 64px;
--content-max-w: none; /* full-width — never max-width:1400px + margin auto */
```

Prefer `fadeUp` / hover `translateY(-2px)` — not glow spam or purple AI defaults.

## Dark theme

Redefine paper/ink/line/status-bg on **all three**:

```scss
.dark-theme,
html.dark-theme,
.cdk-overlay-container.dark-theme { … }
```

Dark status backgrounds use translucent `rgba(...)`, not light-mode solid pastels.

## Full-width content

```scss
.app-page {
  width: 100%;
  max-width: var(--content-max-w); /* none */
  margin-inline: 0;
  padding: var(--content-pad-block) var(--content-pad-inline) 48px;
}
.main-content { width: 100%; min-width: 0; }
```

## Do not

- Purple-on-white / Inter-only default stacks for Estate brand
- Hardcode hex in feature SCSS when a token exists
- Leave dialogs light while `html` is dark (miss CDK overlay)
