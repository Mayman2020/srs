-- =============================================================================
-- V9__confidentiality_clearance.sql
--
-- Add per-user security clearance level (FK to confidentiality), so
-- CorrespondenceViewAuthorization can enforce clearance for
-- requires_clearance = TRUE confidentiality levels (TOP_SECRET).
--
-- Defaults every existing user to NORMAL so production behavior is unchanged
-- until admins explicitly elevate clearance.
-- =============================================================================

SET search_path TO srs_system, public;

ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS security_clearance_id BIGINT REFERENCES confidentiality (id) ON DELETE SET NULL;

COMMENT ON COLUMN app_user.security_clearance_id IS
  'Maximum confidentiality level a user is cleared to view. NULL or NORMAL = no restricted access.';

-- Default every user (including the bootstrap system user) to NORMAL.
UPDATE app_user u
SET security_clearance_id = (
  SELECT id FROM confidentiality
  WHERE UPPER(code) = 'NORMAL' AND deleted_at IS NULL
  ORDER BY id LIMIT 1
)
WHERE u.security_clearance_id IS NULL;

-- System principal sees everything (TOP_SECRET).
UPDATE app_user u
SET security_clearance_id = (
  SELECT id FROM confidentiality
  WHERE UPPER(code) = 'TOP_SECRET' AND deleted_at IS NULL
  ORDER BY id LIMIT 1
)
WHERE u.id = '00000000-0000-0000-0000-000000000001';

CREATE INDEX IF NOT EXISTS ix_app_user_security_clearance
  ON app_user (security_clearance_id) WHERE deleted_at IS NULL;
