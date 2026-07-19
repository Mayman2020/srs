---
name: estate-my-profile-page
description: >-
  Property my-profile page — hero avatar, bilingual name fields, readonly email,
  aside change-password card, identity media reuse, portal-aware routes. Use when
  building /admin/profile or role portal profile screens.
---

# Estate My Profile Page

**Reference:** `Property_Managments/property-frontend/src/app/features/profile/profile/`  
API: `user-profile.service.ts` + backend `USERS_ME` / change-password  

Combine with: `estate-identity-media-uploads`, `estate-detail-page-hero` (header), `estate-topbar-user-chrome` (menu entry), `estate-settings-security-roles` (must-change-password).

## Layout (≥960px)

```
app-page-header (breadcrumbs + smart back)
profile-layout
  profile-main
    hero card → avatar/initials, display name, email (readonly), role + username chips
    details card → form (names, phone, bio, optional identity media)
  aside
    security card → current / new / confirm password + Update Password
```

Below 960px: stack main then aside.

## Details form

| Field | Notes |
|-------|-------|
| `fullNameAr` | required, `dir="rtl"` |
| `fullNameEn` | required, `dir="ltr"` |
| Email | **readonly** + hint (`PROFILE.EMAIL_READONLY_HINT`) |
| Phone | editable (country affordance OK) |
| Bio | optional textarea |
| Media | `app-identity-media-fields` when role needs photo/civil |

Separate **save profile** submit from **change password** submit.

## Password card

- Independent `FormGroup`: currentPassword, newPassword, confirmPassword
- Min length ≥ 6 (or app policy); mismatch validation
- Distinct primary button (`PROFILE.UPDATE_PASSWORD`)
- Align with must-change-password flow when `mustChangePassword=true`

## i18n

Namespace `PROFILE.*` (TITLE, SUBTITLE, YOUR_DETAILS, SECTION_PROFILE, EMAIL_READONLY_HINT, PHOTO_BELOW_HINT, IMAGE_UPLOAD, UPDATE_PASSWORD, …).  
Keep `ar.json` / `en.json` parity.

## Display name rule

Same as topbar/sidebar: AR preferred when `lang=ar`, else EN.

## Do not

- Editable email without a verified change-email flow
- Raw file inputs instead of identity-media skill
- Mixing password fields into the main profile form submit
