-- =============================================================================
-- V21__notification_outbox_and_preferences.sql
--
-- Slice 6 — Advanced notification channels:
--
--   1. notification_outbox: durable queue of pending dispatches with retry
--      backoff, idempotency dedup, DEAD-letter status. Replaces the inline
--      "save InAppNotificationEntity inside the request thread" pattern with
--      a SELECT ... FOR UPDATE SKIP LOCKED multi-instance worker.
--   2. notification_preference: per-(user, event_type, channel) opt-in/out
--      consulted at enqueue time.
--   3. notification_channel_target: per-target webhook/Teams URL +
--      signing_secret_ref (env-var name, never the literal secret).
--   4. WEBHOOK + TEAMS rows added to notification_channel.
--   5. Permissions: NOTIFICATION_PREFERENCE_MANAGE, NOTIFICATION_CHANNEL_ADMIN.
--   6. ui_screen rows for the new admin + profile pages.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- 1. notification_outbox
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_outbox (
  id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  idempotency_key             VARCHAR(128) NOT NULL,
  event_type_code             VARCHAR(64)  NOT NULL,
  channel_code                VARCHAR(32)  NOT NULL,
  recipient_user_id           UUID REFERENCES app_user (id) ON DELETE CASCADE,
  recipient_address           VARCHAR(512),
  subject                     VARCHAR(512),
  body_text                   TEXT,
  payload_json                TEXT,
  message_key                 VARCHAR(128),
  message_params_json         TEXT,
  correlation_resource_type   VARCHAR(64),
  correlation_resource_id     VARCHAR(64),
  status                      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  attempt_count               INTEGER      NOT NULL DEFAULT 0,
  next_attempt_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  last_attempted_at           TIMESTAMPTZ,
  last_error                  TEXT,
  created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                  UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_notification_outbox_status
    CHECK (status IN ('PENDING', 'IN_FLIGHT', 'SENT', 'FAILED', 'DEAD')),
  CONSTRAINT ck_notification_outbox_attempts
    CHECK (attempt_count >= 0)
);

COMMENT ON TABLE notification_outbox IS
  'Durable queue of pending channel dispatches. NotificationOutboxDispatchJob '
  'polls PENDING rows with next_attempt_at <= now() using SELECT ... FOR UPDATE '
  'SKIP LOCKED for multi-instance safety. After max-attempts a row transitions '
  'to DEAD (DLQ) and emits NOTIFICATION_OUTBOX_DEAD_LETTER on audit_event.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_outbox_idempotency
  ON notification_outbox (idempotency_key);

CREATE INDEX IF NOT EXISTS ix_notification_outbox_dispatch_queue
  ON notification_outbox (status, next_attempt_at)
  WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS ix_notification_outbox_correlation
  ON notification_outbox (correlation_resource_type, correlation_resource_id);

CREATE INDEX IF NOT EXISTS ix_notification_outbox_dead
  ON notification_outbox (status, updated_at DESC)
  WHERE status = 'DEAD';

CREATE TRIGGER tr_notification_outbox_updated_at BEFORE UPDATE ON notification_outbox
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2. notification_preference
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_preference (
  id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  event_type_code     VARCHAR(64)  NOT NULL,
  channel_code        VARCHAR(32)  NOT NULL,
  enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at          TIMESTAMPTZ,
  deleted_by          UUID,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by          UUID REFERENCES app_user (id) ON DELETE SET NULL
);

COMMENT ON TABLE notification_preference IS
  'Per-(user, event_type, channel) opt-in/out flag consulted by '
  'NotificationOutboxService.enqueue. Default is enabled unless a row says '
  'otherwise (i.e. opt-out is explicit, not implicit).';

CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_preference_per_user_event_channel
  ON notification_preference (user_id, event_type_code, channel_code)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_notification_preference_user
  ON notification_preference (user_id)
  WHERE deleted_at IS NULL;

CREATE TRIGGER tr_notification_preference_updated_at BEFORE UPDATE ON notification_preference
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 3. notification_channel_target
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_channel_target (
  id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_code        VARCHAR(32)  NOT NULL,
  target_code         VARCHAR(64)  NOT NULL,
  target_url          VARCHAR(1024),
  signing_secret_ref  VARCHAR(128),
  enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
  description         TEXT,
  deleted_at          TIMESTAMPTZ,
  deleted_by          UUID,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by          UUID REFERENCES app_user (id) ON DELETE SET NULL
);

COMMENT ON TABLE notification_channel_target IS
  'Per-channel-target URL bag (webhook destinations, Teams incoming-webhook '
  'URLs, etc.). signing_secret_ref is the env-var NAME (e.g. '
  'AC_NOTIFICATION_WEBHOOK_SECRET), never the literal secret value.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_channel_target_code
  ON notification_channel_target (target_code)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_notification_channel_target_channel
  ON notification_channel_target (channel_code)
  WHERE enabled = TRUE AND deleted_at IS NULL;

CREATE TRIGGER tr_notification_channel_target_updated_at BEFORE UPDATE ON notification_channel_target
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 4. WEBHOOK + TEAMS channel rows (idempotent)
-- -----------------------------------------------------------------------------
INSERT INTO notification_channel (code, name_ar, name_en, sort_order)
SELECT v.code, v.name_ar, v.name_en, v.sort_order
FROM (VALUES
  ('WEBHOOK', 'ويب هوك',      'Webhook', 50),
  ('TEAMS',   'مايكروسوفت تيمز', 'Microsoft Teams', 60)
) AS v(code, name_ar, name_en, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM notification_channel nc WHERE nc.code = v.code AND nc.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 5. Permissions
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('NOTIFICATION_PREFERENCE_MANAGE',
   'إدارة تفضيلات الإشعارات',
   'Manage notification preferences',
   'Manage own notification opt-in/out preferences per channel.',
   1100),
  ('NOTIFICATION_CHANNEL_ADMIN',
   'إدارة قنوات الإشعارات',
   'Administer notification channels',
   'Manage notification channel targets and the outbox DLQ.',
   1110)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- SYS_ADMIN gets channel admin + preferences.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN ('NOTIFICATION_PREFERENCE_MANAGE', 'NOTIFICATION_CHANNEL_ADMIN')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Anyone who already has NOTIFICATION_VIEW (read own inbox) may also manage own
-- preferences (opt-out per channel).
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp_existing ON rp_existing.role_id = r.id
JOIN permission existing ON existing.id = rp_existing.permission_id
CROSS JOIN permission p
WHERE r.deleted_at IS NULL
  AND existing.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND existing.code = 'NOTIFICATION_VIEW'
  AND p.code = 'NOTIFICATION_PREFERENCE_MANAGE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

-- -----------------------------------------------------------------------------
-- 6. ui_screen rows for admin + profile pages
-- -----------------------------------------------------------------------------
INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'notification_preferences', '/profile/notifications', 'تفضيلات الإشعارات', 'Notification preferences',
       'Manage own opt-in/out preferences per channel and event.',
       510, TRUE, 'tune', FALSE,
       (SELECT id FROM permission WHERE code = 'NOTIFICATION_PREFERENCE_MANAGE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'notification_preferences');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'notification_channels', '/admin/notifications/channels', 'قنوات الإشعارات', 'Notification channels',
       'Manage webhook / Teams targets and signing secret references.',
       520, TRUE, 'hub', TRUE,
       (SELECT id FROM permission WHERE code = 'NOTIFICATION_CHANNEL_ADMIN' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'notification_channels');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'notification_outbox', '/admin/notifications/outbox', 'صندوق إصدار الإشعارات', 'Notification outbox',
       'List, re-queue, and cancel notification outbox rows including DLQ.',
       525, TRUE, 'forward_to_inbox', TRUE,
       (SELECT id FROM permission WHERE code = 'NOTIFICATION_CHANNEL_ADMIN' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'notification_outbox');
