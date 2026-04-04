-- =============================================================================
-- V8__correspondence_create_support.sql
-- Monotonic reference numbers + explicit CREATE action/event for workflow_history.
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS correspondence_reference_seq AS BIGINT START WITH 1 INCREMENT BY 1;

COMMENT ON SEQUENCE correspondence_reference_seq IS 'Application allocates reference_number via nextval in a transaction.';

-- Timeline classification for correspondence creation (distinct from generic USER_ACTION).
INSERT INTO workflow_history_event_type (code, name_ar, name_en, sort_order) VALUES
  ('CREATE', 'إنشاء معاملة', 'Correspondence created', 5);

-- Canonical workflow action code for "create" (API / business semantics).
INSERT INTO workflow_action_type (code, name_ar, name_en, sort_order) VALUES
  ('CREATE', 'إنشاء', 'Create', 5);
