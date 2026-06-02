-- =============================================================================
-- V18__classified_attachments_and_signatures.sql
--
-- Slice 5 — Classified attachments + Digital signatures
-- (see docs/enterprise-phase-defensive-hardening.md §3 and §9):
--
--   1. attachment_version is extended with AES-256-GCM at-rest encryption
--      metadata (per-version DEK wrapped by a KEK identified by encryption_key_ref)
--      plus plaintext_sha256 (canonical content hash; signatures bind to this).
--   2. workflow_action_type gains a requires_signature boolean so an admin can
--      force a "sign before complete" gate on a workflow action.
--   3. document_signature is a per-attachment-version signature row produced by
--      the SigningKeyProvider SPI; status + verification_status track lifecycle.
--   4. attachment_download_token backs the new short-lived, single-use signed
--      download flow (token_hash stored, never the raw token).
--   5. Permissions ATTACHMENT_SIGN_VIEW, ATTACHMENT_SIGN_CREATE,
--      ATTACHMENT_SIGNATURE_ADMIN and role grants.
--
-- No PostgreSQL ENUMs; CHECK constraints for stable code sets only.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- 1. attachment_version: encryption + canonical hash columns
--
-- Pre-V18 rows stay valid: encryption_algo IS NULL means the blob on disk is
-- plaintext (legacy). The CHECK constraint forbids inconsistent combinations
-- on V18+ rows.
-- -----------------------------------------------------------------------------
ALTER TABLE attachment_version
  ADD COLUMN IF NOT EXISTS encryption_algo        VARCHAR(32),
  ADD COLUMN IF NOT EXISTS encryption_key_ref     VARCHAR(128),
  ADD COLUMN IF NOT EXISTS encryption_wrapped_dek BYTEA,
  ADD COLUMN IF NOT EXISTS encryption_iv          BYTEA,
  ADD COLUMN IF NOT EXISTS ciphertext_sha256      VARCHAR(64),
  ADD COLUMN IF NOT EXISTS plaintext_sha256       VARCHAR(64);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.check_constraints
    WHERE constraint_schema = current_schema()
      AND constraint_name   = 'ck_attachment_version_encryption_consistent'
  ) THEN
    ALTER TABLE attachment_version
      ADD CONSTRAINT ck_attachment_version_encryption_consistent
      CHECK (
        encryption_algo IS NULL
        OR (
          encryption_wrapped_dek IS NOT NULL
          AND encryption_iv      IS NOT NULL
          AND encryption_key_ref IS NOT NULL
        )
      );
  END IF;
END$$;

COMMENT ON COLUMN attachment_version.encryption_algo IS
  'AES_256_GCM when bytes on disk are encrypted; NULL = legacy plaintext blob.';
COMMENT ON COLUMN attachment_version.plaintext_sha256 IS
  'Canonical content hash (hex). Digital signatures bind to this value.';

-- -----------------------------------------------------------------------------
-- 2. workflow_action_type.requires_signature
--
-- Default FALSE keeps every existing transition behaving as before. Admins can
-- flip the flag for an action (e.g. APPROVE) once the system has signing keys
-- provisioned.
-- -----------------------------------------------------------------------------
ALTER TABLE workflow_action_type
  ADD COLUMN IF NOT EXISTS requires_signature BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN workflow_action_type.requires_signature IS
  'When TRUE, completing this action requires a VALID+VERIFIED document_signature '
  'on the latest version of every active attachment by the acting user.';

-- -----------------------------------------------------------------------------
-- 3. document_signature
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_signature (
  id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  attachment_version_id  BIGINT       NOT NULL REFERENCES attachment_version (id) ON DELETE CASCADE,
  signer_user_id         UUID         NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  algorithm              VARCHAR(32)  NOT NULL,
  canonical_hash_sha256  VARCHAR(64)  NOT NULL,
  signature_bytes        BYTEA        NOT NULL,
  key_ref                VARCHAR(256) NOT NULL,
  certificate_pem        TEXT,
  signed_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  status                 VARCHAR(16)  NOT NULL DEFAULT 'VALID',
  verification_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  verification_at        TIMESTAMPTZ,
  verification_detail    TEXT,
  revoked_at             TIMESTAMPTZ,
  revoked_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_document_signature_status
    CHECK (status IN ('VALID', 'REVOKED')),
  CONSTRAINT ck_document_signature_verification_status
    CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED'))
);

COMMENT ON TABLE document_signature IS
  'Per-attachment-version digital signature. The signature binds to '
  'canonical_hash_sha256 (== attachment_version.plaintext_sha256 at sign time).';

CREATE UNIQUE INDEX IF NOT EXISTS ux_document_signature_active
  ON document_signature (attachment_version_id, signer_user_id)
  WHERE status = 'VALID';

CREATE INDEX IF NOT EXISTS ix_document_signature_version
  ON document_signature (attachment_version_id);

CREATE INDEX IF NOT EXISTS ix_document_signature_signer
  ON document_signature (signer_user_id);

CREATE TRIGGER tr_document_signature_updated_at BEFORE UPDATE ON document_signature
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 4. attachment_download_token
--
-- Opaque short-lived single-use token. Only the SHA-256 of the token is stored;
-- the raw value is shown to the client once at intent issuance.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attachment_download_token (
  id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  token_hash             VARCHAR(64)  NOT NULL,
  attachment_id          BIGINT       NOT NULL REFERENCES attachment (id) ON DELETE CASCADE,
  attachment_version_id  BIGINT       NOT NULL REFERENCES attachment_version (id) ON DELETE CASCADE,
  user_id                UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  issued_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at             TIMESTAMPTZ  NOT NULL,
  consumed_at            TIMESTAMPTZ,
  revoked_at             TIMESTAMPTZ,
  ip_address             VARCHAR(64),
  user_agent             VARCHAR(512),
  created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by             UUID REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT ck_attachment_download_token_expiry
    CHECK (expires_at > issued_at)
);

COMMENT ON TABLE attachment_download_token IS
  'Short-lived, single-use opaque download tokens for the encrypted attachment '
  'stream. Stores only SHA-256(token); the raw token is returned to the caller '
  'exactly once at intent issuance.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_attachment_download_token_hash
  ON attachment_download_token (token_hash);

CREATE INDEX IF NOT EXISTS ix_attachment_download_token_expiry
  ON attachment_download_token (expires_at);

CREATE INDEX IF NOT EXISTS ix_attachment_download_token_user
  ON attachment_download_token (user_id, issued_at DESC);

CREATE TRIGGER tr_attachment_download_token_updated_at BEFORE UPDATE ON attachment_download_token
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- -----------------------------------------------------------------------------
-- 5. Permissions
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('ATTACHMENT_SIGN_VIEW',
   'عرض توقيعات المرفقات',
   'View attachment signatures',
   'Read the digital signature history of an attachment version.',
   920),
  ('ATTACHMENT_SIGN_CREATE',
   'توقيع المرفقات رقميًا',
   'Digitally sign attachments',
   'Create a digital signature on an attachment version using the system signing key.',
   925),
  ('ATTACHMENT_SIGNATURE_ADMIN',
   'إدارة التوقيعات (مشرف)',
   'Administer attachment signatures',
   'Revoke any attachment signature; force re-verification.',
   930)
) AS v(code, name_ar, name_en, description, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- SYS_ADMIN gets every new code.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.code IN ('ATTACHMENT_SIGN_VIEW', 'ATTACHMENT_SIGN_CREATE', 'ATTACHMENT_SIGNATURE_ADMIN')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- AUDITOR + GENERAL_MANAGER: read + administrative revoke (no signing).
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('AUDITOR', 'GENERAL_MANAGER') AND r.deleted_at IS NULL
  AND p.code IN ('ATTACHMENT_SIGN_VIEW', 'ATTACHMENT_SIGNATURE_ADMIN')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Every role that may currently update a correspondence may also sign its
-- attachments. ATTACHMENT_SIGN_VIEW follows CORRESPONDENCE_VIEW so anyone with
-- correspondence read access sees the signature panel.
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT r.id, p.id
FROM role r
JOIN role_permission rp_existing ON rp_existing.role_id = r.id
JOIN permission existing ON existing.id = rp_existing.permission_id
CROSS JOIN permission p
WHERE r.deleted_at IS NULL
  AND existing.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND (
    (existing.code = 'CORRESPONDENCE_UPDATE' AND p.code = 'ATTACHMENT_SIGN_CREATE')
    OR (existing.code = 'CORRESPONDENCE_VIEW'   AND p.code = 'ATTACHMENT_SIGN_VIEW')
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = r.id AND rp2.permission_id = p.id
  );
