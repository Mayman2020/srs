-- V26: drive workflow behaviour, org routing roles, SLA defaults, and permissions from DB metadata
SET search_path TO srs_system, public;

-- ---------------------------------------------------------------------------
-- System parameters (global defaults — no hardcoded fallbacks in Java)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_parameter (
  param_key   VARCHAR(64)  PRIMARY KEY,
  param_value VARCHAR(512) NOT NULL,
  name_ar     VARCHAR(200),
  name_en     VARCHAR(200),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE system_parameter IS 'Key/value operational defaults editable by admins (not business lookup rows).';

INSERT INTO system_parameter (param_key, param_value, name_ar, name_en)
VALUES ('workflow.default_sla_hours', '72', 'مدة SLA الافتراضية (ساعات)', 'Default SLA duration (hours)')
ON CONFLICT (param_key) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Org level → default role (replaces Java Map.of Q/L/K/S)
-- ---------------------------------------------------------------------------
ALTER TABLE organizational_unit_level
  ADD COLUMN IF NOT EXISTS default_role_code VARCHAR(64);

UPDATE organizational_unit_level SET default_role_code = 'HQ_OFFICER', updated_at = now()
WHERE UPPER(code) = 'Q' AND deleted_at IS NULL;

UPDATE organizational_unit_level SET default_role_code = 'BRIGADE_OFFICER', updated_at = now()
WHERE UPPER(code) = 'L' AND deleted_at IS NULL;

UPDATE organizational_unit_level SET default_role_code = 'DEPT_MANAGER', updated_at = now()
WHERE UPPER(code) = 'K' AND deleted_at IS NULL;

UPDATE organizational_unit_level SET default_role_code = 'STAFF', updated_at = now()
WHERE UPPER(code) = 'S' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Correspondence status: process-complete outcome (replaces hardcoded COMPLETED)
-- ---------------------------------------------------------------------------
ALTER TABLE correspondence_status
  ADD COLUMN IF NOT EXISTS process_complete_outcome BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE correspondence_status
SET process_complete_outcome = TRUE, updated_at = now()
WHERE UPPER(code) = 'COMPLETED' AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_correspondence_status_one_process_complete
  ON correspondence_status ((1))
  WHERE deleted_at IS NULL AND process_complete_outcome IS TRUE;

-- ---------------------------------------------------------------------------
-- workflow_action_type behaviour metadata
-- ---------------------------------------------------------------------------
ALTER TABLE workflow_action_type
  ADD COLUMN IF NOT EXISTS required_permission_id BIGINT REFERENCES permission (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS requires_target_user BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS requires_target_department BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS keeps_task_open BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS suppress_process_end_status BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS notification_event_type_id BIGINT REFERENCES notification_event_type (id) ON DELETE SET NULL;

COMMENT ON COLUMN workflow_action_type.required_permission_id IS 'If set, action requires this permission (in addition to WORKFLOW_TASK_ACTION route guard).';
COMMENT ON COLUMN workflow_action_type.requires_target_user IS 'REFER-like: API requires targetUserId.';
COMMENT ON COLUMN workflow_action_type.requires_target_department IS 'FORWARD-like: API requires targetDepartmentId.';
COMMENT ON COLUMN workflow_action_type.keeps_task_open IS 'When TRUE, completing via API reassigns without Camunda task completion.';
COMMENT ON COLUMN workflow_action_type.suppress_process_end_status IS 'When TRUE, process end listener must not apply a terminal status (e.g. REFER).';
COMMENT ON COLUMN workflow_action_type.notification_event_type_id IS 'In-app notification event fired when this action completes a task.';

UPDATE workflow_action_type SET
  requires_target_user = TRUE,
  keeps_task_open = TRUE,
  suppress_process_end_status = TRUE,
  updated_at = now()
WHERE UPPER(code) = 'REFER' AND deleted_at IS NULL;

UPDATE workflow_action_type SET
  requires_target_department = TRUE,
  requires_comment = TRUE,
  updated_at = now()
WHERE UPPER(code) = 'FORWARD' AND deleted_at IS NULL;

UPDATE workflow_action_type w SET required_permission_id = p.id, updated_at = now()
FROM permission p
WHERE w.deleted_at IS NULL AND p.deleted_at IS NULL
  AND ((UPPER(w.code) = 'APPROVE' AND p.code = 'CORRESPONDENCE_APPROVE')
    OR (UPPER(w.code) = 'REJECT' AND p.code = 'CORRESPONDENCE_REJECT')
    OR (UPPER(w.code) = 'FORWARD' AND p.code = 'CORRESPONDENCE_FORWARD'));

UPDATE workflow_action_type w SET notification_event_type_id = e.id, updated_at = now()
FROM notification_event_type e
WHERE w.deleted_at IS NULL AND e.deleted_at IS NULL
  AND ((UPPER(w.code) = 'APPROVE' AND UPPER(e.code) = 'APPROVED')
    OR (UPPER(w.code) = 'REJECT' AND UPPER(e.code) = 'REJECTED')
    OR (UPPER(w.code) = 'RETURN' AND UPPER(e.code) = 'RETURNED'));

-- ---------------------------------------------------------------------------
-- Permission: view any correspondence (replaces hardcoded role set in Java)
-- ---------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, sort_order)
SELECT 'CORRESPONDENCE_VIEW_ANY', 'عرض كل المعاملات', 'View any correspondence', 125
WHERE NOT EXISTS (
  SELECT 1 FROM permission WHERE code = 'CORRESPONDENCE_VIEW_ANY' AND deleted_at IS NULL
);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('SYS_ADMIN', 'AUDITOR')
  AND p.code = 'CORRESPONDENCE_VIEW_ANY'
  AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------------
-- SLA escalation action catalog (replaces static FE/Java lists)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sla_escalation_action_type (
  code       VARCHAR(48)  PRIMARY KEY,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  sort_order INTEGER      NOT NULL DEFAULT 0,
  is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO sla_escalation_action_type (code, name_ar, name_en, sort_order) VALUES
  ('NOTIFY_MANAGER',            'إشعار المدير',           'Notify manager',            10),
  ('REASSIGN_TO_DELEGATE',      'إعادة للمفوّض',          'Reassign to delegate',      20),
  ('ESCALATE_TO_HIGHER_LEVEL',  'تصعيد للمستوى الأعلى',   'Escalate to higher level',  30),
  ('NOTIFY_AUDIT_ADMIN',        'إشعار التدقيق',          'Notify audit admin',        40)
ON CONFLICT (code) DO NOTHING;
