---
name: estate-identity-media-uploads
description: >-
  Property identity media UX — profile photo + civil ID cards, view/download/
  remove/replace toolbar, upload-zone for documents. Use when adding person
  forms (owners, tenants, employees, contractors, profile) or file uploads.
---

# Estate Identity Media Uploads

**Reference:** `Property_Managments/property-frontend/src/app/shared/components/`  
- `identity-media-fields/identity-media-fields.component.ts` → `app-identity-media-fields`  
- `upload-zone/upload-zone.component.ts` → `app-upload-zone`  

Used in owner / tenant / employee / contractor dialogs and profile.

## Person media pair

```html
<app-identity-media-fields
  [profileImageUrl]="form.value.profileImageUrl"
  [civilIdImageUrl]="form.value.civilIdImageUrl"
  (profileImageUrlChange)="form.patchValue({ profileImageUrl: $event })"
  (civilIdImageUrlChange)="form.patchValue({ civilIdImageUrl: $event })">
</app-identity-media-fields>
```

- Profile = round thumb  
- Civil ID = card aspect  
- Optional `compact` / hide civil section via inputs

## Action toolbar (when media exists)

Icon-only + tooltip (i18n):

| Action | Icon |
|--------|------|
| View (new tab) | `open_in_new` |
| Download | `download` |
| Remove | `delete_outline` |
| Replace | `drive_folder_upload` (+ spinner while uploading) |

Empty state = placeholder icon + single `add_a_photo` / add affordance.

## General files

Use `app-upload-zone` for documents / multi-file:

- `layout="identity"` for compact cards beside forms  
- Controlled mode: `[urlList]` + `(urlListChange)`  
- Detect type IMAGE / VIDEO / DOCUMENT  
- Drag-over highlight on strip  
- Upload via shared `ApiService` helper; snack on failure

## Rules

1. Person entities → identity-media-fields, not raw `<input type="file">`.
2. Show spinner on replace; disable concurrent clicks.
3. Accept `image/*` for profile/ID; broader types for upload-zone.
4. Never leave broken URLs silent — snack on upload error.

## Do not

- One-off upload UIs per dialog
- Missing remove/replace when preview exists
