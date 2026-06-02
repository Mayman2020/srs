# Correspondence Workflow Reference

This document explains how the SRS correspondence workflow is modelled, executed and observed.

## 1. Domain primitives

| Table                         | Owner module        | Purpose                                                                 |
|-------------------------------|---------------------|-------------------------------------------------------------------------|
| `workflow_instance`           | `workflow.execution`| One row per running / completed Camunda process instance.                |
| `workflow_history`            | `workflow.execution`| Append-only narrative timeline (one row per state change).               |
| `workflow_action`             | `workflow.execution`| One row per **decision** the user took (APPROVE/REJECT/FORWARD/…).       |
| `workflow_action_type`        | `lookups`           | Catalog of decisions with `next_correspondence_status_id`, `requires_comment`, `ui_variant`, `required_role_id`. |
| `service_workflow_route`      | `workflow.routes`   | Optional manual route override the user can pick at create time.         |
| `organizational_unit_level`   | `organization`      | Q / L / K / S level lookup (sort_order = depth).                         |

## 2. Routing topology (Q / L / K / S)

Implemented in `OrgRoutingService#computeChain(originator, target)`:

- **Originator at Q** (HQ) → direct dispatch to target. `chain = [target]`.
- **Same battalion K, both endpoints at S** → peer route. `chain = [target]`.
- **Same brigade L, different K** → bounce through both K commanders.
  `chain = [originator.K, target.K, target]`.
- **Different brigade L** → climb to HQ and back down.
  `chain = [originator.K, originator.L, Q, target.L, target.K, target]`.

The result is serialised to `workflow_instance.routing_chain_json` at process start (by
`RoutingChainDelegate`) and consumed by the multi-instance subprocess and by the FE
routing-preview endpoint.

## 3. BPMN files

All three processes live under `src/main/resources/processes`:

- `inbound-correspondence.bpmn` — externally originated correspondence (received by clerks).
- `outbound-correspondence.bpmn` — outbound dispatch from internal units.
- `internal-correspondence.bpmn` — circulation between internal units.

Each process contains:

1. A start event seeded with `routingChainDelegate` (computes and stores the routing chain).
2. A multi-instance sub-process iterating over `routingChain`, where each iteration creates one
   user task assigned via `routingStopAssignmentListener` (resolves a user from
   `correspondence_user_recipient` or falls back to the role candidate group from
   `routing_stop.role`).
3. Boundary timer events on each user task driven by
   `${execution.getVariable('routingStop').slaIso}` so that long-running tasks raise an event
   for `WorkflowEscalationScheduler` to act on.
4. A final end event wired to `finalStatusExecutionListener` which closes
   `workflow_instance` (`ended_at = now()`), updates `correspondence.correspondence_status_id`
   to the `next_correspondence_status_id` from `workflow_action_type`, and records a Micrometer
   `workflow_task_duration_seconds` timer.

## 4. Task completion contract

Whenever a user completes a task through `CorrespondenceController#actOnTask` →
`CorrespondenceWorkflowTaskPersistenceService#completeTask`:

1. The chosen `workflow_action_type` row is loaded (validates `required_role_id`,
   `requires_comment`, `next_correspondence_status_id`).
2. Camunda completes the task with the action code as process variable so the next gateway
   can branch on it.
3. **Exactly one** `workflow_action` row is persisted with `camunda_task_id`,
   `correspondence_id`, `action_type_id`, comment, payload.
4. **Exactly one** `workflow_history` row is appended (timeline event of type
   `WORKFLOW_TASK_COMPLETED` with `previous_status` → `new_status`).
5. Correspondence status is updated atomically when the action type carries
   `next_correspondence_status_id`.

## 5. SLA, escalation and metrics

`WorkflowEscalationScheduler` (configurable via `ac.workflow.escalation.unassigned-after-minutes`
and `ac.workflow.escalation.poll-ms`) scans Camunda tasks every two minutes:

- **Unassigned and overdue:** writes a `WF_TASK_STALE_UNASSIGNED` audit event, sets the
  `escalationAuditLogged` process variable, increments `correspondence_sla_breach_total{outcome="unassigned"}`.
- **Assigned and overdue, assignee has an active `authority_delegation`:** reassigns the task
  to the delegate, writes a `WF_TASK_REASSIGNED_TO_DELEGATE` audit event, sets the
  `escalationDelegateReassigned` process variable, increments
  `correspondence_sla_breach_total{outcome="reassigned_to_delegate"}`.
- Either branch increments `workflow_instance.escalation_count` so dashboards can surface a
  workflow that has breached SLA.

The end-to-end duration of every completed workflow is recorded as the Timer metric
`workflow_task_duration_seconds{process="<bpmn key>"}` for SRE dashboards.

### 5.1 Task delegation overlay (Slice 2)

At task `create`, after the canonical assignee is resolved by
`correspondenceTaskAssignmentListener` (initiator / explicit first assignee) or
`routingStopAssignmentListener` (department + role match for a routing stop), the listener
calls `TaskDelegationAssignmentResolver`. The resolver runs at most one of these queries:

1. **Task-scoped lookup** (`scope_type = 'TASK'`) — matches when `camunda_task_id` or
   `correspondence_id` on a `task_delegation` row equals the new task / its correspondence.
2. **Broad scope** (`scope_type = 'TYPE_CONFIDENTIALITY'`) — matches when the correspondence's
   `correspondence_type.code` and `confidentiality.code` are reachable by the row's csv filters
   (empty filter = all).

If a match is found, the resolver replaces the task assignee with the delegate and writes three
**task-local** variables (so subsequent task listeners and the persistence listener see them):

- `originalAssigneeUserId` — UUID of the user who would have held the task.
- `actingDelegateUserId` — UUID of the delegate now holding the task.
- `taskDelegationId` — UUID of the `task_delegation` row that fired.

`CorrespondenceWorkflowTaskPersistenceService` copies these three into
`workflow_history.detail` on task completion. `WorkflowTaskInboxService` reads
`actingDelegateUserId` to set the `actingAsDelegate` flag on each inbox row, which the
frontend renders as a "نيابة عن / Acting for" badge.

The resolver is intentionally side-effect-tolerant: any failure (DB blip, missing
correspondence, non-UUID assignee) logs at WARN and leaves the canonical assignee. Workflow
progress must never depend on delegation resolution.

### 5.15 Acting manager overlay (Slice 4)

After the listener sets `WORKFLOW_DIRECT_ASSIGNEE_USER_ID`, `TaskDelegationAssignmentResolver`
evaluates `acting_assignment` rows for the **canonical** assignee (absent user). The best match
uses specificity (non-null scope predicates) and optional filters on department, org level,
correspondence type, confidentiality, workflow action type, and process/task definition keys.
When a row is active (`revoked_at IS NULL`, today in `[valid_from, valid_to]`) and the acting user
passes `SlaClearanceFilter` for the correspondence, the resolver sets the Camunda assignee to the
acting user and stores task locals including `ACTING_ASSIGNMENT_ID`, `ACTING_FOR_ABSENT_USER_ID`,
and `ACTING_MANAGER_USER_ID`. **Task delegation** is then evaluated on that post-acting assignee,
so the effective order is: direct assignee → acting → task delegation. Escalation and audit paths
outside this resolver chain are unchanged.

`ActingAssignmentExpiryJob` stamps expired rows; `ActingAssignmentReconciliationJob` scans active
tasks (bounded by `ac.acting-assignment.reconciliation-max-tasks`) to drop stale acting locals and
restore the absent user when coverage ended, then calls the same delegation overlay helper so
delegations tied to the restored assignee still apply.

`CorrespondenceWorkflowTaskPersistenceService` copies acting and direct-assignee fields into
`workflow_history.detail` alongside delegation keys. The workflow task inbox exposes
`actingAsManager` when the caller holds the task as the acting substitute.

**Frontend.** `/acting-assignments` lists coverage tabs plus a read-only audit feed when the user
holds `ACTING_ASSIGNMENT_VIEW`. The inbox shows an “Acting manager” badge distinct from the task
delegation “Acting for” badge.

### 5.16 Signature-aware workflow actions (Slice 5)

Each `workflow_action_type` row carries two admin-editable boolean gates: `requires_comment` (V1)
and `requires_signature` (V18). When `requires_signature = true`,
`CorrespondenceWorkflowActionService.completeActiveAssigneeTask` runs an extra check before it
invokes Camunda: it iterates the correspondence's active attachments and asserts that the actor
holds a **VALID + VERIFIED** `document_signature` on the latest version of every one. The first
miss throws `BadRequestException("Sign required attachments before completing this action")`, the
Camunda task stays open, and no `workflow_history` row is written.

The signature itself is created via `POST /api/v1/attachments/{id}/signatures`. The service
re-streams the plaintext (decrypting AES-256-GCM on the fly when needed), recomputes its
SHA-256 digest, refuses to sign on drift from the stored `plaintext_sha256`, and delegates the
sign primitive to the `SigningKeyProvider` SPI (ED25519 by default; HSM / PKI adapters slot in by
replacing the bean). Subsequent `POST /api/v1/signatures/{id}/verify` recomputes the same digest
and flips `verification_status` to `VERIFIED` or `FAILED`. `DELETE /api/v1/signatures/{id}`
(behind `ATTACHMENT_SIGNATURE_ADMIN`) flips `status` to `REVOKED` — revoked rows lose the partial
unique index lock so the same signer can re-sign if needed. Every create / verify / revoke
appends an `audit_event` row.

Precedence is unchanged from Slice 4: direct assignee → acting → task delegation → escalation.
The signature gate is an **action-level** check on top of that resolution.

### 5.2 SLA policy engine (Slice 3)

The legacy `WorkflowEscalationScheduler` keeps handling unassigned/stale-task fan-out as
described in §5; **alongside** it, the SLA policy engine drives configurable, DB-driven
escalations.

**Resolution.** `SlaPolicyResolverService` matches a Camunda task to the most specific
`sla_policy` row at evaluation time. Specificity is one point per non-null criterion
(`correspondence_type`, `priority`, `confidentiality`, `org_level_code`,
`workflow_action_type`); ties break by primary key. There is no BPMN-side coupling: editing
a policy or adding a step takes effect on the next evaluation tick (default: one minute) for
every live task without touching the BPMN diagrams.

**Evaluation loop.** `SlaPolicyEvaluationJob` runs every `ac.sla.evaluation-poll-ms`
milliseconds. Each tick:

1. Pages active Camunda tasks (bounded by `ac.sla.max-tasks-per-tick`).
2. For each task, resolves the policy. If the task is past
   `task.createTime + target_hours + breach_grace_minutes`, ensures an `sla_breach_event` row
   exists (idempotent: unique index on `task_id`).
3. Fires every escalation step whose `step_order > last_step_executed_order` AND whose
   `delay_after_breach_minutes` has elapsed since `breached_at`. Each step is executed by
   `SlaEscalationService`.
4. Reconciles resolutions: any unresolved `sla_breach_event` whose `task_id` is no longer in
   the active set is stamped `resolved_at = now()` with outcome `TASK_NO_LONGER_ACTIVE`. The
   overdue gauge is then refreshed.

**Escalation matrix.** `SlaEscalationService` supports four action codes:

| Action | Behaviour |
| --- | --- |
| `NOTIFY_MANAGER` | Resolves `DEPT_MANAGER` for the correspondence's owning department, runs `SlaClearanceFilter`, and sends an in-app notification via `SlaNotifier` (no email/SMS fan-out at this slice; reuses the `OVERDUE` notification event type). |
| `REASSIGN_TO_DELEGATE` | Reads the assignee's most recent active `authority_delegation`; **refuses silently** if the delegate fails `SlaClearanceFilter.isCleared`. When cleared, sets the Camunda task assignee to the delegate and emits an audit event. |
| `ESCALATE_TO_HIGHER_LEVEL` | Walks `department.parent` up the Q/L/K/S ladder, picks the first parent whose `organizational_unit_level.rank_order` is strictly lower than the breach department's, resolves `DEPT_MANAGER` there, filters by clearance, notifies. |
| `NOTIFY_AUDIT_ADMIN` | Queries `UserRoleRepository.findActiveUserIdsByRoleCodes(['SYS_ADMIN','AUDITOR'])`, filters by clearance, and notifies. Used as the final "the task is still hanging, ops needs to look" step. |

**Confidentiality preservation.** Every action runs candidate recipients through
`SlaClearanceFilter`, which mirrors `CorrespondenceViewAuthorization.assertClearance`: a user
without sufficient `security_clearance_id.sort_order` (and not in the privileged
`ARCHIVE_ADMIN` / `SYS_ADMIN` / `MINISTRY_LEADERSHIP` set) is dropped from the recipient
list. A `REASSIGN_TO_DELEGATE` whose delegate is not cleared is treated as a clean no-op so
the next step still runs; it is **never** allowed to bypass clearance.

**Audit trail.** Every escalation step writes an `AC_SLA_*` audit event (correlated by
`task_id` and `process_instance_id`) and increments
`correspondence_sla_escalation_total{action="…"}`. Initial breach detection emits
`correspondence_sla_breach_total{outcome="breach_detected"}`; reconciled resolutions emit
`correspondence_sla_breach_total{outcome="task_no_longer_active"}`.

**Frontend surface.** The workflow task inbox renders per-row SLA chips
("remaining 02h 14m" green / "overdue by 01h 03m" red) fed by
`GET /api/v1/sla/tasks/{taskId}/status` and supports a "show breached only" filter.
Admins reach the SLA policy editor at `/sla-policies` (gated by `SLA_POLICY_MANAGE`);
auditors with `SLA_POLICY_VIEW` reach the same screen via direct URL for read-only review of
the breach ledger.

## 6. Reporting

- `GET /api/v1/reports/department-sla-heatmap` — count + overdue per owner department.
- `GET /api/v1/reports/workflow-sla-trend` — monthly average routing duration (seconds),
  scoped to the caller's department unless they have a privileged view role.
- `GET /api/v1/reports/org-level-distribution` — active correspondence count per Q/L/K/S level.
- `GET /api/v1/reports/confidentiality-distribution` — active correspondence count by
  confidentiality; rows the caller is not cleared for are filtered out upstream.
- `GET /api/v1/reports/export/excel` — Excel export including confidentiality and level codes;
  honours department scope **and** clearance.

## 7. Notifications & Slice 6 outbox cutover

Correspondence and SLA triggers still call the same domain-level notification APIs; internally,
`ac.notification.routing=outbox` (default) enqueues rows into `notification_outbox` and the
`NotificationOutboxDispatchJob` persists in-app notifications asynchronously. Set
`ac.notification.routing=inline` only as a short-lived rollback if the dispatcher must be
disabled during an incident.

Operator + user surfaces (Slice 6, **fully implemented**):

- `/profile/notifications` — user picks which channels deliver which event types.
- `/admin/notifications/channels` — manage Email/Webhook/Teams targets; signing secrets are
  stored as references to environment variables and are never typed or displayed here.
- `/admin/notifications/outbox` — page DLQ by status, requeue or cancel a row, inspect last
  error and next attempt timestamps.
- `/admin/retention/{policies,legal-holds,log}` — policy on/off toggle, place/release legal
  holds, view the archive transition audit trail.
