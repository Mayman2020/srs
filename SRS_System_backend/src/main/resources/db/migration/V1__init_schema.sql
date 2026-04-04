-- =============================================================================
-- V1__init_schema.sql
-- Extensions, shared functions, and global conventions for
-- Government Administrative Communications System (PostgreSQL + Flyway)
-- =============================================================================

-- UUID generation (correspondence.id, app_user.id, etc.)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Optional: case-insensitive reference lookups (reference_number, username)
CREATE EXTENSION IF NOT EXISTS citext;

-- -----------------------------------------------------------------------------
-- updated_at maintenance (application may still set explicitly)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ac_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

COMMENT ON FUNCTION ac_set_updated_at() IS 'Sets NEW.updated_at to transaction timestamp before row update.';
