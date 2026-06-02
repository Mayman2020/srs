-- =============================================================================
-- V14__read_tracking_attachment_access.sql
--
-- Slice 1 of the defense-grade hardening phase
-- (see docs/enterprise-phase-defensive-hardening.md):
--
--   1. correspondence_read_receipt  : per-(correspondence,user) implicit
--                                      open tracking and optional acknowledgement.
--   2. attachment_access_log        : append-only log of every successful
--                                      attachment download / metadata view.
--   3. Two new canonical permissions for cross-user visibility of the above,
--      granted to SYS_ADMIN (other roles can be granted later via the admin
--      UI without a new migration).
--
-- No PostgreSQL ENUMs are introduced; codes are stable VARCHAR values, matching
-- the existing pattern used by audit_event.action_code.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- correspondence_read_receipt
-- One row per (correspondence, user) for as long as the receipt is not soft
-- deleted. open_count / last_opened_at are bumped on every detail view.
-- acknowledged_at is set the first time the user explicitly acknowledges.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS correspondence_read_receipt (
  id                       BIGSERIAL PRIMARY KEY,
  correspondence_id        UUID         NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  first_opened_at          TIMESTAMPTZ  NOT NULL,
  last_opened_at           TIMESTAMPTZ  NOT NULL,
  open_count               INTEGER      NOT NULL DEFAULT 1,
  acknowledged_at          TIMESTAMPTZ,
  acknowledgement_comment  TEXT,
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_correspondence_read_receipt_open_count_positive
    CHECK (open_count > 0)
);

COMMENT ON TABLE correspondence_read_receipt IS
  'Per-(correspondence,user) read tracking and explicit acknowledgement. '
  'open_count/last_opened_at are bumped on every authorized detail view; '
  'acknowledged_at is set once when the user clicks Acknowledge.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_correspondence_read_receipt_active
  ON correspondence_read_receipt (correspondence_id, user_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_correspondence_read_receipt_correspondence_first_opened
  ON correspondence_read_receipt (correspondence_id, first_opened_at);

CREATE INDEX IF NOT EXISTS ix_correspondence_read_receipt_user_acknowledged
  ON correspondence_read_receipt (user_id, acknowledged_at);

-- -----------------------------------------------------------------------------
-- attachment_access_log
-- Append-only log; no soft-delete columns. Used for forensic / audit queries
-- on attachment downloads and metadata access. Audit signal also flows into
-- audit_event for cross-resource searches; this table keeps the high-volume
-- access trail decoupled from the generic audit stream.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attachment_access_log (
  id                       BIGSERIAL PRIMARY KEY,
  attachment_version_id    BIGINT       NOT NULL REFERENCES attachment_version (id) ON DELETE CASCADE,
  attachment_id            BIGINT       NOT NULL REFERENCES attachment (id) ON DELETE CASCADE,
  correspondence_id        UUID         NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  user_id                  UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  action_code              VARCHAR(64)  NOT NULL,
  occurred_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  ip_address               VARCHAR(64),
  user_agent               VARCHAR(512),
  success                  BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by               UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_attachment_access_log_action
    CHECK (action_code IN ('DOWNLOAD', 'VIEW_METADATA'))
);

COMMENT ON TABLE attachment_access_log IS
  'Append-only audit trail of attachment downloads and metadata views; '
  'used for forensic queries and Slice 1 access-log views.';

CREATE INDEX IF NOT EXISTS ix_attachment_access_log_attachment_time
  ON attachment_access_log (attachment_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_attachment_access_log_correspondence_time
  ON attachment_access_log (correspondence_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_attachment_access_log_user_time
  ON attachment_access_log (user_id, occurred_at DESC);

-- -----------------------------------------------------------------------------
-- New canonical permissions (idempotent insert)
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('CORRESPONDENCE_READ_STATUS_VIEW',
   'عرض حالة قراءة المراسلات',
   'View correspondence read status',
   'See who has opened/acknowledged a correspondence (cross-user visibility).',
   900),
  ('ATTACHMENT_ACCESS_LOG_VIEW',
   'عرض سجل الوصول للمرفقات',
   'View attachment access log',
   'See history of attachment downloads and metadata views.',
   910)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- Grant new permissions to SYS_ADMIN (idempotent).
-- Other roles can opt in via the admin UI without a new migration.
-- -----------------------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND p.code IN ('CORRESPONDENCE_READ_STATUS_VIEW', 'ATTACHMENT_ACCESS_LOG_VIEW')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- AUDITOR already gets ADMIN_AUDIT_VIEW; grant the two new access-log views as
-- well so that auditors can investigate without additional admin steps.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'AUDITOR' AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND p.code IN ('CORRESPONDENCE_READ_STATUS_VIEW', 'ATTACHMENT_ACCESS_LOG_VIEW')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
