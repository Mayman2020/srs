-- =============================================================================
-- V11__audit_circular_integrity.sql
--
-- Strengthen user references on audit_event, circular, and circular_recipient
-- so foreign key integrity matches the rest of the schema.
--
-- Strategy:
--   1. Add nullable UUID columns alongside the existing VARCHAR ones.
--   2. Backfill from old VARCHAR when it parses as UUID; clear otherwise.
--   3. Add FK to app_user (NULL ON DELETE) so historical rows survive user deletes.
--
-- VARCHAR columns are kept to avoid breaking any code that still reads them;
-- a later migration (V13/V14) can drop them once consumers move to *_id.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- audit_event.actor_user_uuid (typed FK column).
-- -----------------------------------------------------------------------------
ALTER TABLE audit_event
  ADD COLUMN IF NOT EXISTS actor_user_uuid UUID;

UPDATE audit_event
SET actor_user_uuid = actor_user_id::UUID
WHERE actor_user_uuid IS NULL
  AND actor_user_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

ALTER TABLE audit_event
  DROP CONSTRAINT IF EXISTS fk_audit_event_actor_user_uuid;

ALTER TABLE audit_event
  ADD CONSTRAINT fk_audit_event_actor_user_uuid
  FOREIGN KEY (actor_user_uuid) REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_audit_event_actor_uuid_time
  ON audit_event (actor_user_uuid, occurred_at DESC) WHERE actor_user_uuid IS NOT NULL;

-- -----------------------------------------------------------------------------
-- circular.created_by_user_id (typed FK column).
-- -----------------------------------------------------------------------------
ALTER TABLE circular
  ADD COLUMN IF NOT EXISTS created_by_user_id UUID;

UPDATE circular
SET created_by_user_id = created_by::UUID
WHERE created_by_user_id IS NULL
  AND created_by ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

ALTER TABLE circular
  DROP CONSTRAINT IF EXISTS fk_circular_created_by_user;

ALTER TABLE circular
  ADD CONSTRAINT fk_circular_created_by_user
  FOREIGN KEY (created_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_circular_created_by_user
  ON circular (created_by_user_id) WHERE created_by_user_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- circular_recipient.recipient_user_id (typed FK column).
-- -----------------------------------------------------------------------------
ALTER TABLE circular_recipient
  ADD COLUMN IF NOT EXISTS recipient_user_id UUID;

UPDATE circular_recipient
SET recipient_user_id = user_id::UUID
WHERE recipient_user_id IS NULL
  AND user_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';

ALTER TABLE circular_recipient
  DROP CONSTRAINT IF EXISTS fk_circular_recipient_user;

ALTER TABLE circular_recipient
  ADD CONSTRAINT fk_circular_recipient_user
  FOREIGN KEY (recipient_user_id) REFERENCES app_user (id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS ix_circular_recipient_user_uuid
  ON circular_recipient (recipient_user_id) WHERE recipient_user_id IS NOT NULL;
