-- =============================================================================
-- V9__notification_i18n_keys.sql
-- i18n: store UI message keys + params; titles optional for in-app inbox.
-- =============================================================================

ALTER TABLE notification
  ADD COLUMN message_key VARCHAR(256),
  ADD COLUMN message_params JSONB;

ALTER TABLE notification ALTER COLUMN title_ar DROP NOT NULL;
ALTER TABLE notification ALTER COLUMN title_en DROP NOT NULL;

COMMENT ON COLUMN notification.message_key IS 'Frontend i18n key; human text resolved in UI.';
COMMENT ON COLUMN notification.message_params IS 'Interpolation payload for the message key (JSON object).';
