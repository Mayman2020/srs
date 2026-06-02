-- =============================================================================
-- V8__workflow_action_transitions.sql
--
-- Complete the data-driven workflow_action_type transition catalog so
-- WorkflowActionResolutionService can resolve every Camunda decision string
-- (APPROVE, REJECT, RETURN, REFER, FORWARD, ARCHIVE, CLOSE, CANCEL) to a
-- next correspondence_status without any hardcoded Java switches.
--
-- V27 (already in V1 baseline) covered APPROVE / REJECT / RETURN / REFER.
-- This migration adds FORWARD, ARCHIVE, CLOSE, CANCEL plus UI variant rows.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- FORWARD: route the correspondence to a different department/user without
-- changing the lifecycle status. Stays IN_PROGRESS.
-- -----------------------------------------------------------------------------
INSERT INTO workflow_action_type (code, name_ar, name_en, sort_order, is_active)
SELECT 'FORWARD', 'تحويل', 'Forward', 75, TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM workflow_action_type WHERE UPPER(code) = 'FORWARD' AND deleted_at IS NULL
);

UPDATE workflow_action_type w
SET next_correspondence_status_id = (
      SELECT id FROM correspondence_status
      WHERE UPPER(code) = 'IN_PROGRESS' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    requires_comment = TRUE,
    show_in_task_decision_ui = TRUE,
    ui_variant = 'secondary'
WHERE UPPER(w.code) = 'FORWARD'
  AND w.deleted_at IS NULL
  AND w.allowed_from_correspondence_status_id IS NULL;

-- -----------------------------------------------------------------------------
-- ARCHIVE: move a terminal/completed correspondence into archive.
-- -----------------------------------------------------------------------------
UPDATE workflow_action_type w
SET next_correspondence_status_id = (
      SELECT id FROM correspondence_status
      WHERE UPPER(code) = 'ARCHIVED' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    requires_comment = FALSE,
    show_in_task_decision_ui = TRUE,
    ui_variant = 'secondary'
WHERE UPPER(w.code) = 'ARCHIVE'
  AND w.deleted_at IS NULL
  AND w.allowed_from_correspondence_status_id IS NULL;

-- -----------------------------------------------------------------------------
-- CLOSE: close an active correspondence without approval (e.g. obsolete).
-- Moves to COMPLETED so the existing terminal-state machinery applies.
-- -----------------------------------------------------------------------------
UPDATE workflow_action_type w
SET next_correspondence_status_id = (
      SELECT id FROM correspondence_status
      WHERE UPPER(code) = 'COMPLETED' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    requires_comment = TRUE,
    show_in_task_decision_ui = TRUE,
    ui_variant = 'secondary'
WHERE UPPER(w.code) = 'CLOSE'
  AND w.deleted_at IS NULL
  AND w.allowed_from_correspondence_status_id IS NULL;

-- -----------------------------------------------------------------------------
-- CANCEL: explicit user-initiated cancellation. Maps to the single
-- correspondence_status row flagged cancel_outcome = TRUE (V28 in baseline).
-- Seeded as an action row only if missing; cancel_outcome row may or may not
-- exist depending on environment, so we only update if it does.
-- -----------------------------------------------------------------------------
INSERT INTO workflow_action_type (code, name_ar, name_en, sort_order, is_active)
SELECT 'CANCEL', 'إلغاء', 'Cancel', 135, TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM workflow_action_type WHERE UPPER(code) = 'CANCEL' AND deleted_at IS NULL
);

UPDATE workflow_action_type w
SET next_correspondence_status_id = (
      SELECT id FROM correspondence_status
      WHERE cancel_outcome = TRUE AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    requires_comment = TRUE,
    show_in_task_decision_ui = FALSE,
    ui_variant = 'danger'
WHERE UPPER(w.code) = 'CANCEL'
  AND w.deleted_at IS NULL
  AND w.allowed_from_correspondence_status_id IS NULL;

-- -----------------------------------------------------------------------------
-- Ensure REFER UI variant is informational and APPROVE/REJECT/RETURN stayed
-- aligned (idempotent re-application in case V27 was patched).
-- -----------------------------------------------------------------------------
UPDATE workflow_action_type SET ui_variant = 'primary'
  WHERE UPPER(code) = 'APPROVE' AND deleted_at IS NULL AND ui_variant <> 'primary';
UPDATE workflow_action_type SET ui_variant = 'danger'
  WHERE UPPER(code) = 'REJECT'  AND deleted_at IS NULL AND ui_variant <> 'danger';
UPDATE workflow_action_type SET ui_variant = 'warning'
  WHERE UPPER(code) = 'RETURN'  AND deleted_at IS NULL AND ui_variant <> 'warning';
UPDATE workflow_action_type SET ui_variant = 'secondary'
  WHERE UPPER(code) = 'REFER'   AND deleted_at IS NULL AND ui_variant <> 'secondary';
