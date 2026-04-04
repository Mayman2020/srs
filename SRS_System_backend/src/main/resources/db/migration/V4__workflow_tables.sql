-- =============================================================================
-- V4__workflow_tables.sql
-- Workflow bridge to Camunda + append-only action history (SRS FR-400, FR-105).
-- Process state of record remains in Camunda; this schema supports APIs & audit.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- workflow_instance (one active bridge row per correspondence recommended)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_instance (
  id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  correspondence_id          UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  process_definition_key     VARCHAR(255) NOT NULL,
  process_instance_id        VARCHAR(64)  NOT NULL,
  workflow_instance_status_id BIGINT      NOT NULL REFERENCES workflow_instance_status (id) ON DELETE RESTRICT,
  started_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  ended_at                   TIMESTAMPTZ,
  business_key               VARCHAR(128),
  deleted_at                 TIMESTAMPTZ,
  deleted_by                 UUID,
  created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                 UUID,
  updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                 UUID,
  CONSTRAINT uq_workflow_instance_process UNIQUE (process_instance_id),
  CONSTRAINT ck_workflow_instance_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

COMMENT ON TABLE workflow_instance IS 'Links correspondence to Camunda processInstanceId and definition key.';
COMMENT ON COLUMN workflow_instance.business_key IS 'Optional Camunda business key (often reference_number).';

-- -----------------------------------------------------------------------------
-- workflow_action (immutable business steps; mirrors Camunda history + SRS table)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_action (
  id                       BIGSERIAL PRIMARY KEY,
  workflow_instance_id     UUID        NOT NULL REFERENCES workflow_instance (id) ON DELETE CASCADE,
  correspondence_id        UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  workflow_action_type_id  BIGINT      NOT NULL REFERENCES workflow_action_type (id) ON DELETE RESTRICT,
  actor_user_id            UUID REFERENCES app_user (id) ON DELETE SET NULL,
  comment_text             TEXT,
  payload                  JSONB,
  camunda_task_id          VARCHAR(64),
  camunda_activity_id      VARCHAR(128),
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by               UUID,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by               UUID
);

COMMENT ON TABLE workflow_action IS 'Each row records one workflow action for timeline APIs (FR-105, FR-402).';
COMMENT ON COLUMN workflow_action.payload IS 'Variable snapshot or structured metadata (decision, targets, etc.).';
