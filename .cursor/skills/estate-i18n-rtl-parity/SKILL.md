---
name: estate-i18n-rtl-parity
description: >-
  Property i18n/RTL conventions — default Arabic, ar.json/en.json key parity,
  Latin digits (ar-u-nu-latn), CDK overlay dir, bilingual notification segments.
  Use when adding languages, translating screens, fixing RTL layout, or porting
  Property lang toggle and i18n namespaces.
---

# Estate i18n + RTL Parity

**Reference:** `Property_Managments/property-frontend`  
- `src/app/core/i18n/i18n.service.ts`  
- `src/app/core/i18n/locale-format.ts`  
- `src/assets/i18n/en.json` + `ar.json`  

Combine with: `rms-premium-shell` (toggle UI), `rms-dates-property-style` (digits/dates).  
Shell owns the topbar flag menu — this skill owns **i18n rules**.

## Bootstrap

1. Default language **`ar`** (RTL).
2. Persist with app-specific key (`pm_lang` for Property).
3. On change, set `dir` on `html`, `body`, **and** CDK `OverlayContainer`.
4. Arabic `lang` attribute = `ar-u-nu-latn` (Western digits 0–9 always).

```ts
document.documentElement.setAttribute('dir', lang.dir);
document.documentElement.setAttribute('lang', code === 'ar' ? 'ar-u-nu-latn' : 'en');
overlayContainer.getContainerElement().setAttribute('dir', lang.dir);
```

## Key namespaces

| Prefix | Use |
|--------|-----|
| `APP.*` | Product name, tagline |
| `NAV.*` / `NAV_SECTION.*` | Sidebar |
| `COMMON.*` | Shared actions (Save, Cancel, Export…) |
| `STATUS.*` / `PRIORITY.*` | Badges |
| `LOV.*` | Searchable select dialog |
| `DIALOG.*` / `AUDIT.*` | Dialogs / audit strip |
| Feature `*_LIST` / `*_FORM` | Screen copy |

## Parity rule

Every new UI string → add to **both** `en.json` and `ar.json` with the **same key path**.  
Especially after security screens: `PERMISSIONS.*`, `LOOKUPS.*`, `PROFILE.*`, `USERS.*`.

**Zero raw keys in UI** — if the screen shows `PATIENTS.ACTIVE` or `COMMON.FOO`, the key is missing or a duplicate JSON namespace overwrote it. Never ship that.

### Shared filter labels

Use `COMMON.ALL`, `COMMON.ACTIVE`, `COMMON.INACTIVE` for list status filters — not feature-specific keys like `PATIENTS.ACTIVE`.

### Duplicate JSON namespaces (critical)

JSON allows only **one** top-level object per key. Duplicate `"AUDIT": { … }` blocks silently drop the first — merge into a single object.

## Validation (mandatory before sign-off)

Run from workspace root:

```bash
node scripts/validate-i18n.mjs "<frontend-root>"
```

Wire in each app `package.json`:

```json
"validate:i18n": "node ../../scripts/validate-i18n.mjs ."
```

Adjust relative path per repo layout. **Exit 0 required** — fix missing keys in both `ar.json` and `en.json`.

Checks:
- Static `| translate` / `instant('KEY')` / `*Key="KEY"` references exist in **both** locale files
- `ar.json` ↔ `en.json` key parity (no orphans)
- Strips UTF-8 BOM before parse (ERP files)

## Digits & formats

Use `I18nService.formatNumber` / `formatCurrency` / `formatDateTime` / `toLatinDigits` — never rely on Arabic-Indic digits in tables or counters.

CSS assist:

```scss
html[lang^='ar'], html[dir='rtl'] {
  font-variant-numeric: lining-nums tabular-nums;
}
```

## Bilingual stored text

Backend may store titles as `"ar | en"` and bodies as `ar\n\n—\n\nen`.  
Use `I18nService.pickBilingualSegment(text, 'title'|'body')` when rendering notifications.

## RTL polish

- Arabic-only fields: `dir="rtl"` even when session is LTR.
- Sidebar active rail flips with `[dir=rtl]`.
- Page-header back chevron + eyebrow tracking follow direction.
- Body font switches to `--font-ar` under RTL.

## Do not

- Hardcode Arabic or English strings in templates
- Ship `en.json` keys missing from `ar.json`
- Forget CDK overlay `dir` (dialogs stay LTR)
