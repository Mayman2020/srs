-- =============================================================================
-- V16__sla_policy_engine.sql
--
-- Slice 3 of the defense-grade hardening phase
-- (see docs/enterprise-phase-defensive-hardening.md §6):
--
--   1. sla_policy            : DB-driven SLA rules. A rule may match on any of
--                              correspondence_type / priority / confidentiality /
--                              org_level_code / workflow_action_type. Resolution
--                              picks the highest-specificity active row at
--                              evaluation time, so policy edits affect open
--                              tasks immediately without redeploying BPMN.
--   2. sla_escalation_step   : ordered escalation steps belonging to a policy.
--                              action_code is one of NOTIFY_MANAGER /
--                              REASSIGN_TO_DELEGATE / ESCALATE_TO_HIGHER_LEVEL /
--                              NOTIFY_AUDIT_ADMIN.
--   3. sla_breach_event      : per-task state row tracking which step ran last.
--                              Idempotent: PRIMARY KEY on task_id means the
--                              evaluation job can run as often as it wants
--                              without spawning duplicate work.
--   4. Two new canonical permissions:
--        - SLA_POLICY_VIEW    : read SLA policies and breach events.
--        - SLA_POLICY_MANAGE  : CRUD SLA policies.
--   5. Shell nav entry pointing /sla-policies at SLA_POLICY_MANAGE.
--   6. Seed: a sensible baseline policy + steps so the engine has something to
--      resolve before an admin opens the UI.
--
-- No PostgreSQL ENUMs are introduced; codes are stable VARCHAR values enforced
-- by CHECK constraints, matching V14 / V15 conventions.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- sla_policy
-- A rule matches a task when every non-null criterion equals the task's value
-- (NULL criterion = wildcard). target_hours is the wall-clock SLA budget
-- counted from Camunda task creation time. breach_grace_minutes is an optional
-- soft buffer past target_hours before the first step fires (lets ops absorb
-- short clock skew without paging the duty officer).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_policy (
  id                          BIGSERIAL PRIMARY KEY,
  code                        VARCHAR(64)  NOT NULL,
  name_ar                     VARCHAR(255) NOT NULL,
  name_en                     VARCHAR(255) NOT NULL,
  description                 TEXT,
  correspondence_type_id      BIGINT       REFERENCES correspondence_type (id) ON DELETE RESTRICT,
  priority_id                 BIGINT       REFERENCES priority (id) ON DELETE RESTRICT,
  confidentiality_id          BIGINT       REFERENCES confidentiality (id) ON DELETE RESTRICT,
  org_level_code              VARCHAR(8)   REFERENCES organizational_unit_level (code) ON DELETE RESTRICT,
  workflow_action_type_id     BIGINT       REFERENCES workflow_action_type (id) ON DELETE RESTRICT,
  target_hours                INTEGER      NOT NULL,
  breach_grace_minutes        INTEGER      NOT NULL DEFAULT 0,
  is_active                   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at                  TIMESTAMPTZ,
  deleted_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_sla_policy_target_hours_positive       CHECK (target_hours > 0),
  CONSTRAINT ck_sla_policy_breach_grace_non_negative   CHECK (breach_grace_minutes >= 0)
);

COMMENT ON TABLE sla_policy IS
  'DB-driven SLA rules. Resolution at evaluation time prefers the highest-specificity '
  'active row (one point per non-null criterion). target_hours counts from Camunda task '
  'creation time; breach_grace_minutes is a soft buffer before the first escalation step fires.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_sla_policy_code_active
  ON sla_policy (code) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_sla_policy_active_lookup
  ON sla_policy (is_active) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- sla_escalation_step
-- Ordered list of actions to run once a task is past its SLA target. Steps fire
-- in order of step_order; delay_after_breach_minutes is the elapsed minutes
-- past the breach moment before the step is allowed to fire. The evaluation
-- job runs every step whose delay window has opened and whose step_order >
-- last_step_executed_order on the breach event.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_escalation_step (
  id                          BIGSERIAL PRIMARY KEY,
  sla_policy_id               BIGINT       NOT NULL REFERENCES sla_policy (id) ON DELETE CASCADE,
  step_order                  INTEGER      NOT NULL,
  action_code                 VARCHAR(48)  NOT NULL,
  delay_after_breach_minutes  INTEGER      NOT NULL DEFAULT 0,
  target_role_code            VARCHAR(64),
  description                 TEXT,
  is_active                   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at                  TIMESTAMPTZ,
  deleted_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_sla_escalation_step_order_non_negative CHECK (step_order >= 0),
  CONSTRAINT ck_sla_escalation_step_delay_non_negative CHECK (delay_after_breach_minutes >= 0),
  CONSTRAINT ck_sla_escalation_step_action CHECK (
    action_code IN (
      'NOTIFY_MANAGER',
      'REASSIGN_TO_DELEGATE',
      'ESCALATE_TO_HIGHER_LEVEL',
      'NOTIFY_AUDIT_ADMIN'
    )
  )
);

COMMENT ON TABLE sla_escalation_step IS
  'Ordered escalation actions for an SLA policy. step_order is the position in the '
  'sequence; delay_after_breach_minutes is the minimum elapsed time past the breach '
  'before the step is allowed to fire. The job runs every newly-eligible step in '
  'order on each evaluation pass.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_sla_escalation_step_policy_order
  ON sla_escalation_step (sla_policy_id, step_order) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_sla_escalation_step_policy
  ON sla_escalation_step (sla_policy_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- sla_breach_event
-- One row per Camunda task that has been observed to breach its SLA. Used by
-- the evaluation job for idempotency and by ops for "what is currently
-- overdue?" queries.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_breach_event (
  id                          BIGSERIAL PRIMARY KEY,
  task_id                     VARCHAR(64)  NOT NULL,
  process_instance_id         VARCHAR(64),
  workflow_instance_id        UUID         REFERENCES workflow_instance (id) ON DELETE SET NULL,
  correspondence_id           UUID         REFERENCES correspondence (id) ON DELETE SET NULL,
  sla_policy_id               BIGINT       REFERENCES sla_policy (id) ON DELETE SET NULL,
  target_at                   TIMESTAMPTZ  NOT NULL,
  breached_at                 TIMESTAMPTZ  NOT NULL,
  last_step_executed_order    INTEGER      NOT NULL DEFAULT -1,
  last_step_executed_at       TIMESTAMPTZ,
  last_step_action_code       VARCHAR(48),
  steps_executed_total        INTEGER      NOT NULL DEFAULT 0,
  resolved_at                 TIMESTAMPTZ,
  resolution_outcome          VARCHAR(64),
  created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_sla_breach_event_steps_total_non_negative CHECK (steps_executed_total >= 0)
);

COMMENT ON TABLE sla_breach_event IS
  'Per-task SLA breach ledger. Created the first time a task is observed past its '
  'SLA target and updated as each escalation step fires. resolved_at is stamped '
  'when the underlying Camunda task completes; the job uses this to maintain the '
  'overdue-active gauge without scanning Camunda on every tick.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_sla_breach_event_task
  ON sla_breach_event (task_id);

CREATE INDEX IF NOT EXISTS ix_sla_breach_event_unresolved
  ON sla_breach_event (correspondence_id) WHERE resolved_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_sla_breach_event_process
  ON sla_breach_event (process_instance_id) WHERE resolved_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_sla_breach_event_breached_at
  ON sla_breach_event (breached_at) WHERE resolved_at IS NULL;

-- -----------------------------------------------------------------------------
-- updated_at trigger (matches V1 convention for tables that don't use the
-- AuditUserListener flow).
-- -----------------------------------------------------------------------------
CREATE TRIGGER tr_sla_policy_updated_at BEFORE UPDATE ON sla_policy
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_sla_escalation_step_updated_at BEFORE UPDATE ON sla_escalation_step
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_sla_breach_event_updated_at BEFORE UPDATE ON sla_breach_event
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- New canonical permissions (idempotent insert).
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('SLA_POLICY_VIEW',
   'عرض سياسات SLA',
   'View SLA policies',
   'Read SLA policies and breach events.',
   940),
  ('SLA_POLICY_MANAGE',
   'إدارة سياسات SLA',
   'Manage SLA policies',
   'Create, edit, and revoke SLA policies and their escalation steps.',
   950)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- Grant: SYS_ADMIN gets both permissions; AUDITOR gets read-only view; GENERAL_MANAGER
-- gets view to read overdue dashboards (idempotent).
-- -----------------------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN ('SLA_POLICY_VIEW', 'SLA_POLICY_MANAGE')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('AUDITOR', 'GENERAL_MANAGER') AND r.deleted_at IS NULL
  AND p.code = 'SLA_POLICY_VIEW'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- -----------------------------------------------------------------------------
-- Shell nav entry. We point to SLA_POLICY_MANAGE so only admins see the menu
-- item; auditors with read-only access can still reach the page via direct URL
-- since the page itself is reachable to any user holding SLA_POLICY_VIEW.
-- The route_path matches app.routes.ts.
-- -----------------------------------------------------------------------------
INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'sla_policies', '/sla-policies', 'سياسات SLA', 'SLA policies',
       'Configure SLA targets and escalation steps.',
       180, TRUE, 'schedule', TRUE,
       (SELECT id FROM permission WHERE code = 'SLA_POLICY_MANAGE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'sla_policies');

-- -----------------------------------------------------------------------------
-- Seed: one baseline policy ("DEFAULT") and one URGENT variant so the engine
-- has matches on day 1. The escalation matrix mirrors the operational expectation:
--   step 0  : notify manager when SLA breached (no delay).
--   step 1  : auto-reassign to active delegate, 30 min after breach.
--   step 2  : escalate to higher org level, 2 hours after breach.
--   step 3  : notify audit / admin, 8 hours after breach.
-- The URGENT variant compresses target_hours but keeps the same escalation
-- shape so policy authoring stays predictable for ops.
-- -----------------------------------------------------------------------------
INSERT INTO sla_policy (code, name_ar, name_en, description, target_hours, breach_grace_minutes, is_active)
SELECT 'SLA_DEFAULT',
       'سياسة SLA الافتراضية',
       'Default SLA policy',
       'Applies to any task that does not match a more specific policy.',
       48, 15, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sla_policy WHERE code = 'SLA_DEFAULT' AND deleted_at IS NULL);

INSERT INTO sla_policy (code, name_ar, name_en, description, priority_id, target_hours, breach_grace_minutes, is_active)
SELECT 'SLA_URGENT_PRIORITY',
       'سياسة SLA للأولوية العاجلة',
       'Urgent priority SLA',
       'Applies to any task whose correspondence priority is URGENT.',
       (SELECT id FROM priority WHERE code = 'URGENT' AND deleted_at IS NULL ORDER BY id LIMIT 1),
       4, 5, TRUE
WHERE EXISTS (SELECT 1 FROM priority WHERE code = 'URGENT' AND deleted_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM sla_policy WHERE code = 'SLA_URGENT_PRIORITY' AND deleted_at IS NULL);

INSERT INTO sla_escalation_step (sla_policy_id, step_order, action_code, delay_after_breach_minutes, description)
SELECT p.id, v.step_order, v.action_code, v.delay_after_breach_minutes, v.description
FROM sla_policy p
CROSS JOIN (VALUES
  (0, 'NOTIFY_MANAGER', 0,
   'Notify the assignee''s management chain that the task has breached SLA.'),
  (1, 'REASSIGN_TO_DELEGATE', 30,
   'Auto-reassign the task to the assignee''s active authority delegate.'),
  (2, 'ESCALATE_TO_HIGHER_LEVEL', 120,
   'Push the task up the org-level ladder (S → K → L → Q).'),
  (3, 'NOTIFY_AUDIT_ADMIN', 480,
   'Notify auditors and system administrators that the task is still open.')
) AS v(step_order, action_code, delay_after_breach_minutes, description)
WHERE p.code IN ('SLA_DEFAULT', 'SLA_URGENT_PRIORITY')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM sla_escalation_step s
    WHERE s.sla_policy_id = p.id AND s.step_order = v.step_order AND s.deleted_at IS NULL
  );
