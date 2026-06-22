-- V27: BPMN decision variables from DB, leave_status lookup, chart ui_variant, routing task meta
SET search_path TO srs_system, public;

-- ---------------------------------------------------------------------------
-- workflow_action_type: multi-instance early exit (replaces hardcoded REJECT/RETURN in BPMN)
-- ---------------------------------------------------------------------------
ALTER TABLE workflow_action_type
  ADD COLUMN IF NOT EXISTS terminates_routing_chain BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN workflow_action_type.terminates_routing_chain IS
  'When TRUE, completing this action ends the routing multi-instance loop (e.g. REJECT, RETURN).';

UPDATE workflow_action_type
SET terminates_routing_chain = TRUE, updated_at = now()
WHERE UPPER(code) IN ('REJECT', 'RETURN') AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Camunda task metadata (replaces Java string heuristics for routing cursor advance)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workflow_camunda_task_meta (
  task_definition_key     VARCHAR(128) PRIMARY KEY,
  advances_routing_cursor BOOLEAN      NOT NULL DEFAULT FALSE,
  name_ar                 VARCHAR(200),
  name_en                 VARCHAR(200)
);

COMMENT ON TABLE workflow_camunda_task_meta IS
  'Per Camunda userTask id: whether completing the task advances workflow_instance routing cursor.';

INSERT INTO workflow_camunda_task_meta (task_definition_key, advances_routing_cursor, name_ar, name_en) VALUES
  ('Task_Stop_Action',     TRUE, 'محطة ت routing (وارد)', 'Inbound routing stop'),
  ('Task_Internal_Stop', TRUE, 'محطة routing (داخلي)',  'Internal routing stop'),
  ('Task_Approve',       TRUE, 'محطة routing (صادر)',   'Outbound routing stop')
ON CONFLICT (task_definition_key) DO UPDATE SET
  advances_routing_cursor = EXCLUDED.advances_routing_cursor,
  name_ar = EXCLUDED.name_ar,
  name_en = EXCLUDED.name_en;

-- ---------------------------------------------------------------------------
-- priority.ui_variant for dashboard chart colours (aligned with correspondence_status palette)
-- ---------------------------------------------------------------------------
ALTER TABLE priority
  ADD COLUMN IF NOT EXISTS ui_variant VARCHAR(32) NOT NULL DEFAULT 'neutral';

ALTER TABLE priority
  DROP CONSTRAINT IF EXISTS ck_priority_ui_variant;

ALTER TABLE priority
  ADD CONSTRAINT ck_priority_ui_variant CHECK (
    ui_variant IN ('success', 'danger', 'warning', 'info', 'secondary', 'neutral')
  );

UPDATE priority SET ui_variant = 'neutral', updated_at = now()
WHERE UPPER(code) = 'LOW' AND deleted_at IS NULL;

UPDATE priority SET ui_variant = 'info', updated_at = now()
WHERE UPPER(code) = 'NORMAL' AND deleted_at IS NULL;

UPDATE priority SET ui_variant = 'warning', updated_at = now()
WHERE UPPER(code) = 'HIGH' AND deleted_at IS NULL;

UPDATE priority SET ui_variant = 'danger', updated_at = now()
WHERE UPPER(code) IN ('URGENT', 'VERY_URGENT') AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- leave_status lookup (replaces hardcoded PENDING/APPROVED/REJECTED strings)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leave_status (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  ui_variant  VARCHAR(32)  NOT NULL DEFAULT 'neutral',
  is_initial  BOOLEAN      NOT NULL DEFAULT FALSE,
  is_terminal BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_leave_status_ui_variant CHECK (
    ui_variant IN ('success', 'danger', 'warning', 'info', 'secondary', 'neutral')
  ),
  CONSTRAINT ck_leave_status_sort_non_negative CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_leave_status_code_active
  ON leave_status (UPPER(code)) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_leave_status_one_initial
  ON leave_status ((1)) WHERE deleted_at IS NULL AND is_initial IS TRUE;

INSERT INTO leave_status (code, name_ar, name_en, sort_order, ui_variant, is_initial, is_terminal)
SELECT v.code, v.name_ar, v.name_en, v.sort_order, v.ui_variant, v.is_initial, v.is_terminal
FROM (VALUES
  ('PENDING',  'قيد المراجعة', 'Pending',  10, 'warning',  TRUE,  FALSE),
  ('APPROVED', 'موافق عليها',   'Approved', 20, 'success',  FALSE, TRUE),
  ('REJECTED', 'مرفوضة',        'Rejected', 30, 'danger',   FALSE, TRUE)
) AS v(code, name_ar, name_en, sort_order, ui_variant, is_initial, is_terminal)
WHERE NOT EXISTS (
  SELECT 1 FROM leave_status ls WHERE ls.deleted_at IS NULL AND UPPER(ls.code) = UPPER(v.code)
);

INSERT INTO lookup_catalog (lookup_code, name_ar, name_en, parent_lookup_code, sort_order)
SELECT 'leave_status', 'حالة الإجازة', 'Leave status', NULL, 45
WHERE NOT EXISTS (
  SELECT 1 FROM lookup_catalog WHERE lookup_code = 'leave_status'
);

ALTER TABLE leave_request
  ADD COLUMN IF NOT EXISTS leave_status_id BIGINT REFERENCES leave_status (id) ON DELETE RESTRICT;

UPDATE leave_request lr
SET leave_status_id = ls.id
FROM leave_status ls
WHERE lr.leave_status_id IS NULL
  AND lr.deleted_at IS NULL
  AND ls.deleted_at IS NULL
  AND UPPER(ls.code) = UPPER(COALESCE(lr.status_code, 'PENDING'));

UPDATE leave_request lr
SET leave_status_id = ls.id
FROM leave_status ls
WHERE lr.leave_status_id IS NULL
  AND lr.deleted_at IS NULL
  AND ls.deleted_at IS NULL
  AND ls.is_initial IS TRUE;

ALTER TABLE leave_request
  ALTER COLUMN leave_status_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_leave_request_leave_status
  ON leave_request (leave_status_id) WHERE deleted_at IS NULL;

-- status_code kept for backward-compatible reads; application writes via FK only.
