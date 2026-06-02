-- =============================================================================
-- V10__bulk_indexes.sql
--
-- Composite + reverse-direction indexes for hot list / search paths that V1
-- left split or missing:
--   - correspondence reporting filter on (status, created_at DESC)
--   - reverse correspondence_link lookups ("who links to me?")
--   - Camunda task id reverse lookup on workflow_action
--   - confidentiality filter on correspondence
-- =============================================================================

SET search_path TO srs_system, public;

-- Reporting + dashboard: status + creation time window.
CREATE INDEX IF NOT EXISTS ix_correspondence_status_created
  ON correspondence (correspondence_status_id, created_at DESC)
  WHERE deleted_at IS NULL;

-- Filter by confidentiality (auditor / report screens).
CREATE INDEX IF NOT EXISTS ix_correspondence_confidentiality
  ON correspondence (confidentiality_id) WHERE deleted_at IS NULL;

-- Reverse correspondence link lookups.
CREATE INDEX IF NOT EXISTS ix_correspondence_link_reverse
  ON correspondence_link (linked_correspondence_id) WHERE deleted_at IS NULL;

-- Camunda task id -> workflow_action.
CREATE INDEX IF NOT EXISTS ix_workflow_action_camunda_task
  ON workflow_action (camunda_task_id) WHERE deleted_at IS NULL;

-- workflow_history camunda task id (timeline grouping per task).
-- workflow_history is an immutable timeline (V1 baseline notes "do not
-- soft-delete; append corrections as new rows"), so the table has no
-- deleted_at column — keep the index unfiltered.
CREATE INDEX IF NOT EXISTS ix_workflow_history_camunda_task
  ON workflow_history (camunda_task_id);

-- workflow_instance current routing pointer (Q/L/K/S filter).
-- (current_level_code index already created in V5.)

-- correspondence_recipient secondary index (per-user/department mailbox).
CREATE INDEX IF NOT EXISTS ix_correspondence_recipient_dept
  ON correspondence_recipient (department_id) WHERE deleted_at IS NULL;

-- correspondence priority filter (urgent dashboards).
CREATE INDEX IF NOT EXISTS ix_correspondence_priority_created
  ON correspondence (priority_id, created_at DESC) WHERE deleted_at IS NULL;
