---
name: estate-entity-audit-trail
description: >-
  Property admin audit strip (معلومات التدقيق) — history icon, created/modified
  by+at, optional approved-by, DTO name resolution, hide when empty. Mandatory on
  edit dialogs and detail pages for persisted entities. Not the audit-log page.
---

# Estate Entity Audit Trail

**Mandatory for admin UX:** every edit dialog / detail of a saved entity shows the read-only strip **معلومات التدقيق / Audit Information** (history icon) — نفس البلوك اللي بيظهر تحت بيانات السجل للأدمن.

**Reference:**  
`Property_Managments/property-frontend/src/app/shared/components/audit-trail/audit-trail.component.ts` → `app-audit-trail`

Used across: users, owners, tenants, units, properties, contractors, employees, inventory, legal entities, maintenance, complaints, contracts, …

Combine with: `rms-dates-property-style` (dd/MM/yyyy HH:mm), `estate-i18n-rtl-parity` (`AUDIT.*`), dialog layout from `rms-premium-shell`.

## What the admin sees

```
history  معلومات التدقيق
تم الإنشاء بواسطة     | تاريخ الإنشاء
Super Administrator   | 05/06/2026 18:59
تم التعديل بواسطة      | تاريخ آخر تعديل
Super Administrator   | 05/06/2026 18:59
تم الاعتماد بواسطة     (full width, when present)
Super Administrator
```

Do **not** reinvent per-screen footnotes — one shared component.

## Not the system audit log

| Feature | Skill / route |
|---------|----------------|
| Entity footer: who created/updated/approved **this row** | **This skill** (`app-audit-trail`) |
| Global event history `/admin/audit-log` | Separate list screen (action stream) — optional later skill |

## Component API

```html
<app-audit-trail
  *ngIf="record?.id"
  [createdBy]="record.createdBy"
  [createdByName]="record.createdByName"
  [createdAt]="record.createdAt"
  [modifiedBy]="record.modifiedBy ?? record.updatedBy"
  [modifiedByName]="record.modifiedByName ?? record.updatedByName"
  [updatedAt]="record.updatedAt ?? record.modifiedAt"
  [approvedBy]="record.approvedBy"
  [approvedByName]="record.approvedByName">
</app-audit-trail>
```

| Input | Rule |
|-------|------|
| `createdBy` / `createdByName` | Prefer **name**; fallback `#id` then `—` |
| `createdAt` | Format `dd/MM/yyyy HH:mm` (Property builds it inside the component; verticals may use `rmsDate`) |
| `modifiedBy` / `modifiedByName` + `updatedAt` | Same pairing |
| `approvedBy` / `approvedByName` | Only for workflows with approval; wide row; if id without name → `AUDIT.UNKNOWN` |

`*ngIf="hasAny"` inside component: **no empty bordered box**. Hide entire host when creating (`!id`).

## Placement (always)

1. **Edit dialogs** — after form fields, inside scroll body (before sticky footer actions).  
2. **Detail pages** — near bottom / after main sections (hero stays above).  
3. Read-only — never inputs, never editable.  
4. Same strip for SUPER_ADMIN and other roles who can open the record (visibility = open record, not a separate “audit permission” for the strip).

## Backend contract (required)

Persist on auditable tables:

- `created_by`, `created_at`
- `updated_by` / `modified_by`, `updated_at`
- optional `approved_by`, `approved_at`

On **every detail/edit DTO**, resolve display names:

```java
.createdByName(resolveUserName(entity.getCreatedBy()))
.modifiedByName(resolveUserName(entity.getUpdatedBy()))
.approvedByName(resolveUserName(entity.getApprovedBy())) // if applicable
```

Without names the strip shows `#42` — works but looks unfinished; Prefer full names (`Super Administrator`).

Set actors from security context on create/update (and approval transitions).

## i18n (`AUDIT.*`)

| Key | AR (Property) | EN |
|-----|----------------|-----|
| `INFO_TITLE` | معلومات التدقيق | Audit Information |
| `CREATED_BY` | تم الإنشاء بواسطة | Created By |
| `CREATED_AT` | تاريخ الإنشاء | Created At |
| `MODIFIED_BY` | تم التعديل بواسطة | Last Modified By |
| `UPDATED_AT` | تاريخ آخر تعديل | Last Modified |
| `APPROVED_BY` | تم الاعتماد بواسطة | Approved By |
| `UNKNOWN` | (system) | System |

Keep both `ar.json` and `en.json`.

## Port checklist (new dialog)

1. Copy/port `AuditTrailComponent` once into `shared/components/audit-trail`.  
2. Ensure API returns `*ByName` + timestamps.  
3. Drop `<app-audit-trail *ngIf="entity?.id" …>` at end of edit content.  
4. Verify empty create mode has no strip; saved edit shows created + modified.  
5. Wire `approvedByName` only where approval exists.

## Do not

- Custom “أنشئ بواسطة …” text under each form  
- Show the strip with all empty placeholders  
- Use Angular `date` pipe here if project uses RMS dd/MM (Property’s component already formats)  
- Confuse this strip with `/admin/audit-log`
- Skip the strip on “simple” admin dialogs — **default is include it**
