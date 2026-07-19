SET search_path TO srs_system, public;

ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN app_user.must_change_password IS
  'Blocks business APIs until the user changes an administrator-issued temporary password.';
