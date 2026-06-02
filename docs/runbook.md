# SRS Correspondence — Operations Runbook

Operational playbook for SREs and on-call operators. Treat this as the single source of truth
for production tasks.

## 1. Deploy & promote

### Build artefacts

- Backend: `./mvnw -B -DskipTests=false clean package` produces
  `SRS_System_backend/target/admin-communications-api-*.jar`.
- Frontend: `npm ci && npm run build` from `SRS_System_frontend/` outputs `dist/`.

### Required environment in `prod`

| Variable                            | Purpose                                                                 |
|-------------------------------------|-------------------------------------------------------------------------|
| `AC_JWT_SECRET`                     | HS256 signing key, ≥ 32 UTF-8 bytes. Rotated per §3.                    |
| `SPRING_DATASOURCE_URL`             | JDBC URL. **Must include** `?currentSchema=srs_system`.                  |
| `SPRING_DATASOURCE_USERNAME`        | DB user with DDL rights only during migration windows.                   |
| `SPRING_DATASOURCE_PASSWORD`        | Secret store mount.                                                      |
| `AC_CORS_ALLOWED_ORIGIN_PATTERNS`   | Comma-separated allow-list (no `*` in prod).                             |
| `CAMUNDA_BPM_ADMIN_PASSWORD`        | Camunda webapp admin (webapp disabled by default, leave secret).        |
| `AC_STORAGE_ROOT`                   | Mounted volume for attachments. Must persist across pod restarts.        |
| `SMTP_HOST` / `SMTP_PORT`           | Mail relay used by notification dispatch.                                |
| `SPRING_PROFILES_ACTIVE=prod`       | Activates the locked-down profile.                                       |

### Startup smoke test

After deploy:

1. `GET /actuator/health` → `{ "status": "UP" }`.
2. `GET /actuator/prometheus` → `# HELP workflow_task_duration_seconds …` is present.
3. `POST /api/v1/auth/login` with a seeded service account returns a JWT.
4. Frontend `/profile/me/navigation` returns the user's permitted screens.

## 2. Database (Flyway)

- Flyway runs at boot (`baseline-on-migrate: true`, `repair-on-migrate: true`).
- Migrations live under `SRS_System_backend/src/main/resources/db/migration`.
- **Reset procedure** (only on staging or a brand-new prod DB):
  1. Stop the application replicas.
  2. Run `docs/db/srs_system_full_clean_reset.sql` against the DB owner; it drops
     `srs_system` plus any stale `public.flyway_schema_history` row.
  3. Bring replicas back up. Flyway will re-apply V1 → latest.
- **Repair after a failed migration:** ensure replicas are at zero, then start a single
  replica with `SPRING_PROFILES_ACTIVE=prod,dbrepair`; the `dbrepair` profile flips
  `flyway.repair-on-migrate=true` and clears the failed checksum. Scale back up afterwards.

## 3. JWT secret rotation

Goal: rotate `AC_JWT_SECRET` with zero session loss.

1. Generate a new HS256 key ≥ 32 bytes from the secret store (e.g.
   `openssl rand -base64 48`).
2. Roll deploys one replica at a time. After replicas finish rolling, **every existing JWT is
   invalid** (sessions stored in browsers will be rejected at the next API call).
3. Users with active sessions will be bounced to `/login`; the FE detects the 401 and routes
   to login via `http-error.interceptor`. Refresh-tokens persist in `auth_refresh_token` and
   the refresh endpoint succeeds — no manual user action required as long as the FE retries.
4. Update the documented rotation cadence (default: every 90 days, or immediately on
   suspected compromise).

## 4. Prometheus metrics

| Metric                                                | Type    | Labels                  | Notes                                                |
|-------------------------------------------------------|---------|-------------------------|------------------------------------------------------|
| `workflow_task_duration_seconds`                      | Timer   | `process`               | End-to-end process duration on final end event.      |
| `correspondence_sla_breach_total`                     | Counter | `outcome`               | `unassigned` or `reassigned_to_delegate`.            |
| `http_server_requests_seconds`                        | Timer   | standard                | Spring MVC default — paginate per endpoint.          |
| `hikaricp_*`                                          | Gauge   | `pool`                  | DB pool saturation.                                  |

Suggested alerts:

- `rate(correspondence_sla_breach_total[15m]) > 0.1` → page on-call.
- `histogram_quantile(0.95, workflow_task_duration_seconds_bucket{process="inbound-correspondence"}) > 3600` (1 hour).
- `hikaricp_connections_pending > 5 for 1m`.

## 5. Common incidents

### "Login works but every API call returns 401"

- JWT signing key mismatch across replicas. Confirm `AC_JWT_SECRET` is identical on every pod;
  reroll if it diverged from the secret store.

### "Tasks stuck in inbox, no escalation"

- Check `WorkflowEscalationScheduler` logs. Verify the scheduled job is enabled
  (`ac.workflow.escalation.poll-ms`), and that `authority_delegation` rows exist for the
  stuck assignees. A blank delegate keeps the task with the original assignee.

### "User can't see correspondences in their own department"

- Confirm `app_user.department_id` and that the user does NOT carry a stale clearance.
  `CorrespondenceSpecifications#visibleByClearance` hides restricted material when
  `security_clearance_id` is missing.
- For `TOP_SECRET` correspondence: the caller must have a clearance with `sort_order` ≤ the
  level's `sort_order`.

### "Camunda BPMN deploy failure"

- Re-run `mvn -B -Dtest=BpmnDeployabilityTest test` against the offending branch. The test
  parses every BPMN under `processes/` and rejects invalid models.

### "Flyway migration failed mid-run"

- Switch one replica to `prod,dbrepair`, let it complete, then redeploy normally.
- If Flyway reports a checksum mismatch on V1 or V2 (baseline / Camunda DDL), do **not** edit
  the migration. Apply the manual reset (§2) on staging first.

## 6. Frontend deploys

- Static bundle served behind Nginx/CDN. Routes resolve client-side; the API base URL is
  injected via `apps/SRS_System_frontend/src/environments/`.
- `npm run check:i18n` MUST pass before promoting a build — missing translation keys break
  the FE silently.

## 7. Read tracking & attachment access

Added by Flyway V14 as Slice 1 of the defense-grade hardening phase
(`docs/enterprise-phase-defensive-hardening.md`).

### Forensic queries

```sql
-- All readers of a specific correspondence (most-recent first).
SELECT u.username, r.first_opened_at, r.last_opened_at, r.open_count, r.acknowledged_at
FROM srs_system.correspondence_read_receipt r
JOIN srs_system.app_user u ON u.id = r.user_id
WHERE r.correspondence_id = :corr_uuid AND r.deleted_at IS NULL
ORDER BY r.last_opened_at DESC;

-- Every attachment download by a specific user in the last 30 days.
SELECT l.occurred_at, a.display_name, l.action_code, l.ip_address, l.user_agent
FROM srs_system.attachment_access_log l
JOIN srs_system.attachment a ON a.id = l.attachment_id
WHERE l.user_id = :user_uuid
  AND l.occurred_at >= now() - interval '30 days'
ORDER BY l.occurred_at DESC;
```

### Audit-stream cross-reference

The same events are also emitted as canonical `audit_event` rows so they appear in the admin
audit screen and any external SIEM forwarder:

- `CORRESPONDENCE_FIRST_OPENED` — once per `(correspondence_id, user_id)`.
- `CORRESPONDENCE_ACKED` — once on the first acknowledgement.
- `ATTACHMENT_DOWNLOADED` — every successful binary fetch.

### Permissions

| Permission code                    | Granted in V14 to                                                |
|------------------------------------|------------------------------------------------------------------|
| `CORRESPONDENCE_READ_STATUS_VIEW`  | `SYS_ADMIN`, `AUDITOR`. Add to other roles via the admin UI.     |
| `ATTACHMENT_ACCESS_LOG_VIEW`       | `SYS_ADMIN`, `AUDITOR`. Add to other roles via the admin UI.     |

### Operational notes

- Both tables grow with traffic. `correspondence_read_receipt` is bounded by
  `users × correspondences`; `attachment_access_log` is append-only and bounded only by retention
  policy (covered by future Slice 10 — Archive retention).
- Read-tracking writes use `Propagation.REQUIRES_NEW` and are wrapped in `try/catch` at every
  call site — a tracking failure is logged at WARN and does **not** block the detail view or the
  attachment stream. Spot-check the `Read tracking failed` / `Attachment access log write failed`
  WARN log lines after a DB failover.

## 8. Task delegation

Added by Flyway V15 as Slice 2 of the defense-grade hardening phase
(`docs/enterprise-phase-defensive-hardening.md` §1). Distinct from V1
`authority_delegation` — task delegation is the per-task / per-(type, confidentiality)
overlay on top of Camunda task assignment.

### Forensic queries

```sql
-- All active delegations affecting tasks today.
SELECT d.id, dg.username AS delegator, de.username AS delegate,
       d.scope_type, d.camunda_task_id, d.correspondence_id,
       d.valid_from, d.valid_to
FROM srs_system.task_delegation d
JOIN srs_system.app_user dg ON dg.id = d.delegator_user_id
JOIN srs_system.app_user de ON de.id = d.delegate_user_id
WHERE d.revoked_at IS NULL
  AND d.valid_from <= CURRENT_DATE
  AND d.valid_to   >= CURRENT_DATE
ORDER BY d.created_at DESC;

-- Tasks that were completed while routed through a delegation in the last 7 days
-- (workflow_history.detail is JSONB and carries originalAssigneeUserId/actingDelegateUserId).
SELECT h.occurred_at,
       h.detail ->> 'originalAssigneeUserId' AS original_assignee,
       h.detail ->> 'actingDelegateUserId'   AS acting_delegate,
       h.detail ->> 'taskDelegationId'       AS delegation_id,
       h.camunda_task_id, h.primary_comment_text, h.correspondence_id
FROM srs_system.workflow_history h
WHERE h.detail ? 'taskDelegationId'
  AND h.occurred_at >= now() - interval '7 days'
ORDER BY h.occurred_at DESC;
```

### Audit-stream cross-reference

The same lifecycle is also emitted as canonical `audit_event` rows so it appears in the admin
audit screen and any SIEM forwarder:

- `TASK_DELEGATION_CREATED` — once per row, actor = delegator.
- `TASK_DELEGATION_REVOKED` — once per manual revoke, actor = the user who revoked.
- `TASK_DELEGATION_EXPIRED` — once per row from `TaskDelegationExpiryJob`, actor = `SYSTEM`.
- `TASK_ACTED_UNDER_DELEGATION` — once per task that the assignment listener actually rewired
  to a delegate, actor = the delegate. Lets you count "delegations that fired" without trawling
  Camunda history.

### Permissions

| Permission code                | Granted in V15 to                                                          |
|--------------------------------|----------------------------------------------------------------------------|
| `TASK_DELEGATION_MANAGE_OWN`   | Every role that already has `DELEGATION_MANAGE`. Self-service create/revoke. |
| `TASK_DELEGATION_ADMIN`        | `SYS_ADMIN`, `GENERAL_MANAGER`, `AUDITOR`. Revoke on someone else's behalf. |

### Operational notes

- **Expiry job:** `TaskDelegationExpiryJob` runs every `ac.task-delegation.expiry-poll-ms`
  milliseconds (default 600000 = 10 minutes). It is idempotent — running it twice in a row
  finds nothing on the second pass because the `revoked_at IS NULL` predicate filters
  already-expired rows. Safe to invoke manually via Spring Actuator or by bouncing the pod.
- **Manual revoke:** `DELETE /api/v1/delegations/tasks/{id}` — delegator may revoke their own,
  `TASK_DELEGATION_ADMIN` may revoke any.
- **Listener safety:** `TaskDelegationAssignmentResolver` swallows any exception during
  delegation resolution and leaves the canonical assignee in place. A spike in `Task delegation
  lookup failed` WARN logs after a DB failover indicates the lookup is unhappy — workflow
  progress is **not** affected.
- **No BPMN edits:** delegation resolution runs from inside the existing
  `correspondenceTaskAssignmentListener` and `routingStopAssignmentListener` beans, so no
  diagram redeployment is required when toggling the feature.

### Acting manager assignments (Slice 4)

Added by Flyway V17 (`acting_assignment` and permissions `ACTING_ASSIGNMENT_VIEW`,
`ACTING_ASSIGNMENT_MANAGE_OWN`, `ACTING_ASSIGNMENT_ADMIN`). Distinct from task delegation: this
row says user **B** receives user tasks that would have been assigned to absent user **A** when
optional scope predicates match. **Precedence:** direct workflow assignee → acting overlay → task
delegation on the post-acting assignee (`TaskDelegationAssignmentResolver`). **Clearance:** acting
user must be at least as cleared as the absent user on create; acting overlay on a task is skipped
when `SlaClearanceFilter` fails for the correspondence.

### Forensic queries (acting)

```sql
-- Active acting rows today.
SELECT a.id, abs.username AS absent, act.username AS acting,
       a.valid_from, a.valid_to, a.department_id, a.revoked_at
FROM srs_system.acting_assignment a
JOIN srs_system.app_user abs ON abs.id = a.absent_user_id
JOIN srs_system.app_user act ON act.id = a.acting_user_id
WHERE a.revoked_at IS NULL
  AND a.valid_from <= CURRENT_DATE
  AND a.valid_to   >= CURRENT_DATE
ORDER BY a.created_at DESC;
```

### Audit-stream cross-reference (acting)

- `ACTING_ASSIGNMENT_CREATED`, `ACTING_ASSIGNMENT_REVOKED`, `ACTING_ASSIGNMENT_EXPIRED`,
  `ACTING_ASSIGNMENT_USED` (see `ActingAssignmentService` and audit append paths).

### Permissions (acting)

| Permission code                  | Notes |
|----------------------------------|-------|
| `ACTING_ASSIGNMENT_VIEW`         | Read audit feed + shell nav to `/acting-assignments`. Granted with `MANAGE_OWN` to `DELEGATION_MANAGE` roles in V17; also `AUDITOR`, `GENERAL_MANAGER`, `SYS_ADMIN`. |
| `ACTING_ASSIGNMENT_MANAGE_OWN`   | Create rows where `absent_user_id` is the current user; revoke own absent-side rows. |
| `ACTING_ASSIGNMENT_ADMIN`        | Create/revoke for any absent user. |

### Operational notes (acting)

- **Expiry job:** `ActingAssignmentExpiryJob` (`ac.acting-assignment.expiry-poll-ms`).
- **Reconciliation job:** `ActingAssignmentReconciliationJob` (`ac.acting-assignment.reconciliation-poll-ms`, `reconciliation-max-tasks`) clears stale Camunda locals and restores assignees.
- **APIs:** `GET /api/v1/acting-assignments/mine`, `GET /api/v1/acting-assignments/audit` (VIEW),
  `POST /api/v1/acting-assignments`, `DELETE /api/v1/acting-assignments/{id}`.

## 9. SLA policy engine

Added by Flyway V16 as Slice 3 of the defense-grade hardening phase
(`docs/enterprise-phase-defensive-hardening.md` §6). Drives configurable, DB-driven SLA
breach handling alongside the legacy `WorkflowEscalationScheduler`.

### Forensic queries

```sql
-- Currently overdue tasks (one row per Camunda task with an unresolved breach event).
SELECT b.task_id,
       b.process_instance_id,
       c.reference_number,
       sp.code           AS sla_policy,
       b.target_at,
       b.breached_at,
       b.last_step_action_code,
       b.steps_executed_total
FROM srs_system.sla_breach_event b
LEFT JOIN srs_system.correspondence c ON c.id = b.correspondence_id
LEFT JOIN srs_system.sla_policy   sp ON sp.id = b.sla_policy_id
WHERE b.resolved_at IS NULL
ORDER BY b.breached_at;

-- SLA escalation throughput in the last 24 hours, by action code.
SELECT last_step_action_code, COUNT(*) AS fires
FROM srs_system.sla_breach_event
WHERE last_step_executed_at >= now() - interval '24 hours'
GROUP BY last_step_action_code
ORDER BY fires DESC;

-- Highest-specificity policies currently winning resolution (sanity check after policy edits).
SELECT code, name_en, target_hours, breach_grace_minutes,
       (CASE WHEN correspondence_type_id  IS NOT NULL THEN 1 ELSE 0 END)
     + (CASE WHEN priority_id             IS NOT NULL THEN 1 ELSE 0 END)
     + (CASE WHEN confidentiality_id      IS NOT NULL THEN 1 ELSE 0 END)
     + (CASE WHEN org_level_code          IS NOT NULL THEN 1 ELSE 0 END)
     + (CASE WHEN workflow_action_type_id IS NOT NULL THEN 1 ELSE 0 END) AS specificity
FROM srs_system.sla_policy
WHERE is_active = TRUE AND deleted_at IS NULL
ORDER BY specificity DESC, id;
```

### Permissions

| Permission code     | Granted in V16 to                          | Meaning                                                             |
|---------------------|--------------------------------------------|---------------------------------------------------------------------|
| `SLA_POLICY_VIEW`   | `SYS_ADMIN`, `AUDITOR`, `GENERAL_MANAGER`  | Read SLA policies and the breach ledger (`/admin/sla/*` GET endpoints). |
| `SLA_POLICY_MANAGE` | `SYS_ADMIN`                                | CRUD on SLA policies and escalation steps.                          |

The sidebar entry `sla_policies → /sla-policies` is gated by `SLA_POLICY_MANAGE` so only
admins see the link; auditors with `SLA_POLICY_VIEW` reach the page via direct URL for the
read-only breach review.

### Metrics

| Metric | Labels | Source |
|---|---|---|
| `correspondence_sla_breach_total` | `outcome` ∈ {unassigned, reassigned_to_delegate, breach_detected, task_no_longer_active, task_completed} | `WorkflowEscalationScheduler` (legacy) **and** `SlaPolicyEvaluationJob` (new). |
| `correspondence_sla_escalation_total` | `action` ∈ {NOTIFY_MANAGER, REASSIGN_TO_DELEGATE, ESCALATE_TO_HIGHER_LEVEL, NOTIFY_AUDIT_ADMIN} | `SlaEscalationService` per step fire. |
| `correspondence_sla_overdue_active` | none | Gauge: count of `sla_breach_event` rows where `resolved_at IS NULL`. Refreshed every tick. |
| `workflow_task_duration_seconds` | `process` | End-of-process timer recorded by `finalStatusExecutionListener`. |

Alert recipe: page when `correspondence_sla_overdue_active > N` for more than 15 minutes, or
when `rate(correspondence_sla_escalation_total{action="NOTIFY_AUDIT_ADMIN"}[5m]) > 0` —
audit/admin notification means the task has been overdue for at least the cumulative delay
of all earlier steps and ops needs to look.

### Operational notes

- **Evaluation job cadence:** `ac.sla.evaluation-poll-ms` (default 60 000 ms = 1 minute) and
  `ac.sla.max-tasks-per-tick` (default 200) bound the work per tick. The job is idempotent:
  the unique index `ux_sla_breach_event_task` on `task_id` prevents duplicate breach rows,
  and a step won't fire again because the engine compares `step_order` against
  `last_step_executed_order` on every pass.
- **Confidentiality on escalation:** every recipient (manager, delegate, higher-level user,
  audit/admin) is filtered by `SlaClearanceFilter`, which mirrors
  `CorrespondenceViewAuthorization.assertClearance`. A `REASSIGN_TO_DELEGATE` whose delegate
  is not cleared is logged as a no-op so the next step still runs; clearance is **never**
  bypassed.
- **Reconciliation:** if a Camunda task completes or is deleted while a breach is open, the
  next evaluation tick stamps the row `resolved_at = now()` with
  `resolution_outcome = 'TASK_NO_LONGER_ACTIVE'`. No BPMN complete listener is required.
- **Hot policy edits:** there is no BPMN-side coupling. Changing `target_hours`, adding/
  removing a step, or flipping `is_active = FALSE` takes effect on the next tick. The unique
  index on `code` (where `deleted_at IS NULL`) prevents duplicate active codes.
- **Manual evaluation:** POST `/api/v1/admin/sla/policies` then watch `/actuator/prometheus`
  for `correspondence_sla_overdue_active` and `correspondence_sla_escalation_total` to shift
  within one tick (≤ 1 min). If they don't, check the application log for
  `[SLA] evaluation tick failed: …`.
- **Disabling temporarily:** set the policy(ies) to `is_active = FALSE` rather than dropping
  rows. The job simply won't resolve a policy and will short-circuit at the resolver step
  with no further action.

## 10. RBAC drift detection

The capability/navigation/route-guard contract is documented in
[`permissions-architecture.md`](permissions-architecture.md). To detect drift in CI or before
releases, run from `SRS_System_frontend/`:

```bash
npm run check:routes
```

The script cross-checks three sources of truth: `src/app/app.routes.ts`, the seeded canonical
permission codes (and `permission_alias` rows) in the Flyway migrations, and the `ui_screen`
rows with `required_permission_id`. Output lines are grouped by mismatch kind:

| Mismatch kind        | What it means                                                                                                                                |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `missing-permission` | An Angular route's `data.permission` value is not in the DB at all (typo or missing seed). The route would be permanently locked.            |
| `route-uses-alias`   | An Angular route uses a legacy alias code. `/me/capabilities` returns only canonical codes, so `cap.can()` will always be false on that route. |
| `menu-route-mismatch`| A `ui_screen.required_permission_id` resolves to a different canonical code than the matching Angular route's `data.permission`. Sidebar shows the link but `permissionCanMatch` redirects (or vice versa). |
| `dangling-ui-screen` | A `ui_screen` row with `show_in_shell_nav = true` has a `route_path` that doesn't match any Angular route — the sidebar would render a broken link. |

Common causes after a release:

- A new screen was added to `ui_screen` with the wrong `required_permission_id` reference.
- A permission was renamed in DB but `app.routes.ts` still uses the old code.
- A legacy controller was migrated to a canonical code but the FE was not updated.

Backend gates that pin the underlying contract:

- `EffectivePermissionUnionPostgresTest` — proves the temporal + soft-delete + active filters
  all fire in Postgres (requires Docker; auto-skips when Docker is unavailable).
- `EffectiveUserPermissionServiceTest` — pins the native SQL string content (regression-proof
  even when Docker is unavailable).
- `ModuleBoundaryArchTest.capabilitiesControllerCarriesClassLevelPreAuthorize` — guarantees
  `/api/v1/me/...` controllers keep a class-level `@PreAuthorize` (never become anonymously
  reachable).

## 11. Classified attachments & digital signatures (Slice 5)

### KEK provisioning (`AC_ATTACHMENT_KEK_HEX`)

Generate a fresh 32-byte hex KEK and load it into the deployment's secret store:

```bash
# 64 hex chars = 32 bytes; pin the same value across all app instances.
openssl rand -hex 32
```

Set `AC_ATTACHMENT_KEK_HEX` to that value plus `AC_ATTACHMENT_KEK_REF=KEK_V<n>` (default
`KEK_V1`). When the env var is unset the app boots with a deterministic dev key and logs:

```
[crypto] AC_ATTACHMENT_KEK_HEX is unset — using a deterministic DEV key …
```

If you see that line in production, an attacker who copies the database can read every
classified blob. Treat it as a P0 — rotate immediately and re-encrypt existing blobs (see below).

### Signing keypair (`AC_SIGN_PRIVATE_KEY_PEM` / `AC_SIGN_PUBLIC_KEY_PEM`)

Generate an ED25519 keypair (one-time per `AC_SIGN_KEY_REF`):

```bash
openssl genpkey -algorithm Ed25519 -out sign.key
openssl pkey -in sign.key -pubout -out sign.pub
```

Inject both PEMs into the deployment secret store. Missing values trigger an **ephemeral**
keypair (logged WARN at startup) — signatures created against it will not verify after restart.

### Download-token TTL tuning

`ac.attachment.download-token.ttl-seconds` (default `60`). Lower it for high-confidentiality
deployments; avoid raising above ~300 seconds because the token is bound only to the user, not
the client device. The cleanup job (`AttachmentDownloadTokenCleanupJob`) runs every
`ac.attachment.download-token.cleanup-poll-ms` (default `600000`) and deletes rows past
`expires_at + 1h`.

### Forensic queries

```sql
-- All download attempts (success + failure) for one correspondence in the past 24h.
SELECT  l.occurred_at, u.username, l.action_code, l.success, l.ip_address
FROM    srs_system.attachment_access_log l
JOIN    srs_system.app_user u ON u.id = l.user_id
WHERE   l.correspondence_id = :corr
  AND   l.occurred_at > now() - interval '24 hours'
ORDER BY l.occurred_at DESC;

-- Replay / expired-token attempts (correlate with attachment_download_token).
SELECT  l.occurred_at, u.username, l.attachment_version_id, l.ip_address, l.user_agent
FROM    srs_system.attachment_access_log l
JOIN    srs_system.app_user u ON u.id = l.user_id
WHERE   l.action_code = 'DOWNLOAD' AND l.success = false
  AND   l.occurred_at > now() - interval '7 days'
ORDER BY l.occurred_at DESC;

-- Verify every signature on one attachment version (rebuild the verifier projection).
SELECT  s.id, signer.username, s.algorithm, s.status, s.verification_status, s.signed_at
FROM    srs_system.document_signature s
JOIN    srs_system.app_user signer ON signer.id = s.signer_user_id
WHERE   s.attachment_version_id = :versionId
ORDER BY s.signed_at;
```

### Operational procedures

- **Revoke a signature**: `DELETE /api/v1/signatures/{id}` as a user with
  `ATTACHMENT_SIGNATURE_ADMIN`. The next workflow-action attempt by that signer fails the
  `requires_signature` gate immediately.
- **KEK rotation**: keep the old `KEK_V1` env var available for decryption while setting
  `AC_ATTACHMENT_KEK_REF=KEK_V2` for new uploads. Migrating existing rows requires a re-encrypt
  batch job (out of scope for Slice 5 — tracked in roadmap).
- **Tamper-check spot scan**: pick one classified attachment row and call the verifier projection
  (`GET /api/v1/verify/attachment-versions/{id}`); compare `plaintextSha256` with the value
  computed by decrypting the on-disk blob (the same value is exposed by every existing
  signature's `canonicalHashSha256`).

## 13. Slice 6 — retention dry-run, public verify, webhooks, DLQ

- **KEK / signing preconditions:** retention anonymization touches encrypted attachment versions;
  production must still have `AC_ATTACHMENT_KEK_HEX` and `AC_SIGN_*_PEM` set (same Slice 5 gate).
- **Retention cutover:** keep `AC_RETENTION_DRY_RUN=true` for at least one observation week;
  monitor `archive_transition_log` for unexpected `SKIPPED_*` / `FAILED` spikes before flipping
  dry-run off.
- **Public verify:** allowlist `GET /api/v1/public/verify/**` on the reverse proxy for anonymous
  access; add edge rate limits in addition to `ac.attachment.verify.public.rate-limit-per-minute`.
- **Webhook / Teams outbound:** classify destinations; rotate `AC_NOTIFICATION_WEBHOOK_SECRET` and
  per-target `signing_secret_ref` env vars through your secret manager. Misconfigured URLs fill
  `notification_outbox` with `DEAD` rows — use admin re-queue APIs after fixing the target.
- **Notification rollback:** set `AC_NOTIFICATION_ROUTING=inline` to bypass the outbox worker for
  in-app writes only (same-release escape hatch).
- **Legacy download:** `GET /api/v1/attachments/{id}/download` now returns **410 Gone** with
  `migrateTo` guidance; WARN logs include the actor user id for residual integration tracking.
- **Admin UIs (operator handles):**
  - `/admin/notifications/outbox` — paged DLQ inspector. Use the *Cancel* action when the
    upstream event is no longer relevant (it never retries) and *Requeue* after fixing the
    target (it resets `attempt_count` and schedules immediately).
  - `/admin/notifications/channels` — single source of truth for Email/Webhook/Teams targets.
    Secrets are stored as **references** (env-var names); never paste raw secrets here.
  - `/admin/retention/policies` — toggle policies on/off only. Authoritative CRUD lives in
    Flyway because policies map to compliance requirements.
  - `/admin/retention/legal-holds` — placing a hold freezes the correspondence from any
    retention action; releasing requires a non-empty reason and goes through a destructive
    confirmation dialog.
  - `/admin/retention/log` — read-only paged audit of executed lifecycle actions.
  - `/profile/notifications` — every authenticated user with
    `NOTIFICATION_PREFERENCE_MANAGE` can edit their own per-event × channel matrix.

