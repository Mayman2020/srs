-- =============================================================================
-- V15__task_delegation.sql
--
-- Slice 2 of the defense-grade hardening phase
-- (see docs/enterprise-phase-defensive-hardening.md §1):
--
--   1. task_delegation          : narrow, time-bounded delegation that affects
--                                 Camunda task assignment (one specific task or
--                                 all open tasks matching type + confidentiality).
--                                 Coexists with authority_delegation (V1 admin
--                                 delegation) without replacing it.
--   2. Two new canonical permissions:
--        - TASK_DELEGATION_MANAGE_OWN  : self-service create/revoke own task
--                                       delegations (granted by default to all
--                                       roles that can act on tasks).
--        - TASK_DELEGATION_ADMIN       : manage any task delegation
--                                       (SYS_ADMIN + GENERAL_MANAGER + AUDITOR).
--
-- No PostgreSQL ENUMs are introduced; scope codes are stable VARCHAR values
-- enforced by CHECK constraints, matching the existing audit_event.action_code
-- convention.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- task_delegation
-- One row per active delegation. Lifecycle:
--   * created  -> revoked_at IS NULL
--   * revoked  -> revoked_at = now(), revoked_by = actor
--   * expired  -> revoked_at = expiry_run_time, revoked_by = NULL (system)
--
-- Active predicate (used by every read path):
--   revoked_at IS NULL AND valid_from <= today AND valid_to >= today
--
-- scope_type semantics:
--   TASK                  : applies only to camunda_task_id (or
--                            process_instance_id + correspondence_id) — single
--                            user task.
--   TYPE_CONFIDENTIALITY  : applies to every OPEN task whose correspondence
--                            matches allowed_correspondence_type_codes AND
--                            allowed_confidentiality_codes.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_delegation (
  id                                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  delegator_user_id                 UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  delegate_user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  scope_type                        VARCHAR(32)  NOT NULL,
  correspondence_id                 UUID         REFERENCES correspondence (id) ON DELETE CASCADE,
  camunda_task_id                   VARCHAR(64),
  process_instance_id               VARCHAR(64),
  allowed_correspondence_type_codes TEXT,
  allowed_confidentiality_codes     TEXT,
  valid_from                        DATE         NOT NULL,
  valid_to                          DATE         NOT NULL,
  notes                             TEXT,
  revoked_at                        TIMESTAMPTZ,
  revoked_by                        UUID REFERENCES app_user (id) ON DELETE SET NULL,
  authority_delegation_id           UUID REFERENCES authority_delegation (id) ON DELETE SET NULL,
  created_at                        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                        UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at                        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                        UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_task_delegation_dates           CHECK (valid_to >= valid_from),
  CONSTRAINT ck_task_delegation_users           CHECK (delegator_user_id <> delegate_user_id),
  CONSTRAINT ck_task_delegation_scope           CHECK (scope_type IN ('TASK', 'TYPE_CONFIDENTIALITY')),
  CONSTRAINT ck_task_delegation_scope_task      CHECK (
    scope_type <> 'TASK'
    OR (camunda_task_id IS NOT NULL OR correspondence_id IS NOT NULL)
  )
);

COMMENT ON TABLE task_delegation IS
  'Time-bounded delegation of Camunda user tasks. Coexists with authority_delegation. '
  'Active row predicate: revoked_at IS NULL AND today BETWEEN valid_from AND valid_to. '
  'scope_type=TASK targets a specific camunda_task_id or correspondence_id; '
  'scope_type=TYPE_CONFIDENTIALITY targets every open task whose correspondence '
  'matches the allowed_* csv filters (empty/null = all).';

-- Hot path: "who is delegating to me right now?"  Bounded by partial index.
CREATE INDEX IF NOT EXISTS ix_task_delegation_delegate_active
  ON task_delegation (delegate_user_id)
  WHERE revoked_at IS NULL;

-- Hot path: "what am I delegating right now?"  Used by overlap detection.
CREATE INDEX IF NOT EXISTS ix_task_delegation_delegator_active
  ON task_delegation (delegator_user_id)
  WHERE revoked_at IS NULL;

-- Single-task lookup from listener / inbox query.
CREATE INDEX IF NOT EXISTS ix_task_delegation_task
  ON task_delegation (camunda_task_id)
  WHERE revoked_at IS NULL AND camunda_task_id IS NOT NULL;

-- Correspondence-scoped lookup (used when listener has no taskId yet).
CREATE INDEX IF NOT EXISTS ix_task_delegation_correspondence
  ON task_delegation (correspondence_id)
  WHERE revoked_at IS NULL AND correspondence_id IS NOT NULL;

-- Expiry job scan range.
CREATE INDEX IF NOT EXISTS ix_task_delegation_expiry
  ON task_delegation (valid_to)
  WHERE revoked_at IS NULL;

-- -----------------------------------------------------------------------------
-- New canonical permissions (idempotent insert)
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('TASK_DELEGATION_MANAGE_OWN',
   'إدارة تفويض المهام (ذاتي)',
   'Manage own task delegations',
   'Create and revoke task delegations where the current user is the delegator.',
   920),
  ('TASK_DELEGATION_ADMIN',
   'إدارة تفويض المهام (مشرف)',
   'Administer task delegations',
   'Manage any task delegation (view across users, revoke on behalf of delegator).',
   930)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- Grant TASK_DELEGATION_MANAGE_OWN to every role that can already act on tasks
-- (idempotent). This keeps self-service delegation aligned with the existing
-- "can complete tasks" surface — no role gains a new privilege beyond its
-- current scope, just the ability to hand off a specific task they own.
-- -----------------------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp ON rp.role_id = r.id
JOIN permission existing ON existing.id = rp.permission_id
CROSS JOIN permission p
WHERE existing.code = 'DELEGATION_MANAGE'
  AND existing.deleted_at IS NULL
  AND r.deleted_at IS NULL
  AND p.code = 'TASK_DELEGATION_MANAGE_OWN'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

-- -----------------------------------------------------------------------------
-- Grant TASK_DELEGATION_ADMIN to SYS_ADMIN + GENERAL_MANAGER + AUDITOR
-- (idempotent).
-- -----------------------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('SYS_ADMIN', 'GENERAL_MANAGER', 'AUDITOR') AND r.deleted_at IS NULL
  AND p.code = 'TASK_DELEGATION_ADMIN'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
