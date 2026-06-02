-- =============================================================================
-- V17__acting_assignment.sql
--
-- Slice 4 — Acting manager / delegation expansion
-- (see docs/enterprise-phase-defensive-hardening.md §2):
--
--   1. acting_assignment : time-bounded row where acting_user_id covers workflow
--      tasks that would otherwise be assigned to absent_user_id, with optional
--      scope filters (department, subtree, org level, correspondence type,
--      confidentiality, workflow process key, task definition key,
--      workflow_action_type).
--   2. Permissions: ACTING_ASSIGNMENT_VIEW, ACTING_ASSIGNMENT_MANAGE_OWN,
--      ACTING_ASSIGNMENT_ADMIN.
--   3. Shell nav: /acting-assignments (MANAGE_OWN for self-service; auditors VIEW
--      reach read-only via direct URL if we add admin list later).
--
-- No PostgreSQL ENUMs; CHECK constraints for invariants only.
-- =============================================================================

SET search_path TO srs_system, public;

CREATE TABLE IF NOT EXISTS acting_assignment (
  id                         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  absent_user_id             UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  acting_user_id             UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  department_id              BIGINT       REFERENCES department (id) ON DELETE SET NULL,
  include_department_subtree BOOLEAN      NOT NULL DEFAULT FALSE,
  org_level_code             VARCHAR(8)   REFERENCES organizational_unit_level (code) ON DELETE SET NULL,
  correspondence_type_id     BIGINT       REFERENCES correspondence_type (id) ON DELETE SET NULL,
  confidentiality_id         BIGINT       REFERENCES confidentiality (id) ON DELETE SET NULL,
  workflow_action_type_id    BIGINT       REFERENCES workflow_action_type (id) ON DELETE SET NULL,
  process_definition_key     VARCHAR(128),
  task_definition_key        VARCHAR(255),
  valid_from                 DATE         NOT NULL,
  valid_to                   DATE         NOT NULL,
  notes                      TEXT,
  revoked_at                 TIMESTAMPTZ,
  revoked_by                 UUID REFERENCES app_user (id) ON DELETE SET NULL,
  created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                 UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                 UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_acting_assignment_dates   CHECK (valid_to >= valid_from),
  CONSTRAINT ck_acting_assignment_users  CHECK (absent_user_id <> acting_user_id)
);

COMMENT ON TABLE acting_assignment IS
  'Acting manager coverage: acting_user receives user tasks that would be assigned to absent_user '
  'when optional scope predicates match. Active predicate: revoked_at IS NULL AND today BETWEEN '
  'valid_from AND valid_to. Assignment overlay order: direct assignee -> acting -> task delegation '
  '(see TaskDelegationAssignmentResolver).';

CREATE INDEX IF NOT EXISTS ix_acting_assignment_absent_active
  ON acting_assignment (absent_user_id)
  WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_acting_assignment_acting_active
  ON acting_assignment (acting_user_id)
  WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_acting_assignment_expiry
  ON acting_assignment (valid_to)
  WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_acting_assignment_absent_dept_active
  ON acting_assignment (absent_user_id, COALESCE(department_id, -1))
  WHERE revoked_at IS NULL;

CREATE TRIGGER tr_acting_assignment_updated_at BEFORE UPDATE ON acting_assignment
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('ACTING_ASSIGNMENT_VIEW',
   'عرض تعيينات النيابة الإدارية',
   'View acting assignments',
   'Read acting assignments and audit feed (auditors / leadership).',
   905),
  ('ACTING_ASSIGNMENT_MANAGE_OWN',
   'إدارة النيابة الإدارية (ذاتي)',
   'Manage own acting assignments',
   'Register who acts for you while absent (absent user must be the current user).',
   910),
  ('ACTING_ASSIGNMENT_ADMIN',
   'إدارة النيابة الإدارية (مشرف)',
   'Administer acting assignments',
   'Create, view, and revoke any acting assignment.',
   915)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN ('ACTING_ASSIGNMENT_VIEW', 'ACTING_ASSIGNMENT_MANAGE_OWN', 'ACTING_ASSIGNMENT_ADMIN')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('AUDITOR', 'GENERAL_MANAGER') AND r.deleted_at IS NULL
  AND p.code = 'ACTING_ASSIGNMENT_VIEW'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp ON rp.role_id = r.id
JOIN permission existing ON existing.id = rp.permission_id
CROSS JOIN permission p
WHERE existing.code = 'DELEGATION_MANAGE'
  AND existing.deleted_at IS NULL
  AND r.deleted_at IS NULL
  AND p.code IN ('ACTING_ASSIGNMENT_MANAGE_OWN', 'ACTING_ASSIGNMENT_VIEW')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'acting_assignments', '/acting-assignments', 'النيابة الإدارية', 'Acting assignments',
       'Register acting manager coverage while absent.',
       175, TRUE, 'supervisor_account', TRUE,
       (SELECT id FROM permission WHERE code = 'ACTING_ASSIGNMENT_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'acting_assignments');
