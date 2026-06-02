-- =============================================================================
-- V19__public_verification_tokens.sql
--
-- Slice 6 — Public QR / print verification:
--
--   1. attachment_verification_token: long-lived (but revocable) opaque tokens
--      embedded in QR codes printed on outbound correspondence. Stores only
--      SHA-256(token_hash); the raw token is returned to the issuer exactly
--      once at issuance (mirrors Slice 5 attachment_download_token).
--   2. attachment_verification_access_log: forensic trail for every public scan
--      attempt (success or failure). Lives separately from audit_event since
--      the actor is anonymous — audit_event remains human-actor-only.
--   3. Permissions: ATTACHMENT_VERIFY_TOKEN_ISSUE, ATTACHMENT_VERIFY_TOKEN_VIEW
--      and idempotent role grants.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- 1. attachment_verification_token
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attachment_verification_token (
  id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  token_hash             VARCHAR(64)  NOT NULL,
  attachment_id          BIGINT       NOT NULL REFERENCES attachment (id) ON DELETE CASCADE,
  attachment_version_id  BIGINT       NOT NULL REFERENCES attachment_version (id) ON DELETE CASCADE,
  issued_by              UUID         NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  issued_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at             TIMESTAMPTZ,
  revoked_at             TIMESTAMPTZ,
  revoked_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  access_count           INTEGER      NOT NULL DEFAULT 0,
  last_accessed_at       TIMESTAMPTZ,
  created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_attachment_verification_token_expiry
    CHECK (expires_at IS NULL OR expires_at > issued_at),
  CONSTRAINT ck_attachment_verification_token_access_count
    CHECK (access_count >= 0)
);

COMMENT ON TABLE attachment_verification_token IS
  'Long-lived opaque tokens embedded in printed QR codes. Public scans hit the '
  'permitAll /api/v1/public/verify/{token} endpoint; only SHA-256(token) is '
  'stored, the raw value is returned to the issuer once at issuance.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_attachment_verification_token_hash
  ON attachment_verification_token (token_hash);

CREATE INDEX IF NOT EXISTS ix_attachment_verification_token_version
  ON attachment_verification_token (attachment_version_id);

CREATE INDEX IF NOT EXISTS ix_attachment_verification_token_issued_by
  ON attachment_verification_token (issued_by);

CREATE INDEX IF NOT EXISTS ix_attachment_verification_token_active
  ON attachment_verification_token (attachment_version_id)
  WHERE revoked_at IS NULL;

CREATE TRIGGER tr_attachment_verification_token_updated_at BEFORE UPDATE ON attachment_verification_token
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2. attachment_verification_access_log
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attachment_verification_access_log (
  id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  token_hash             VARCHAR(64)  NOT NULL,
  attachment_version_id  BIGINT REFERENCES attachment_version (id) ON DELETE SET NULL,
  accessed_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  ip_address             VARCHAR(64),
  user_agent             VARCHAR(512),
  success                BOOLEAN      NOT NULL,
  failure_reason         VARCHAR(64)
);

COMMENT ON TABLE attachment_verification_access_log IS
  'Forensic trail for every public verification scan (success or failure). '
  'Anonymous actor: ip + user-agent + token_hash. Distinct from audit_event '
  'which remains human-actor-only.';

CREATE INDEX IF NOT EXISTS ix_attachment_verification_access_log_token
  ON attachment_verification_access_log (token_hash, accessed_at DESC);

CREATE INDEX IF NOT EXISTS ix_attachment_verification_access_log_time
  ON attachment_verification_access_log (accessed_at DESC);

-- -----------------------------------------------------------------------------
-- 3. Permissions + role grants
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('ATTACHMENT_VERIFY_TOKEN_ISSUE',
   'إصدار رموز التحقق العامة',
   'Issue public verification tokens',
   'Generate and revoke QR / print verification tokens for attachment versions.',
   935),
  ('ATTACHMENT_VERIFY_TOKEN_VIEW',
   'عرض رموز التحقق العامة',
   'View public verification tokens',
   'List the verification tokens issued for an attachment version.',
   940)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- SYS_ADMIN gets both codes.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN ('ATTACHMENT_VERIFY_TOKEN_ISSUE', 'ATTACHMENT_VERIFY_TOKEN_VIEW')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Issuance follows ATTACHMENT_SIGN_CREATE: anyone who can sign can also stamp a
-- public QR onto the printed output (governance: same signer authority).
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp_existing ON rp_existing.role_id = r.id
JOIN permission existing ON existing.id = rp_existing.permission_id
CROSS JOIN permission p
WHERE r.deleted_at IS NULL
  AND existing.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND existing.code = 'ATTACHMENT_SIGN_CREATE'
  AND p.code = 'ATTACHMENT_VERIFY_TOKEN_ISSUE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );

-- Listing tokens for an attachment version follows ATTACHMENT_SIGN_VIEW.
-- AUDITOR gets VIEW explicitly (already has SIGN_VIEW; this is belt-and-braces).
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp_existing ON rp_existing.role_id = r.id
JOIN permission existing ON existing.id = rp_existing.permission_id
CROSS JOIN permission p
WHERE r.deleted_at IS NULL
  AND existing.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND existing.code = 'ATTACHMENT_SIGN_VIEW'
  AND p.code = 'ATTACHMENT_VERIFY_TOKEN_VIEW'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );
