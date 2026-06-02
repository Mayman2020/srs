# SRS Correspondence Management — Architecture

This document captures the production architecture of the SRS Correspondence Management System
(Saudi Ministry of Defence). It is the source of truth for module boundaries, data ownership,
security posture and runtime topology. Implementation details live in code; this document
explains the **why**.

## 1. High-level

- **Pattern:** Modular monolith (single deployable JAR) with hard module boundaries enforced
  by ArchUnit (`com.gov.ac.architecture.ModuleBoundaryArchTest`).
- **Stack:** Spring Boot 3 + Java 17, Hibernate 6, Flyway 10, PostgreSQL 15+, Camunda 7.22,
  Angular standalone (v21), Chart.js, Prometheus / Micrometer.
- **Runtime:** one app container per replica + one PostgreSQL instance (schema `srs_system`).
  Camunda engine tables live in the **same** schema; the `table-prefix` is `srs_system.` and the
  JDBC URL pins `currentSchema=srs_system`.

## 2. Module map

```
com.gov.ac
├── common/                  cross-cutting (audit, api envelope, i18n messages, exceptions)
├── config/                  Spring config (security, JPA, Flyway hooks)
├── security/                JWT filter, capability resolver, RBAC helpers
└── feature/
    ├── auth/                login, refresh, MFA, switch-role
    ├── users/               app_user CRUD
    ├── roles/               role + permission admin
    ├── lookups/             reference data; admin facet under .admin
    ├── organizations/       external organisations directory
    ├── organization/        internal Q/L/K/S level lookup + routing service
    ├── departments/         internal department tree
    ├── correspondence/      core domain (CRUD, list, details, workspace, workflow glue)
    ├── workflow/            Camunda execution, task inbox, routes, jobs
    ├── attachment/          file storage + versioning
    ├── notification/        in-app inbox + dispatch (email, sms, push)
    ├── audit/               immutable audit event log
    ├── reports/ + dashboard/ KPI / Excel exports
    ├── communication/       circular broadcasts
    ├── leave/               leave requests
    ├── delegation/          authority delegation (escalation aware)
    │   └── task/            task-level delegation: entity, repo, service,
    │                        Camunda assignment listener bridge, expiry job
    ├── admin/               console + system-issue intake
    ├── profile/             current-user profile + capability/navigation feeds
    ├── letter_templates/    mustache letter templates
    └── new_transaction_details/ (legacy detail composition — kept until FE workspace migration completes)
```

### Module rules (enforced)

- Controllers live in `..controller..`, repositories in `..repository..`, entities in
  `..entity..`. Controllers never depend on entity classes; the network exposes DTOs only.
- Every controller method in a feature package carries `@PreAuthorize` (method or class level).
  Self-scoped endpoints use `@PreAuthorize("isAuthenticated()")`; public auth endpoints carry
  `@PreAuthorize("permitAll()")` and remain on the unsecured `authEndpointsChain` in
  `SecurityConfig`.

## 3. Data model

- All tables under schema `srs_system`. No PostgreSQL `ENUM` types: every enum-like field is a
  lookup table referenced by `*_id` columns.
- Soft delete: `deleted_at` / `deleted_by`; audit: `created_at`, `created_by`, `updated_at`,
  `updated_by` (filled by `AuditUserListener` from JWT principal).
- Organisational hierarchy: `department` is a tree with `level_code` ∈ {Q, L, K, S} (see
  `OrgRoutingService`).
- Permissions: canonical SCREAMING_SNAKE codes (`CORRESPONDENCE_VIEW`, …) +
  `permission_alias` bridge table that maps legacy dotted codes
  (`correspondence.view`, `user.manage`, …) for one release.
- Read tracking (Slice 1 of the defense-grade hardening phase): `correspondence_read_receipt`
  (partial-unique on `(correspondence_id, user_id)` while `deleted_at IS NULL`) records first/last
  open and optional acknowledgement; `attachment_access_log` is an append-only trail of
  attachment downloads with extracted IP / user-agent. Both are seeded by Flyway V14 alongside
  two new permissions, `CORRESPONDENCE_READ_STATUS_VIEW` and `ATTACHMENT_ACCESS_LOG_VIEW`.
- Task delegation (Slice 2 of the defense-grade hardening phase): `task_delegation` (Flyway V15)
  is a time-bounded, scope-bounded delegation of Camunda user-task assignment. It coexists with
  the V1 `authority_delegation` table — administrative blanket delegation continues to flow
  through `authority_delegation`, while granular per-task or per-(type, confidentiality) routing
  uses `task_delegation`. Active row predicate is `revoked_at IS NULL AND today BETWEEN
  valid_from AND valid_to`; revoked vs expired rows are distinguished by `revoked_by` (UUID =
  manual revoke; NULL = system expiry via `TaskDelegationExpiryJob`). Two new permissions are
  seeded: `TASK_DELEGATION_MANAGE_OWN` (self-service) and `TASK_DELEGATION_ADMIN` (audit/admin).
- SLA policy engine (Slice 3 of the defense-grade hardening phase): three new tables in
  Flyway V16. `sla_policy` holds DB-driven rules with optional criteria on correspondence type,
  priority, confidentiality, org level code, and workflow action type — the
  `SlaPolicyResolverService` picks the highest-specificity active row at evaluation time, so
  policy edits affect live tasks without redeploying BPMN. `sla_escalation_step` is the ordered
  action list per policy (`NOTIFY_MANAGER`, `REASSIGN_TO_DELEGATE`, `ESCALATE_TO_HIGHER_LEVEL`,
  `NOTIFY_AUDIT_ADMIN`) with per-step `delay_after_breach_minutes`. `sla_breach_event` is the
  per-task ledger (one row per Camunda task observed past its SLA target, unique on `task_id`)
  used both for idempotency by `SlaPolicyEvaluationJob` and for the ops "what's overdue?"
  query. Two new permissions are seeded: `SLA_POLICY_VIEW` (read) and `SLA_POLICY_MANAGE`
  (CRUD).
- Acting manager assignments (Slice 4 of the defense-grade hardening phase): `acting_assignment`
  in Flyway V17 is a time-bounded substitute row (`absent_user_id`, `acting_user_id`, optional
  department subtree, org level, correspondence type, confidentiality, workflow action type,
  process/task definition keys). `TaskDelegationAssignmentResolver` applies **direct assignee →
  acting overlay (when clearance passes) → task delegation** on the post-acting assignee.
  `ActingAssignmentExpiryJob` and `ActingAssignmentReconciliationJob` clear stale Camunda locals
  and re-apply delegation. Permissions: `ACTING_ASSIGNMENT_VIEW`, `ACTING_ASSIGNMENT_MANAGE_OWN`,
  `ACTING_ASSIGNMENT_ADMIN`. Shell nav `acting_assignments → /acting-assignments` is gated by
  `ACTING_ASSIGNMENT_VIEW` (self-service roles that may create rows also receive `VIEW` in V17
  alongside `MANAGE_OWN`).
- Classified attachments + digital signatures (Slice 5 of the defense-grade hardening phase):
  Flyway V18 extends `attachment_version` with `encryption_algo`, `encryption_key_ref`,
  `encryption_wrapped_dek`, `encryption_iv`, `ciphertext_sha256`, `plaintext_sha256` and adds
  two new tables — `document_signature` (UUID, FK to `attachment_version_id` and
  `signer_user_id`, `algorithm`, `canonical_hash_sha256`, `signature_bytes`, `status`,
  `verification_status`, partial unique index on the active `(version_id, signer_id)` tuple) and
  `attachment_download_token` (SHA-256(token), FKs, `expires_at`, `consumed_at`, IP / UA). On the
  Java side `com.gov.ac.feature.attachment.crypto` (AES-256-GCM via the `KeyProvider` SPI),
  `com.gov.ac.feature.attachment.download` (intent endpoint + signed-download stream + cleanup
  job), and `com.gov.ac.feature.attachment.signature` (entity, repo, `SigningKeyProvider` SPI
  with ED25519 default, service, controller) own the feature. `workflow_action_type` gains
  `requires_signature` so admins can gate any action behind "sign everything first";
  `CorrespondenceWorkflowActionService` enforces it. The verifier projection lives at
  `GET /api/v1/verify/attachment-versions/{id}` for the upcoming QR / print readouts.
  Permissions: `ATTACHMENT_SIGN_VIEW`, `ATTACHMENT_SIGN_CREATE`, `ATTACHMENT_SIGNATURE_ADMIN`.

## 4. Security

- Stateless JWT (HS256). Secret comes from `AC_JWT_SECRET` env var; **no default in prod**.
- Method-level `@PreAuthorize("@effectivePermission.has('CODE')")` for permission gates;
  `EffectiveUserPermissionService` resolves the effective permission set as the
  **union of every currently-valid `user_role` row** in a single SQL join that filters
  `valid_from / valid_to`, `role.is_active / deleted_at`, and `permission.is_active / deleted_at`,
  **unioned** (Slice 4) with permissions of absent users for whom the principal holds an active
  `acting_assignment` when department scope matches
  (`UserRoleRepository.findEffectivePermissionIdsByUserIdIncludingActing`).
  See [`permissions-architecture.md`](permissions-architecture.md) for the full RBAC contract,
  including why the JWT `currentRole` claim is **not** the authorization source. Legacy codes
  (`correspondence.view`, `CANCEL_TRANSACTION`, …) keep working through `permission_alias`.
- Confidentiality: `app_user.security_clearance_id` + `confidentiality.requires_clearance`
  enforced in `CorrespondenceViewAuthorization#assertClearance` (single detail) and in
  `CorrespondenceSpecifications#visibleByClearance` (list / export).
- Department scoping: `DepartmentScopeResolver` returns the caller's `department_id` for normal
  users and `null` (no scope) for privileged roles (sys admin, archive admin, ministry leadership).
- IDOR-prone endpoints always read the actor from `SecurityUtils.requireCurrentUserId()` rather
  than from request parameters (circular inbox, workflow task inbox, audit self-write, profile).

## 5. Camunda workflow

See [`workflow.md`](workflow.md) for the deep dive. Highlights:

- Three executable BPMNs: `inbound-correspondence`, `outbound-correspondence`,
  `internal-correspondence`. Each implements the multi-stage Q/L/K/S routing topology with
  multi-instance subprocesses driven by `routingChainDelegate`, SLA boundary timers, and a
  `finalStatusExecutionListener` that closes the `workflow_instance` row and emits a Micrometer
  timer sample for end-to-end duration.
- Every Camunda task complete writes one `workflow_history` row **and** one `workflow_action`
  row (`CorrespondenceWorkflowTaskPersistenceService`). When a task was rewired through
  delegation or acting coverage, the `workflow_history.detail` JSON additionally carries
  `originalAssigneeUserId`, `actingDelegateUserId`, `taskDelegationId`, and (Slice 4)
  `workflowDirectAssigneeUserId`, `actingAssignmentId`, `actingForAbsentUserId`,
  `actingManagerUserId`, `effectiveActorUserId` so audit consumers can distinguish the canonical
  assignee, task delegation, and acting-manager overlay.
- Task-delegation and acting overlays: at task `create`, `CorrespondenceTaskAssignmentListener` and
  `RoutingStopAssignmentListener` call `TaskDelegationAssignmentResolver` after they set the
  workflow direct assignee. The resolver first applies **acting assignment** (when a scoped row
  matches and the acting user passes `SlaClearanceFilter` on the correspondence), then applies
  **task delegation** on the assignee after acting. Task-local variables hold delegation ids and
  acting ids for persistence and inbox display. No BPMN edits required. The resolver is
  side-effect-tolerant — any lookup failure leaves the prior assignee in place.
- `WorkflowEscalationScheduler` runs every two minutes (configurable), reassigns stale tasks to
  active authority delegates, increments `workflow_instance.escalation_count` and increments
  the `correspondence_sla_breach_total{outcome="…"}` counter.
- `TaskDelegationExpiryJob` runs every ten minutes (configurable via
  `ac.task-delegation.expiry-poll-ms`) and idempotently marks each non-revoked row whose
  `valid_to` is strictly before today as expired. Replay is a no-op.
- `ActingAssignmentExpiryJob` and `ActingAssignmentReconciliationJob` (configurable via
  `ac.acting-assignment.expiry-poll-ms` and `ac.acting-assignment.reconciliation-poll-ms`) expire
  overdue `acting_assignment` rows and scan active Camunda tasks to clear stale acting locals and
  reset assignees when coverage ended, then re-apply task delegation on the restored assignee.
- `SlaPolicyEvaluationJob` runs every minute (configurable via `ac.sla.evaluation-poll-ms`)
  and is the orchestrator behind the SLA engine. Per tick it pages active Camunda tasks,
  resolves the matching `sla_policy` row, decides whether the task is past its target +
  grace window, and fires every escalation step whose `delay_after_breach_minutes` has
  opened and whose `step_order` is greater than `sla_breach_event.last_step_executed_order`.
  Steps are executed by `SlaEscalationService`; every recipient (manager, delegate, higher
  org level user, audit/admin) is run through `SlaClearanceFilter` to enforce confidentiality
  on the escalation path. The job also reconciles resolutions: any unresolved
  `sla_breach_event` whose `task_id` is no longer in the active Camunda set is stamped with
  `resolved_at = now()` and `resolution_outcome = 'TASK_NO_LONGER_ACTIVE'`, so the overdue
  gauge stays accurate without a BPMN-level complete listener.

## 6. Observability

- `/actuator/health` (liveness + readiness via `management.endpoint.health.probes.enabled=true`).
- `/actuator/prometheus` exports Micrometer registry. Custom metrics:
  - `workflow_task_duration_seconds{process="inbound-correspondence|outbound-…|internal-…"}`
    — Timer recorded when a process reaches its final end event.
  - `correspondence_sla_breach_total{outcome="unassigned|reassigned_to_delegate|breach_detected|task_no_longer_active|task_completed"}` —
    Counter shared between `WorkflowEscalationScheduler` and the SLA engine.
  - `correspondence_sla_escalation_total{action="NOTIFY_MANAGER|REASSIGN_TO_DELEGATE|ESCALATE_TO_HIGHER_LEVEL|NOTIFY_AUDIT_ADMIN"}` —
    Counter incremented by `SlaEscalationService` every time a step fires.
  - `correspondence_sla_overdue_active` — Gauge backed by the count of `sla_breach_event` rows
    where `resolved_at IS NULL`. Refreshed every evaluation tick by `SlaPolicyEvaluationJob`.
- `traceId` is propagated by Micrometer Tracing (MDC). `RestExceptionHandler` returns
  `application/problem+json` with `traceId`, `code` and resolved i18n `message`.

## 7. Build & test gates

- `mvn -B test` runs:
  - Module-boundary ArchUnit suite (`ModuleBoundaryArchTest`).
  - `OrgRoutingServiceTest` covering the four Q/L/K/S routing cases.
  - `BpmnDeployabilityTest` (XML model validation for all three BPMNs).
  - `DepartmentScopeResolverTest` covering privileged vs scoped callers.
- Frontend gate: `npm run check:i18n` — fails CI if a template references a translation key
  that is missing from `ar.json` or `en.json` (also detects drift between the two languages).
- Frontend gate: `npm run check:routes` — cross-checks `app.routes.ts` `data.permission` values
  against the seeded canonical permission codes and the `ui_screen.required_permission_id` map,
  catching dangling sidebar links and routes that would be permanently locked because of a typo
  or a stray alias. See [`permissions-architecture.md`](permissions-architecture.md) section 8.

## 8. Deployment

- Active profile is set via `SPRING_PROFILES_ACTIVE`. `local` ships dev defaults
  (`application-local.yml`). `prod` ships locked-down defaults (HSTS on, Swagger off,
  Prometheus scrape on `/actuator/prometheus` only).
- Mandatory environment variables in prod: `AC_JWT_SECRET`, `SPRING_DATASOURCE_*`,
  `AC_CORS_ALLOWED_ORIGIN_PATTERNS`, `CAMUNDA_BPM_ADMIN_PASSWORD`, `AC_STORAGE_ROOT`.
- Flyway is authoritative; Hibernate stays on `validate`.

## 9. Slice 6 — QR verification, retention, notification outbox

- **Public QR / print verification:** `attachment_verification_token` stores only a SHA-256 hash of
  the opaque token; `GET /api/v1/public/verify/{token}` returns a scrubbed projection and appends
  `attachment_verification_access_log` rows. In-memory rate limiting is configurable under
  `ac.attachment.verify.public.*`.
- **Retention + legal hold:** `retention_policy`, `legal_hold`, and `archive_transition_log` back an
  hourly `RetentionLifecycleJob` (advisory lock per policy, dry-run default). `LegalHoldService`
  blocks destructive paths on held correspondences.
- **Advanced notifications:** `notification_outbox` is the durable queue; `NotificationOutboxDispatchJob`
  claims work with `SELECT … FOR UPDATE SKIP LOCKED`. Channel providers (`IN_APP`, `EMAIL`,
  `WEBHOOK`, `TEAMS`) implement `NotificationChannelProvider`. Webhook/Teams POSTs use
  `X-AC-Signature: hmac-sha256=<base64>` over the raw JSON body; signing material resolves from
  `notification_channel_target.signing_secret_ref` (env-var **name**) with fallback to
  `ac.notification.webhook.signing-secret`. User preferences in `notification_preference` gate
  enqueue; routing mode `ac.notification.routing` chooses `outbox` (default) vs legacy `inline`
  in-app writes.
- **Legacy download tombstone:** `GET /api/v1/attachments/{id}/download` returns **410 Gone** with
  `application/problem+json` including a `migrateTo` hint for the intent + token pipeline.
- **Slice 6 admin UIs (fully implemented):**
  - `/profile/notifications` — events × channels grid backed by `GET/PUT /api/v1/me/notification-preferences`,
    with save/reset and dirty tracking. Gated by `NOTIFICATION_PREFERENCE_MANAGE`.
  - `/admin/notifications/channels` — CRUD over `/api/v1/notification-channel-targets`. Webhook /
    Teams rows require an `https://` URL and an *environment-variable name* in
    `signingSecretRef`; raw secrets are neither collected nor displayed. Gated by
    `NOTIFICATION_CHANNEL_ADMIN`.
  - `/admin/notifications/outbox` — paged status filter (PENDING/SENT/FAILED/DEAD/CANCELLED),
    requeue and cancel actions, plus a details panel that exposes `attemptCount`,
    `nextAttemptAt`, and `lastError`. Gated by `NOTIFICATION_CHANNEL_ADMIN`.
  - `/admin/retention/policies`, `/admin/retention/legal-holds`, `/admin/retention/log` — list +
    safe-confirm toggle/release flows on top of `/api/v1/retention/*`. Policies are
    version-controlled; runtime CRUD is intentionally limited to the enable/disable flag.
  - `/verify/:token` — unauthenticated public verification page. Renders only the scrubbed
    projection (`AttachmentPublicVerificationDto`); shows distinct `not-found`, `rate-limited`,
    and `error` UI states.
- **Notification catalog:** `GET /api/v1/notification-catalog` exposes active event types +
  channels for the preferences and channel admin screens (read-only).

## 10. Roadmap — defense-grade hardening (next phase)

The completed baseline is production-oriented; a **further enhancement phase** covers
delegation depth, acting managers, classified attachment handling, PKI-ready signatures,
template governance, SLA policy engine, read/ack analytics, print/QR verification, multi-channel
notifications with preferences, and archive/retention/legal hold. That phase is specified in
full (business rules, backend/FE/Camunda/DB/APIs/security/audit/notifications/production) in
[`enterprise-phase-defensive-hardening.md`](enterprise-phase-defensive-hardening.md). It extends
this architecture; it does not replace prior delivery.
