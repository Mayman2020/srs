-- =============================================================================
-- V7__workflow_history.sql
-- Dedicated timeline / SLA / audit trail (NOT a substitute for workflow_action).
-- workflow_action  = canonical step tied to Camunda task completion.
-- workflow_history = rich chronological feed (user + system + SLA + comments).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- workflow_history_event_type (lookup — no ENUM)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history_event_type (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_workflow_history_event_type_sort CHECK (sort_order >= 0)
);

COMMENT ON TABLE workflow_history_event_type IS 'Classifies timeline rows (user action, SLA tick, system event, etc.).';

-- -----------------------------------------------------------------------------
-- workflow_history (immutable-style timeline row; do not soft-delete — append corrections as new rows)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history (
  id                              BIGSERIAL PRIMARY KEY,
  correspondence_id               UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  workflow_instance_id            UUID REFERENCES workflow_instance (id) ON DELETE SET NULL,
  workflow_history_event_type_id  BIGINT      NOT NULL REFERENCES workflow_history_event_type (id) ON DELETE RESTRICT,
  workflow_action_type_id         BIGINT REFERENCES workflow_action_type (id) ON DELETE SET NULL,
  workflow_action_id              BIGINT REFERENCES workflow_action (id) ON DELETE SET NULL,
  actor_user_id                   UUID REFERENCES app_user (id) ON DELETE SET NULL,
  occurred_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
  sequence_no                     INTEGER     NOT NULL,
  primary_comment_text            TEXT,
  detail                          JSONB,
  sla_due_at                      TIMESTAMPTZ,
  sla_expected_at                 TIMESTAMPTZ,
  sla_breached_at                 TIMESTAMPTZ,
  actual_duration_ms              BIGINT,
  remaining_sla_ms                BIGINT,
  previous_correspondence_status_id BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  new_correspondence_status_id    BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  priority_id_at_event            BIGINT REFERENCES priority (id) ON DELETE SET NULL,
  camunda_task_id                 VARCHAR(64),
  camunda_activity_id             VARCHAR(128),
  source_system                   VARCHAR(64) NOT NULL DEFAULT 'AC_APP',
  created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                      UUID,
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                      UUID,
  CONSTRAINT ck_workflow_history_sequence_positive CHECK (sequence_no > 0),
  CONSTRAINT ck_workflow_history_duration CHECK (actual_duration_ms IS NULL OR actual_duration_ms >= 0),
  CONSTRAINT ck_workflow_history_remaining_sla CHECK (remaining_sla_ms IS NULL OR remaining_sla_ms >= 0)
);

COMMENT ON TABLE workflow_history IS 'Full timeline: human actions, status moves, SLA evaluation, integrations.';
COMMENT ON COLUMN workflow_history.sequence_no IS 'Monotonic per correspondence; allocate in application via counter/lock.';
COMMENT ON COLUMN workflow_history.primary_comment_text IS 'Main comment for this timeline point (distinct from threaded rows in workflow_history_comment).';
COMMENT ON COLUMN workflow_history.detail IS 'Structured audit payload (before/after, assignees, rule id, Camunda variables snapshot).';

CREATE UNIQUE INDEX uq_workflow_history_corr_sequence
  ON workflow_history (correspondence_id, sequence_no);

-- -----------------------------------------------------------------------------
-- workflow_history_comment (additional comments attached to one timeline entry)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history_comment (
  id                   BIGSERIAL PRIMARY KEY,
  workflow_history_id  BIGINT NOT NULL REFERENCES workflow_history (id) ON DELETE CASCADE,
  author_user_id       UUID   NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  body                 TEXT   NOT NULL,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by           UUID,
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by           UUID
);

COMMENT ON TABLE workflow_history_comment IS 'Threaded / supplemental comments for a single workflow_history row.';

-- -----------------------------------------------------------------------------
-- Seed: workflow_history_event_type
-- -----------------------------------------------------------------------------
INSERT INTO workflow_history_event_type (code, name_ar, name_en, sort_order) VALUES
  ('USER_ACTION',          'إجراء مستخدم',        'User action',           10),
  ('SYSTEM_EVENT',         'حدث نظام',            'System event',          20),
  ('STATUS_CHANGE',        'تغيير حالة',          'Status change',         30),
  ('SLA_MILESTONE',        'معلم SLA',            'SLA milestone',         40),
  ('SLA_BREACH',           'تجاوز SLA',           'SLA breach',            50),
  ('TASK_ASSIGNED',        'تعيين مهمة',          'Task assigned',         60),
  ('TASK_COMPLETED',       'إكمال مهمة',          'Task completed',        70),
  ('COMMENT',              'تعليق',               'Comment',               80),
  ('DELEGATION',           'تفويض',               'Delegation',            90),
  ('ESCALATION',           'تصعيد',               'Escalation',            100),
  ('CAMUNDA_TRANSITION',   'انتقال سير عمل',      'Workflow transition',   110),
  ('ATTACHMENT',           'مرفق',                'Attachment',            120),
  ('CORRESPONDENCE_LINK',  'ارتباط معاملة',       'Correspondence link',   130);

-- -----------------------------------------------------------------------------
-- Foreign keys: audit columns → app_user
-- -----------------------------------------------------------------------------
ALTER TABLE workflow_history_event_type
  ADD CONSTRAINT fk_wf_hist_evt_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_wf_hist_evt_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_wf_hist_evt_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_history
  ADD CONSTRAINT fk_workflow_history_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_history_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_history_comment
  ADD CONSTRAINT fk_workflow_history_comment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_history_comment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- Indexes (timeline & SLA dashboards)
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_workflow_history_event_type_code_active
  ON workflow_history_event_type (code) WHERE deleted_at IS NULL;

CREATE INDEX ix_workflow_history_correspondence_time
  ON workflow_history (correspondence_id, occurred_at DESC);

CREATE INDEX ix_workflow_history_correspondence_seq
  ON workflow_history (correspondence_id, sequence_no);

CREATE INDEX ix_workflow_history_instance
  ON workflow_history (workflow_instance_id) WHERE workflow_instance_id IS NOT NULL;

CREATE INDEX ix_workflow_history_actor
  ON workflow_history (actor_user_id) WHERE actor_user_id IS NOT NULL;

CREATE INDEX ix_workflow_history_event_type
  ON workflow_history (workflow_history_event_type_id);

CREATE INDEX ix_workflow_history_sla_breach
  ON workflow_history (sla_breached_at) WHERE sla_breached_at IS NOT NULL;

CREATE INDEX ix_workflow_history_action_link
  ON workflow_history (workflow_action_id) WHERE workflow_action_id IS NOT NULL;

CREATE INDEX ix_workflow_history_comment_parent
  ON workflow_history_comment (workflow_history_id);

CREATE INDEX ix_workflow_history_comment_author
  ON workflow_history_comment (author_user_id);

-- -----------------------------------------------------------------------------
-- updated_at triggers
-- -----------------------------------------------------------------------------
CREATE TRIGGER tr_workflow_history_event_type_updated_at
  BEFORE UPDATE ON workflow_history_event_type
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_history_updated_at
  BEFORE UPDATE ON workflow_history
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_history_comment_updated_at
  BEFORE UPDATE ON workflow_history_comment
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
