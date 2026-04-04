-- =============================================================================
-- V5__notification_tables.sql
-- In-app notification store + per-channel delivery attempts (SRS FR-801/802).
-- Email/SMS/Push adapters record outcomes in notification_delivery.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- notification
-- -----------------------------------------------------------------------------
CREATE TABLE notification (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_user_id        UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  notification_event_type_id BIGINT     NOT NULL REFERENCES notification_event_type (id) ON DELETE RESTRICT,
  correspondence_id        UUID REFERENCES correspondence (id) ON DELETE CASCADE,
  title_ar                 VARCHAR(500) NOT NULL,
  title_en                 VARCHAR(500) NOT NULL,
  body_ar                  TEXT,
  body_en                  TEXT,
  data                     JSONB,
  read_at                  TIMESTAMPTZ,
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by               UUID,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by               UUID
);

COMMENT ON TABLE notification IS 'User notification inbox; channel dispatch tracked in notification_delivery.';
COMMENT ON COLUMN notification.data IS 'Structured payload for UI rendering or downstream template merge.';

-- -----------------------------------------------------------------------------
-- notification_delivery
-- -----------------------------------------------------------------------------
CREATE TABLE notification_delivery (
  id                           BIGSERIAL PRIMARY KEY,
  notification_id              UUID        NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
  notification_channel_id      BIGINT      NOT NULL REFERENCES notification_channel (id) ON DELETE RESTRICT,
  notification_delivery_status_id BIGINT NOT NULL REFERENCES notification_delivery_status (id) ON DELETE RESTRICT,
  attempt_count                INTEGER     NOT NULL DEFAULT 0,
  last_error                   TEXT,
  sent_at                      TIMESTAMPTZ,
  external_message_id          VARCHAR(256),
  deleted_at                   TIMESTAMPTZ,
  deleted_by                   UUID,
  created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                   UUID,
  updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                   UUID,
  CONSTRAINT ck_notification_delivery_attempts CHECK (attempt_count >= 0)
);

COMMENT ON TABLE notification_delivery IS 'One row per targeted channel; supports retries and failure diagnostics.';
