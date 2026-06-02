-- =============================================================================
-- V6__correspondence_user_recipients.sql
--
-- Add user-level recipients alongside the existing department-level
-- correspondence_recipient. Department-level rows continue to drive circular
-- distribution; user-level rows drive per-person To/CC/BCC + per-user read
-- receipts and notification recipient resolution.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- correspondence_recipient_kind: lookup for TO / CC / BCC / INFORMATION.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS correspondence_recipient_kind (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(32)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_correspondence_recipient_kind_sort CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_correspondence_recipient_kind_code_active
  ON correspondence_recipient_kind (UPPER(code)) WHERE deleted_at IS NULL;

INSERT INTO correspondence_recipient_kind (code, name_ar, name_en, sort_order)
SELECT 'TO',      'إلى',           'To',           10
WHERE NOT EXISTS (SELECT 1 FROM correspondence_recipient_kind WHERE UPPER(code) = 'TO' AND deleted_at IS NULL);

INSERT INTO correspondence_recipient_kind (code, name_ar, name_en, sort_order)
SELECT 'CC',      'نسخة',          'Cc',           20
WHERE NOT EXISTS (SELECT 1 FROM correspondence_recipient_kind WHERE UPPER(code) = 'CC' AND deleted_at IS NULL);

INSERT INTO correspondence_recipient_kind (code, name_ar, name_en, sort_order)
SELECT 'BCC',     'نسخة مخفية',     'Bcc',          30
WHERE NOT EXISTS (SELECT 1 FROM correspondence_recipient_kind WHERE UPPER(code) = 'BCC' AND deleted_at IS NULL);

INSERT INTO correspondence_recipient_kind (code, name_ar, name_en, sort_order)
SELECT 'INFO',    'للعلم',          'For information', 40
WHERE NOT EXISTS (SELECT 1 FROM correspondence_recipient_kind WHERE UPPER(code) = 'INFO' AND deleted_at IS NULL);

-- -----------------------------------------------------------------------------
-- correspondence_user_recipient: per-user To/CC/BCC and read tracking.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS correspondence_user_recipient (
  id                BIGSERIAL PRIMARY KEY,
  correspondence_id UUID   NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  recipient_user_id UUID   NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  recipient_kind_id BIGINT NOT NULL REFERENCES correspondence_recipient_kind (id) ON DELETE RESTRICT,
  first_read_at     TIMESTAMPTZ,
  last_read_at      TIMESTAMPTZ,
  read_count        INTEGER NOT NULL DEFAULT 0,
  acknowledged_at   TIMESTAMPTZ,
  notes             TEXT,
  deleted_at        TIMESTAMPTZ,
  deleted_by        UUID,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by        UUID,
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by        UUID,
  CONSTRAINT ck_correspondence_user_recipient_read_count CHECK (read_count >= 0)
);

COMMENT ON TABLE correspondence_user_recipient IS
  'Per-user recipients of a correspondence (To/CC/BCC/Info). Coexists with department-level recipients.';

-- One row per (correspondence, user, kind) while active.
CREATE UNIQUE INDEX IF NOT EXISTS ux_correspondence_user_recipient_unique
  ON correspondence_user_recipient (correspondence_id, recipient_user_id, recipient_kind_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_correspondence_user_recipient_user
  ON correspondence_user_recipient (recipient_user_id) WHERE deleted_at IS NULL;

-- Partial index for "my unread correspondences" inbox query.
CREATE INDEX IF NOT EXISTS ix_correspondence_user_recipient_user_unread
  ON correspondence_user_recipient (recipient_user_id)
  WHERE deleted_at IS NULL AND first_read_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_correspondence_user_recipient_corr
  ON correspondence_user_recipient (correspondence_id) WHERE deleted_at IS NULL;
