-- =============================================================================
-- V20__retention_policy_and_legal_hold.sql
--
-- Slice 6 — Retention engine + Legal hold (no cold tier in this slice):
--
--   1. retention_policy: declarative TTL + action (HARD_DELETE / ANONYMIZE /
--      RETAIN_INDEFINITELY) per resource class (CORRESPONDENCE,
--      ATTACHMENT_VERSION, AUDIT_EVENT, ATTACHMENT_ACCESS_LOG, NOTIFICATION,
--      DOCUMENT_SIGNATURE, ATTACHMENT_DOWNLOAD_TOKEN). Optional scoping via
--      correspondence_type_id + confidentiality_id.
--   2. legal_hold: pin specific correspondences (or blanket holds with NULL
--      correspondence_id) so retention/deletion is suppressed until released.
--   3. archive_transition_log: append-only forensic trail produced by
--      RetentionLifecycleJob. Even SKIPPED_LEGAL_HOLD / SKIPPED_DRY_RUN runs
--      leave a row.
--   4. Permissions: RETENTION_POLICY_VIEW/MANAGE, LEGAL_HOLD_VIEW/MANAGE,
--      RETENTION_LOG_VIEW; idempotent role grants.
--   5. Seed defaults (idempotent) for the resource classes above.
--   6. ui_screen rows for the admin pages.
--
-- No PostgreSQL ENUMs; CHECK constraints for stable code sets only.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- 0. Drop the V1 legacy retention_policy if it's still the old shape.
--
-- The V1 baseline defined `retention_policy` with (id BIGSERIAL, retention_days,
-- warn_before_days, classification_id, …) and never wired it to runtime code
-- (no seeds in V1; no Java references). Slice 6 replaces it with an
-- incompatible UUID-keyed shape (code, applies_to, retain_for_days,
-- action_after, …). The CREATE TABLE IF NOT EXISTS below is a no-op against the
-- legacy shape, which then breaks every subsequent statement that touches
-- `code` / `applies_to`. Drop the legacy table only when it is still the V1
-- shape so re-running Slice 6 on a fresh DB also stays safe.
DO $$
BEGIN
  IF EXISTS (
       SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'srs_system' AND table_name = 'retention_policy'
     )
     AND NOT EXISTS (
       SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'srs_system'
          AND table_name   = 'retention_policy'
          AND column_name  = 'code'
     ) THEN
    EXECUTE 'DROP TABLE srs_system.retention_policy CASCADE';
  END IF;
END$$;

-- -----------------------------------------------------------------------------
-- 1. retention_policy
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS retention_policy (
  id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  code                     VARCHAR(64)  NOT NULL,
  name_ar                  TEXT,
  name_en                  TEXT,
  description              TEXT,
  applies_to               VARCHAR(32)  NOT NULL,
  correspondence_type_id   BIGINT REFERENCES correspondence_type (id) ON DELETE SET NULL,
  confidentiality_id       BIGINT REFERENCES confidentiality (id) ON DELETE SET NULL,
  retain_for_days          INTEGER      NOT NULL,
  action_after             VARCHAR(16)  NOT NULL,
  enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_retention_policy_applies_to
    CHECK (applies_to IN (
      'CORRESPONDENCE',
      'ATTACHMENT_VERSION',
      'AUDIT_EVENT',
      'ATTACHMENT_ACCESS_LOG',
      'NOTIFICATION',
      'DOCUMENT_SIGNATURE',
      'ATTACHMENT_DOWNLOAD_TOKEN'
    )),
  CONSTRAINT ck_retention_policy_action_after
    CHECK (action_after IN ('HARD_DELETE', 'ANONYMIZE', 'RETAIN_INDEFINITELY')),
  CONSTRAINT ck_retention_policy_retain_for_days
    CHECK (retain_for_days >= 0)
);

COMMENT ON TABLE retention_policy IS
  'Declarative retention rule applied hourly by RetentionLifecycleJob. '
  'Slice 6 does not implement a cold tier; action_after is the terminal step.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_retention_policy_code
  ON retention_policy (code)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_retention_policy_applies_to
  ON retention_policy (applies_to)
  WHERE enabled = TRUE AND deleted_at IS NULL;

CREATE TRIGGER tr_retention_policy_updated_at BEFORE UPDATE ON retention_policy
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2. legal_hold
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS legal_hold (
  id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  correspondence_id   UUID REFERENCES correspondence (id) ON DELETE CASCADE,
  reason              TEXT         NOT NULL,
  placed_by           UUID         NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  placed_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  released_at         TIMESTAMPTZ,
  released_by         UUID REFERENCES app_user (id) ON DELETE SET NULL,
  release_reason      TEXT,
  deleted_at          TIMESTAMPTZ,
  deleted_by          UUID,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by          UUID REFERENCES app_user (id) ON DELETE SET NULL
);

COMMENT ON TABLE legal_hold IS
  'Pin a correspondence (or every correspondence, with NULL correspondence_id) '
  'so retention + deletion is suppressed until the hold is released. '
  'LegalHoldService.assertNotHeld(correspondenceId) is wired into '
  'CorrespondenceDeletionService and AttachmentDeletionService.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_legal_hold_active_correspondence
  ON legal_hold (correspondence_id)
  WHERE released_at IS NULL AND deleted_at IS NULL AND correspondence_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_legal_hold_blanket_active
  ON legal_hold (id)
  WHERE released_at IS NULL AND deleted_at IS NULL AND correspondence_id IS NULL;

CREATE INDEX IF NOT EXISTS ix_legal_hold_correspondence
  ON legal_hold (correspondence_id);

CREATE TRIGGER tr_legal_hold_updated_at BEFORE UPDATE ON legal_hold
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 3. archive_transition_log
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS archive_transition_log (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  applied_to      VARCHAR(32)  NOT NULL,
  resource_id     VARCHAR(64)  NOT NULL,
  policy_id       UUID REFERENCES retention_policy (id) ON DELETE SET NULL,
  legal_hold_id   UUID REFERENCES legal_hold (id) ON DELETE SET NULL,
  action          VARCHAR(24)  NOT NULL,
  executed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  executed_by     UUID REFERENCES app_user (id) ON DELETE SET NULL,
  detail_json     TEXT,
  CONSTRAINT ck_archive_transition_log_action
    CHECK (action IN (
      'HARD_DELETE',
      'ANONYMIZE',
      'SKIPPED_LEGAL_HOLD',
      'SKIPPED_DRY_RUN',
      'FAILED'
    ))
);

COMMENT ON TABLE archive_transition_log IS
  'Append-only forensic trail of every retention decision. SKIPPED_DRY_RUN '
  'rows during dry-run mode are the auditor''s preview of what would happen '
  'with dry-run=false.';

CREATE INDEX IF NOT EXISTS ix_archive_transition_log_executed_at
  ON archive_transition_log (executed_at DESC);

CREATE INDEX IF NOT EXISTS ix_archive_transition_log_policy
  ON archive_transition_log (policy_id);

CREATE INDEX IF NOT EXISTS ix_archive_transition_log_resource
  ON archive_transition_log (applied_to, resource_id);

-- -----------------------------------------------------------------------------
-- 4. Permissions
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('RETENTION_POLICY_VIEW',
   'عرض سياسات الاستبقاء',
   'View retention policies',
   'Read retention policy configuration and seeded defaults.',
   1000),
  ('RETENTION_POLICY_MANAGE',
   'إدارة سياسات الاستبقاء',
   'Manage retention policies',
   'Create, edit, enable/disable retention policies.',
   1010),
  ('RETENTION_LOG_VIEW',
   'عرض سجل الاستبقاء',
   'View retention log',
   'Read the archive_transition_log forensic feed.',
   1020),
  ('LEGAL_HOLD_VIEW',
   'عرض الاحتجازات القانونية',
   'View legal holds',
   'Read currently active and historical legal holds.',
   1030),
  ('LEGAL_HOLD_MANAGE',
   'إدارة الاحتجازات القانونية',
   'Manage legal holds',
   'Place and release legal holds; reason is mandatory on both ends.',
   1040)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- SYS_ADMIN gets everything.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN (
    'RETENTION_POLICY_VIEW',
    'RETENTION_POLICY_MANAGE',
    'RETENTION_LOG_VIEW',
    'LEGAL_HOLD_VIEW',
    'LEGAL_HOLD_MANAGE'
  )
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- AUDITOR gets read-only access across the entire retention stack.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'AUDITOR' AND r.deleted_at IS NULL
  AND p.code IN ('RETENTION_POLICY_VIEW', 'RETENTION_LOG_VIEW', 'LEGAL_HOLD_VIEW')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- CORRESP_MGR can place/release legal holds in addition to SYS_ADMIN.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'CORRESP_MGR' AND r.deleted_at IS NULL
  AND p.code IN ('LEGAL_HOLD_VIEW', 'LEGAL_HOLD_MANAGE')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- -----------------------------------------------------------------------------
-- 5. Seed default retention policies (idempotent on code)
-- -----------------------------------------------------------------------------
INSERT INTO retention_policy (code, name_ar, name_en, description, applies_to, retain_for_days, action_after, enabled)
SELECT v.code, v.name_ar, v.name_en, v.description, v.applies_to, v.retain_for_days, v.action_after, TRUE
FROM (VALUES
  ('CORRESP_DEFAULT_7Y',  'الافتراضي للمراسلات (7 سنوات)', 'Default correspondence retention (7 years)',
   'Soft-deleted correspondences and their attachments are purged 7 years after deletion.',
   'CORRESPONDENCE', 2555, 'HARD_DELETE'),
  ('AUDIT_EVENT_10Y',     'سجل التدقيق (10 سنوات)',         'Audit events (10 years)',
   'audit_event rows are hard-deleted after 10 years.',
   'AUDIT_EVENT', 3650, 'HARD_DELETE'),
  ('ACCESS_LOG_3Y',       'سجل الوصول إلى المرفقات (3 سنوات)', 'Attachment access log (3 years)',
   'attachment_access_log rows are hard-deleted after 3 years.',
   'ATTACHMENT_ACCESS_LOG', 1095, 'HARD_DELETE'),
  ('NOTIFICATION_1Y',     'الإشعارات (سنة)',                'Notifications (1 year)',
   'Read or unread notification rows are hard-deleted after 1 year.',
   'NOTIFICATION', 365, 'HARD_DELETE'),
  ('DOWNLOAD_TOKEN_1D',   'رموز التحميل (يوم واحد)',         'Download tokens (1 day)',
   'attachment_download_token rows are hard-deleted 1 day after expiry. '
   'Re-expresses the existing AttachmentDownloadTokenCleanupJob behavior as a retention policy.',
   'ATTACHMENT_DOWNLOAD_TOKEN', 1, 'HARD_DELETE')
) AS v(code, name_ar, name_en, description, applies_to, retain_for_days, action_after)
WHERE NOT EXISTS (
  SELECT 1 FROM retention_policy rp WHERE rp.code = v.code AND rp.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 6. ui_screen rows for the admin pages
-- -----------------------------------------------------------------------------
INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'retention_policies', '/admin/retention/policies', 'سياسات الاستبقاء', 'Retention policies',
       'Configure retention policies and review seed defaults.',
       420, TRUE, 'policy', TRUE,
       (SELECT id FROM permission WHERE code = 'RETENTION_POLICY_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'retention_policies');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'legal_holds', '/admin/retention/legal-holds', 'الاحتجازات القانونية', 'Legal holds',
       'Place and release legal holds on correspondences.',
       425, TRUE, 'gavel', TRUE,
       (SELECT id FROM permission WHERE code = 'LEGAL_HOLD_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'legal_holds');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'retention_log', '/admin/retention/log', 'سجل الاستبقاء', 'Retention log',
       'Forensic feed of retention lifecycle actions.',
       430, TRUE, 'history', TRUE,
       (SELECT id FROM permission WHERE code = 'RETENTION_LOG_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'retention_log');
