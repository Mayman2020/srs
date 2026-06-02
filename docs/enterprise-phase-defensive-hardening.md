# Enterprise Phase — Defense-Grade Correspondence & Operational Continuity

This document defines a **final enhancement phase** on top of the completed enterprise
transformation (modular monolith, scoped security, confidentiality, Camunda Q/L/K/S routing,
reporting scope, Prometheus, i18n gates, documentation). It does **not** replace prior work; it
extends the same standards: Flyway-only schema changes, lookup tables instead of PostgreSQL
`ENUM`s, canonical permission codes with `@PreAuthorize`, DTO-only APIs, immutable audit
append, and government-grade deny-by-default.

**Relationship to existing capabilities**

| Area | Already in SRS | This phase extends |
|------|----------------|----------------------|
| Delegation | `authority_delegation` + escalation reassignment to delegate | Time-bounded **task** delegation, explicit audit of actor vs delegator, UI |
| Leave | Leave requests | **Acting manager** role binding + routing resolution |
| Attachments | Storage key + versioning + clearance on correspondence view | **Encryption at rest**, classified download gate, view audit |
| Templates | `letter_template` (type-linked HTML) | **Correspondence document templates** with placeholders + admin library |
| SLA | Due dates, boundary timers, escalation job, Micrometer counters | **Policy engine** driving timer expressions + matrix escalation |
| Notifications | In-app inbox, email/SMS dispatch APIs | Preferences, WebSocket, retries, task push |
| Read state | Partial patterns (e.g. circular `first_read_at`) | **Correspondence read/ack** analytics |
| QR / archive | Barcode on correspondence (optional) | **Print verification** + **retention / legal-hold** model |

---

## 1. Delegation system (workflow task delegation)

> **Status — DELIVERED (Slice 2).** Implemented in Flyway V15 (`task_delegation`,
> `TASK_DELEGATION_MANAGE_OWN`, `TASK_DELEGATION_ADMIN`),
> `feature.delegation.task` Java package
> (`TaskDelegationEntity`, `TaskDelegationRepository`, `TaskDelegationService`,
> `TaskDelegationController`, `TaskDelegationAssignmentResolver`, `TaskDelegationExpiryJob`),
> `CorrespondenceTaskAssignmentListener` and `RoutingStopAssignmentListener` overlay,
> `CorrespondenceWorkflowTaskPersistenceService` chain capture into `workflow_history.detail`,
> `WorkflowTaskInboxService` "acting as delegate" flag, audit events
> (`TASK_DELEGATION_CREATED`, `TASK_DELEGATION_REVOKED`, `TASK_DELEGATION_EXPIRED`,
> `TASK_ACTED_UNDER_DELEGATION`), frontend `app-task-delegations` screen with
> outgoing/incoming/inactive tabs, and inbox "Acting for" badge. Tests:
> `TaskDelegationServiceTest` (13), `TaskDelegationAssignmentResolverTest` (3); ArchUnit
> coverage extends automatically to the new package. Runbook §8 documents the new permissions,
> forensic queries, and operational guarantees.

### Business rules

- A user may delegate **specific Camunda tasks** (or a delegation scope: type + confidentiality
  band) to another user for a bounded window `[valid_from, valid_to]`.
- Delegation does not transfer ownership of the correspondence; it only affects **assignment /
  candidate resolution** for open tasks within the window.
- When delegation expires, the next poll / task listener **reverts assignee** to the original
  owner unless the task was already completed or reassigned by policy.
- A user cannot delegate above their own clearance or outside their permitted correspondence types
  (reuse / tighten `authority_delegation` constraints).

### Backend architecture

- New feature slice: `feature.delegation.task` (or extend `feature.delegation`) with:
  `TaskDelegationEntity`, `TaskDelegationRepository`, `TaskDelegationService`,
  `TaskDelegationController` (admin + self-service where policy allows).
- Integrate with `WorkflowTaskInboxService` and Camunda `TaskService` query filters: union of
  own tasks + tasks delegated **to** me; exclude tasks delegated **away** from my primary inbox
  unless I am the delegator (supervisor view).
- Reuse `WorkflowEscalationScheduler` patterns: scheduled job `DelegationExpiryJob` sets Camunda
  assignee back and emits notification.

### Frontend UX

- “Delegate this task” action on task row / correspondence workspace (date range picker,
  delegate user search, optional comment).
- Delegation management screen: active / upcoming / expired; revoke before `valid_to`.
- Inbox badge: “Acting for: N users” vs “Delegated out: M tasks”.

### Camunda / workflow

- **Task listener** on `assignment` / `create`: resolve `effectiveAssignee` from delegation
  table (highest priority rule wins: explicit task delegation > authority delegation > default).
- Process variables: `delegationChainJson` optional snapshot for audit.
- BPMN: no diagram rewrite if resolution is **listener + Java delegate**; optional explicit
  sub-process for “delegation approval” if policy requires witness approval.

### DB schema / migrations (Flyway)

- `task_delegation` (id, delegator_user_id, delegate_user_id, valid_from, valid_to, scope_type
  `TASK|TYPE_CONFIDENTIALITY`, correspondence_id nullable, camunda_task_id nullable,
  process_instance_id nullable, revoked_at, revoked_by, notes, audit columns).
- Partial indexes on `(delegate_user_id, valid_from, valid_to)` where `revoked_at IS NULL`.
- Optional FK to `authority_delegation` if a task delegation is **linked** to an existing
  authority delegation record.

### APIs

- `POST /api/v1/delegations/tasks` — create (body: delegate, window, scope).
- `DELETE /api/v1/delegations/tasks/{id}` — revoke.
- `GET /api/v1/delegations/tasks/mine` — incoming/outgoing active delegations.

### Security

- `@PreAuthorize` on mutating endpoints; delegator must match current user or hold
  `DELEGATION_MANAGE` (or new `TASK_DELEGATION_ADMIN`).
- Prevent self-delegation loops and cross-tenant (department boundary) delegation unless HQ role.

### Audit / history

- `audit_event`: `TASK_DELEGATION_CREATED`, `TASK_DELEGATION_REVOKED`, `TASK_ACTED_UNDER_DELEGATION`
  with JSON `{delegatorId, delegateId, taskId, correspondenceId}`.
- Extend `workflow_history` payload or link `workflow_action` comment to delegation id.

### Notifications

- Notify delegate on creation; notify delegator on task completion under delegation; remind both
  T-24h before expiry.

### Production

- Clock skew: use DB `timestamptz` + UTC; idempotent expiry job; rate-limit delegation create API.

---

## 2. Acting manager support

> **Status — DELIVERED (Slice 4).** Implemented in Flyway V17 (`acting_assignment`,
> `ACTING_ASSIGNMENT_VIEW`, `ACTING_ASSIGNMENT_MANAGE_OWN`, `ACTING_ASSIGNMENT_ADMIN`,
> `ui_screen` `acting_assignments → /acting-assignments`), Java package
> `com.gov.ac.feature.acting` (`ActingAssignmentEntity`, `ActingAssignmentRepository`,
> `ActingAssignmentMatcher`, `ActingAssignmentService`, `ActingAssignmentController`,
> `ActingAssignmentExpiryJob`, `ActingAssignmentReconciliationJob`), integration in
> `TaskDelegationAssignmentResolver` (**precedence:** workflow direct assignee → acting overlay
> when `SlaClearanceFilter` passes → task delegation on the post-acting assignee), extended
> `CorrespondenceWorkflowTaskPersistenceService` / `WorkflowTaskInboxService` / inbox DTO fields,
> `UserRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing` for permission union,
> Angular screen `acting-assignments` and inbox “Acting manager” badge. Tests include updated
> `TaskDelegationAssignmentResolverTest` and `EffectiveUserPermissionServiceTest`; runbook §8
> documents forensic queries and jobs.

### Business rules

- While user A is absent (or on leave), user B may be registered as **acting manager** for a
  bounded `[valid_from, valid_to]` window with optional scope (department + subtree, org level,
  correspondence type, confidentiality, workflow action type, process/task definition keys).
- B receives Camunda user tasks that would otherwise be assigned to A when the row matches and B
  is cleared for the correspondence. B’s effective permissions **union** with A’s when the acting
  row’s department scope is open or matches B’s department (`EffectiveUserPermissionService`).
- Acting assignment ends on admin revoke, user revoke (absent party or admin), automatic expiry
  when `valid_to` passes, or reconciliation when task locals are stale.
- Cannot stack two active rows for the same absent user and department scope (partial unique index).

### Backend architecture

- `ActingAssignmentMatcher` picks the highest-specificity active row for absent user + task context.
- `TaskDelegationAssignmentResolver` applies acting before task delegation; stores task locals
  `ACTING_ASSIGNMENT_ID`, `ACTING_FOR_ABSENT_USER_ID`, `ACTING_MANAGER_USER_ID`,
  `WORKFLOW_DIRECT_ASSIGNEE_USER_ID`.
- Jobs: `ActingAssignmentExpiryJob` marks overdue rows; `ActingAssignmentReconciliationJob` scans
  active tasks (bounded), clears stale acting locals, resets assignee to absent, reapplies task
  delegation overlay via `applyTaskDelegationOverlayWithTaskService`.

### Frontend UX

- `/acting-assignments`: tabs for coverage as absent / as acting / upcoming / inactive; audit tab
  when `ACTING_ASSIGNMENT_VIEW`. Create form when `MANAGE_OWN` or `ADMIN`.
- Workflow task inbox: “Acting manager” badge when `actingAsManager` is true (distinct from task
  delegation “Acting for”).

### Camunda / workflow

- Resolution at task `create` inside existing assignment listeners — no BPMN diagram change required.

### DB schema / migrations

- `acting_assignment` — see V17 for full column list and indexes.

### APIs

- `GET /api/v1/acting-assignments/mine`, `GET /api/v1/acting-assignments/audit` (VIEW),
  `POST /api/v1/acting-assignments`, `DELETE /api/v1/acting-assignments/{id}`.

### Security

- Class-level `isAuthenticated()` on controller; mutating methods require `MANAGE_OWN` or `ADMIN`;
  audit list requires `VIEW`. Non-admin create requires `absent_user_id = current user`.

### Audit / history

- `ACTING_ASSIGNMENT_CREATED|REVOKED|EXPIRED|USED` plus `workflow_history.detail` acting keys.

### Production

- Tune `ac.acting-assignment.expiry-poll-ms`, `reconciliation-poll-ms`, `reconciliation-max-tasks`.

---

## 3. Classified attachments

> **Status — DELIVERED (Slice 5).** Implemented in Flyway V18 (`attachment_version.encryption_*`,
> `attachment_download_token`, three new permission codes). Backend lives under
> `com.gov.ac.feature.attachment.crypto` (AES-256-GCM with the `KeyProvider` SPI) and
> `com.gov.ac.feature.attachment.download` (single-use token issuance / consumption). Frontend
> in `transaction-details.ts/.html` (classified badge + intent-token flow). The legacy
> `GET /api/v1/attachments/{id}/download` endpoint stays for one release as a deprecation surface
> (still clearance-checked, still decrypts).

### Business rules

### Backend architecture

- `ClassifiedAttachmentService`: encrypt on upload (AES-256-GCM with per-tenant or per-file CEK
  wrapped by KMS/HSM key in prod; dev key from env).
- Separate `storage_key` namespace: `classified/{correspondenceId}/{versionId}.enc`.
- `AttachmentController`: stream endpoint checks `CorrespondenceViewAuthorization` **and**
  attachment classification level ≤ user clearance (reuse sort_order rule).

### Frontend UX

- Classified attachment row: “Request download” → polling or WebSocket when approved (if
  dual-control required); otherwise immediate download with progress.
- Disable thumbnail preview for classified.

### Camunda / workflow

- Optional user task “Release classified annex” before external send — only if policy demands.

### DB schema / migrations

- `attachment_version`: add `encryption_key_id`, `content_classification_id` FK nullable,
  `integrity_sha256` of ciphertext.
- `attachment_access_log` (id, attachment_version_id, user_id, action `VIEW_METADATA|DOWNLOAD`,
  occurred_at, ip, user_agent, success boolean).

### APIs

- `POST /api/v1/attachments/{id}/download-intent` — returns 202 + `intentId` if async approval.
- `GET /api/v1/attachments/download/{token}` — single-use JWT or opaque token (short TTL).

### Security

- Tokens bound to user + attachment version + jti; Content-Disposition attachment; CSP headers
  for any viewer microservice.

### Audit / history

- Every successful download appends `audit_event` + row in `attachment_access_log`.

### Notifications

- Notify correspondence owner on classified download from non-owner.

### Production

- KMS integration (AWS KMS, Azure Key Vault, or HashiCorp Vault transit); key rotation runbook;
  virus scan before encrypt (ClamAV sidecar).

---

## 4. Digital signature integration (PKI-ready)

> **Status — DELIVERED (Slice 5).** Implemented in Flyway V18 (`document_signature`,
> `workflow_action_type.requires_signature`). Backend in
> `com.gov.ac.feature.attachment.signature` (entity + repo, `SigningKeyProvider` SPI with an
> env-loaded ED25519 keypair as the dev default, `DocumentSignatureService` for
> create / verify / revoke, controllers under `/api/v1/attachments/{id}/signatures` and
> `/api/v1/signatures/{id}`). A stable verifier endpoint at
> `GET /api/v1/verify/attachment-versions/{id}` is reserved for the upcoming QR/print readouts.
> Workflow enforcement is wired into `CorrespondenceWorkflowActionService` — actions with
> `requires_signature=true` reject `completeActiveAssigneeTask` until the actor has a
> VALID + VERIFIED signature on every active attachment.

### Business rules

### Backend architecture

- `CorrespondenceSignatureEntity` (correspondence_id, workflow_action_id nullable, signer_user_id,
  payload_hash, signature_algorithm, raw_signature bytea or text, cert_chain text, verified_at,
  external_provider nullable).
- `SignatureVerificationService` interface + `NoOpSignatureVerificationAdapter` (dev) +
  `PkiSignatureVerificationAdapter` (future).
- Extend `CorrespondenceWorkspaceDto` with `signatures[]` and `pendingSignatureRequirement`.

### Frontend UX

- Sign dialog: “Sign and submit” with client-side hash preview (i18n); integrate later with
  national PKI browser plugin or server-side HSM signing service.

### Camunda / workflow

- Intermediate message catch or user task `SignDocument` before gateway; timer boundary if
  signature not received.

### DB schema / migrations

- Table above + optional `signature_policy` lookup (which types require sign).

### APIs

- `POST /api/v1/correspondence/{id}/signatures` — submit signature package.
- `GET /api/v1/correspondence/{id}/signatures` — list for auditors.

### Security

- Non-repudiation: sign hash of server-stored canonical JSON/HTML snapshot id, not user-edited
  HTML from browser alone.

### Audit / history

- Immutable `workflow_history` event `SIGNATURE_RECORDED`; tamper-evident linkage to attachment
  version.

### Notifications

- Notify next approver when signature verified.

### Production

- OCSP/CRL checks in PKI adapter; clock sync (NTP); HSM network policy.

---

## 5. Correspondence templates (document generation)

### Business rules

- Admin defines templates per `correspondence_type` + optional `classification` / language
  (`ar`, `en`, `bilingual`).
- Placeholders map to workspace fields and lookups (Mustache or Handlebars — align with
  existing `letter_template` approach).
- Versioning: publish / draft; only one **published** version per (type, language) tuple.

### Backend architecture

- Either extend `letter_template` with `template_kind` `LETTER|CORRESPONDENCE_BODY` or new table
  `correspondence_document_template` with `body_html`, `placeholder_schema_json`.
- `CorrespondenceTemplateRenderService` used by create wizard preview and PDF export.

### Frontend UX

- Admin: template editor with placeholder picker; preview with sample data.
- Create wizard: “Apply template” dropdown.

### Camunda / workflow

- Service task `RenderFinalPdf` optional before archive (async job).

### DB schema / migrations

- Columns / tables for versioning, `published_at`, `published_by`.

### APIs

- CRUD under `ADMIN_LOOKUP_MANAGE` or new `CORRESPONDENCE_TEMPLATE_MANAGE`.
- `POST /api/v1/correspondence-templates/{id}/preview` with sample DTO.

### Security

- Sanitize HTML server-side (OWASP Java HTML Sanitizer); CSP for preview iframe.

### Audit / history

- `TEMPLATE_PUBLISHED`, `TEMPLATE_USED_ON_CREATE`.

### Notifications

- None mandatory; optional notify subscribers on template change.

### Production

- Store large bodies in object storage with checksum; DB holds metadata only.

---

## 6. SLA policy engine

> **Status — DELIVERED (Slice 3).** Implemented in Flyway V16 (`sla_policy`,
> `sla_escalation_step`, `sla_breach_event` + the `SLA_POLICY_VIEW` / `SLA_POLICY_MANAGE`
> permissions and a `sla_policies → /sla-policies` shell-nav entry). The engine is driven by
> `SlaPolicyEvaluationJob`, which runs every `ac.sla.evaluation-poll-ms` ms (default 60 000),
> resolves the most specific active policy via `SlaPolicyResolverService` (specificity = one
> point per non-null criterion in `correspondence_type / priority / confidentiality /
> org_level_code / workflow_action_type`, ties broken by primary key), and fires every step
> in `sla_escalation_step` whose `delay_after_breach_minutes` has elapsed and whose
> `step_order` is greater than `sla_breach_event.last_step_executed_order`. Idempotency is
> backed by the unique index `ux_sla_breach_event_task` on `task_id`; resolution is
> reconciled per tick by detecting `sla_breach_event` rows whose Camunda task has left the
> active set and stamping them `resolved_at = now()` with outcome `TASK_NO_LONGER_ACTIVE`.
>
> `SlaEscalationService` implements the four escalation actions
> (`NOTIFY_MANAGER` / `REASSIGN_TO_DELEGATE` / `ESCALATE_TO_HIGHER_LEVEL` /
> `NOTIFY_AUDIT_ADMIN`). Every candidate recipient is filtered through
> `SlaClearanceFilter`, which mirrors `CorrespondenceViewAuthorization.assertClearance` so
> confidentiality is never bypassed; a `REASSIGN_TO_DELEGATE` whose delegate is not cleared
> is logged as a no-op so the next step still runs. Notifications use a dedicated
> `SlaNotifier` (REQUIRES_NEW transaction, in-app channel; reuses the `OVERDUE` notification
> event type to stay additive). The legacy `WorkflowEscalationScheduler` is intentionally
> untouched and continues to handle unassigned-task fan-out alongside the engine.
>
> Observability extends the existing counter: `correspondence_sla_breach_total{outcome="…"}`
> now carries `breach_detected / task_no_longer_active / task_completed` in addition to
> `unassigned / reassigned_to_delegate`. New metrics:
> `correspondence_sla_escalation_total{action="…"}` (counter, one per step fire) and
> `correspondence_sla_overdue_active` (gauge backed by unresolved breach rows). REST surface:
> `GET/POST/PUT/DELETE /api/v1/admin/sla/policies` (gated by `SLA_POLICY_VIEW` /
> `SLA_POLICY_MANAGE`), `GET /api/v1/admin/sla/breaches?onlyActive=true|false` (gated by
> `SLA_POLICY_VIEW`), and `GET /api/v1/sla/tasks/{taskId}/status` (`isAuthenticated()` —
> protected by the inbox visibility rule the caller already satisfies to know the `taskId`).
>
> Frontend: an admin screen at `/sla-policies` (`SlaPoliciesComponent`) for CRUD + breach
> review, a per-row SLA chip + "show breached only" filter on the workflow task inbox, and
> `sla.*` translation keys in `ar.json` and `en.json`. Tests cover policy resolution,
> escalation routing, clearance preservation, metric registration, and job idempotency
> (`SlaPolicyResolverServiceTest`, `SlaEscalationServiceTest`, `SlaClearanceFilterTest`,
> `SlaMetricsTest`, `SlaPolicyEvaluationJobTest`). An ArchUnit rule
> (`slaAdminControllersCarryClassLevelPreAuthorize`) pins class-level `@PreAuthorize` on the
> SLA admin controllers.

### Business rules

- SLA = f(`correspondence_type`, `priority`, `confidentiality`, optional `owner_department.level`).
- Policies define: **first response**, **resolution**, **per-routing-stop** budgets, escalation
  matrix (who to notify / reassign after breach level 1 vs 2).
- Timers in Camunda must be computed from policy rows, not hardcoded ISO durations in BPMN.

### Backend architecture

- `sla_policy`, `sla_policy_rule` (match dimensions with NULL = wildcard), `sla_escalation_step`
  (order, action `NOTIFY|REASSIGN|ESCALATE_ROLE`, target).
- `SlaPolicyResolver` produces `Duration` per routing stop → injected into process variables at
  `RoutingChainDelegate` / dedicated `SlaInjectionDelegate` before multi-instance.

### Frontend UX

- Admin SLA matrix UI; simulation “what SLA applies to this draft?”

### Camunda / workflow

- Replace static `${routingStop.slaIso}` with variable from delegate; boundary timer uses
  `PT${slaSeconds}s` or ISO-8601 from resolver.
- Incident on `sla_breach_level` increment for reporting.

### DB schema / migrations

- Tables above + seed defaults migrated from current hardcoded escalation minutes.

### APIs

- Admin CRUD + `GET /api/v1/sla-policies/resolve?type=&priority=&confidentiality=` for UI.

### Security

- Admin-only mutators; read for authenticated users doing create preview.

### Audit / history

- Policy changes audited; breach events already partially covered — extend Micrometer with
  `sla_policy_id` tag when safe cardinality.

### Notifications

- Matrix-driven: email/SMS/in-app/WebSocket per step.

### Production

- Watch cardinality of metric labels; use low-cardinality `policy_code` not raw id in prod if
  needed.

---

## 7. Read tracking & acknowledgement

> **Status — DELIVERED (Slice 1).** Implemented in Flyway V14 (`correspondence_read_receipt`,
> `attachment_access_log`) plus the new `feature.correspondence.readtracking` and
> `feature.attachment.access` backend packages, FE acknowledgement panel on
> `transaction-details`, and unit tests under `…/readtracking/service/` and
> `…/attachment/access/service/`. See `docs/runbook.md` §7 for forensic queries. Remaining
> work (mandatory-ack policy, read-rate KPI dashboards) is deferred to Slices 6 and follow-up
> reporting work — the sections below describe the full envisioned design, not what was
> delivered in Slice 1.

### Business rules

- Track **first open** of correspondence detail per user; track **each download** of attachments.
- Optional **acknowledgement** required for certain statuses (e.g. directive read receipt).
- Recipients may be users (`correspondence_user_recipient`) and/or departments; analytics roll up
  by unit.

### Backend architecture

- `correspondence_read_receipt` (correspondence_id, user_id, first_opened_at, last_opened_at,
  open_count, acknowledged_at nullable, acknowledgement_comment nullable).
- Hook in workspace GET: idempotent upsert first open (transactional, low overhead).
- Extend reporting service with read-rate KPIs.

### Frontend UX

- Detail view fires lightweight `POST .../read-events` once per session (or rely on workspace
  GET side-effect — prefer explicit event for clarity).
- Ack button when required; disabled after submit.

### Camunda / workflow

- Message correlation `ACK_RECEIVED` to unblock “awaiting acknowledgement” task if policy adds
  such a state.

### DB schema / migrations

- Table above + partial unique `(correspondence_id, user_id)` where deleted_at IS NULL.

### APIs

- `POST /api/v1/correspondence/{id}/ack` — body optional comment.
- `GET /api/v1/correspondence/{id}/read-status` — for owner / privileged roles.

### Security

- Only participants or same-department managers can see per-user read grid; redact names for
  lower clearance if needed.

### Audit / history

- `CORRESPONDENCE_OPENED`, `CORRESPONDENCE_ACKED` in `audit_event` (belt and suspenders with table).

### Notifications

- Notify originator when key recipient acknowledges.

### Production

- Batch analytics ETL optional; index `(correspondence_id, first_opened_at)`.

---

## 8. Print + QR verification

### Business rules

- Official printout carries **QR** encoding a signed payload: `{correspondenceId, referenceNumber,
  issuedAt, documentVersionHash}`.
- Public verification page (no auth) only confirms authenticity, **does not** leak subject/body
  unless optional public flag on correspondence type.

### Backend architecture

- `PrintVerificationService` issues HMAC-signed or asymmetric-signed QR payload using dedicated
  `AC_QR_SIGNING_SECRET` or key pair.
- `GET /api/v1/public/verify?token=` returns `{valid, referenceNumber?, printedAt?, status?}` —
  minimal DTO.

### Frontend UX

- Print stylesheet; QR rendered client-side from API `print-package` or server-side PDF service.
- Mobile-friendly `/verify` route (standalone, no shell nav).

### Camunda / workflow

- None unless “mark as printed” is a workflow milestone.

### DB schema / migrations

- `correspondence_print_artifact` (id, correspondence_id, version_hash, issued_by, issued_at,
  qr_nonce) for replay detection.

### APIs

- `POST /api/v1/correspondence/{id}/print-token` — authenticated; returns signed payload.
- Public verify endpoint as above (rate limited).

### Security

- Rate limit + CAPTCHA on public verify; constant-time compare; no user enumeration via error
  messages.

### Audit / history

- Log verify attempts (success/fail) with IP hash only for privacy.

### Notifications

- Optional alert on repeated failed verify for same token.

### Production

- Separate signing key rotation; CDN caching disabled for verify.

---

## 9. Multi-channel notifications (in-app + WebSocket + email + SMS)

### Business rules

- Users choose channels per **event category** (task assigned, SLA breach, delegation, classified
  download, etc.) with constraints (classified events cannot go to SMS without opt-in).
- Retries with exponential backoff; dead-letter queue table for failed outbound.

### Backend architecture

- `notification_channel_preference` (user_id, event_code, channel_bitmask, updated_at).
- Spring `@EventListener` internal domain events → `NotificationDispatchRouter` → channel
  handlers (WebSocket `SimpMessagingTemplate`, `JavaMailSender`, SMS provider).
- WebSocket STOMP broker optional (simple broker in monolith first; Redis pub/sub for multi
  replica).

### Frontend UX

- Settings screen for preferences; toast + live badge updates via WebSocket subscription.

### Camunda / workflow

- Task listeners emit domain events on assignment/create/complete for real-time inbox refresh.

### DB schema / migrations

- Preferences + `notification_outbox` (id, payload jsonb, channel, attempts, next_attempt_at,
  last_error).

### APIs

- `GET/PUT /api/v1/me/notification-preferences`.
- Internal only: worker endpoint or scheduled job processes outbox.

### Security

- WebSocket auth via JWT in connect header; subscribe destinations scoped to `/user/queue/...`.

### Audit / history

- Aggregate counts per dispatch; detailed logs only for failures in `notification_outbox`.

### Notifications

- This item is foundational; aligns retries with ops dashboards (`notification_dispatch_failures_total`).

### Production

- Horizontal scaling requires shared broker (Redis/RabbitMQ); document in runbook.

---

## 10. Archive retention policies & legal hold

### Business rules

- Each `correspondence_type` + `classification` maps to **retention years** and **archive tier**
  (`ACTIVE`, `SOFT_ARCHIVE`, `PERMANENT_ARCHIVE`, `DESTRUCTION_ELIGIBLE` — destruction never auto
  without workflow in MoD context).
- **Legal hold** suppresses destruction and tightens delete permissions; hold reason + reference
  number stored.
- Soft archive = read-only + hidden from default lists; permanent archive = cold storage flag.

### Backend architecture

- `retention_policy` + `legal_hold` (correspondence_id OR department-wide scope, start, end
  nullable, reference).
- Nightly `ArchiveLifecycleJob`: transition rows when `now > created_at + retention` and no hold.
- Integrate with `CorrespondenceSpecifications.forList` to exclude soft-archived unless
  `CORRESPONDENCE_ARCHIVE_VIEW`.

### Frontend UX

- Archive search facet; legal hold banner for privileged users; export allowed only under
  `REPORT_EXPORT` + hold check.

### Camunda / workflow

- Optional subprocess “Approve destruction” for `DESTRUCTION_ELIGIBLE` (future); for now only
  state transitions without Camunda.

### DB schema / migrations

- `correspondence.archive_tier`, `correspondence.archive_at`, `correspondence.legal_hold_id`
  nullable FK; or separate `correspondence_archive_state` table if you prefer no wide table
  growth.
- `retention_policy` seed from current types.

### APIs

- Admin CRUD retention; `POST /api/v1/legal-holds` (privileged); `DELETE` lift hold.

### Security

- Strong `@PreAuthorize`; dual control for legal hold placement (four-eyes optional).

### Audit / history

- Every tier transition and hold place/lift is `audit_event` + row in `archive_transition_log`.

### Notifications

- Notify records officer before soft-archive bulk job runs (digest).

### Production

- Cold storage move to object lifecycle (Glacier); DB keeps pointer + hash only.

---

## Cross-cutting implementation order (recommended)

1. **Read tracking + attachment access log** — **DELIVERED** (Flyway V14, §7 above).
2. **Task delegation** — **DELIVERED** (Flyway V15, §1 above).
3. **RBAC / capabilities verification pass** — **DELIVERED** (no Flyway changes; tightened
   `EffectiveUserPermissionService` to a single filtered SQL union; added Testcontainers Postgres
   slice test (`EffectivePermissionUnionPostgresTest`) plus four Mockito suites
   (`EffectiveUserPermissionServiceTest`, `UserCapabilitiesServiceTest`, `ShellNavigationServiceTest`,
   `EffectivePermissionExpressionsTest`); added `MeCapabilitiesController` class-level
   `@PreAuthorize` ArchUnit pin; added `permission.guard.spec.ts` + `capabilities.service.spec.ts`
   on the frontend; added `canAny/canAll` helpers and fixed the `CANCEL_TRANSACTION` alias usage
   in `transaction-details.html`; added the `npm run check:routes` diagnostic; the architecture
   is fully documented in [`permissions-architecture.md`](permissions-architecture.md)).
4. **SLA policy engine** — **DELIVERED** (Flyway V16 — `sla_policy`, `sla_escalation_step`,
   `sla_breach_event`). DB-driven specificity-ranked rules resolved by `SlaPolicyResolverService`
   with criteria on correspondence type / priority / confidentiality / org level / workflow
   action type. `SlaPolicyEvaluationJob` runs every minute (configurable via
   `ac.sla.evaluation-poll-ms`), is idempotent via the unique index on
   `sla_breach_event.task_id`, and reconciles resolutions when Camunda tasks complete or
   disappear. `SlaEscalationService` implements the four escalation actions
   (`NOTIFY_MANAGER` / `REASSIGN_TO_DELEGATE` / `ESCALATE_TO_HIGHER_LEVEL` /
   `NOTIFY_AUDIT_ADMIN`) and runs every candidate recipient through `SlaClearanceFilter` —
   clearance is never bypassed and a non-cleared delegate causes `REASSIGN_TO_DELEGATE` to no-op.
   Metrics: `correspondence_sla_breach_total{outcome="…"}` (extended),
   `correspondence_sla_escalation_total{action="…"}`, and the
   `correspondence_sla_overdue_active` gauge. New permissions `SLA_POLICY_VIEW` (read) and
   `SLA_POLICY_MANAGE` (CRUD) granted in V16; admin UI at `/sla-policies`; per-task SLA chip
   + "show breached only" filter on the workflow inbox. Tests: `SlaPolicyResolverServiceTest`,
   `SlaEscalationServiceTest`, `SlaClearanceFilterTest`, `SlaPolicyEvaluationJobTest`,
   `SlaMetricsTest`, plus ArchUnit pin `slaAdminControllersCarryClassLevelPreAuthorize`.
5. **Acting manager** — **DELIVERED** (Flyway V17 — `acting_assignment`, resolver + jobs,
   permission union SQL, `/acting-assignments` UI, inbox badge). Coordinate with task delegation
   (item 2) for precedence: direct assignee → acting → task delegation.
6. **Classified attachments** — depends on KMS readiness; can ship metadata + access log first.
7. **Notification preferences + outbox + WebSocket** — enables reliable delivery for 1–6.
8. **Templates** — extends `letter_template` path.
9. **Digital signatures** — after stable canonical document hash from 8.
10. **Print / QR verify** — uses signing patterns from 9 in lighter form.
11. **Archive / retention / legal hold** — policy-heavy; last to avoid blocking hot paths.

---

## Slice 6 delivery note (in-repo baseline)

The following enterprise-phase bullets are now **DELIVERED** in the modular monolith (Flyway
`V19`–`V21`, packages `com.gov.ac.feature.attachment.verification`, `com.gov.ac.feature.retention`,
`com.gov.ac.feature.notification.channel`, Angular `/verify/:token` plus a full admin UI suite):

- **§9 — Multi-channel notifications:** durable `notification_outbox`, per-user
  `notification_preference`, admin `notification_channel_target`, dispatch SPI (`IN_APP`, `EMAIL`,
  `WEBHOOK`, `TEAMS`) with HMAC-signed outbound webhooks, `ac.notification.routing` cutover flag.
- **§8 / print — Public QR verification:** opaque token + SHA-256 storage, public verify API,
  access log, FE QR issuance dialog (print + copy-URL flow), and a polished public verify page
  that distinguishes invalid / rate-limited / generic errors.
- **§10 — Retention / legal hold:** policy engine + hourly job + `legal_hold` enforcement hooks;
  **no** separate cold-archive tier (hard delete / anonymize only).
- **Admin UI (now fully implemented, no placeholders):**
  - User: `/profile/notifications` (events × channels grid, save/reset).
  - Admin: `/admin/notifications/channels` (CRUD; secrets stored as env-var references),
    `/admin/notifications/outbox` (paged status filter, requeue/cancel, error + next-attempt
    panel), `/admin/retention/policies` (list + safe toggle),
    `/admin/retention/legal-holds` (place/release with confirmation), `/admin/retention/log`
    (paged audit view).

Remaining items in this document (WebSocket inbox fan-out, full template governance,
cold archive tier, etc.) stay on the roadmap until explicitly scheduled.

---

## Compliance traceability

Each capability above should map to:

- **Permission codes** (new SCREAMING_SNAKE rows + `permission_alias` during migration window).
- **ArchUnit** rules (new packages under `feature.*` follow existing controller/service/repository
  layout).
- **i18n** keys for every new UI surface (`npm run check:i18n`).
- **Runbook** updates: KMS, WebSocket scale-out, QR key rotation, legal hold SOP.

This phase keeps the **modular monolith** intact: introduce sub-packages per capability, avoid
circular dependencies from `correspondence` → `notification` by using Spring application events
or a small `common` module contract.
