-- V22: password reset tokens, letter template admin permission + nav screen
SET search_path TO srs_system, public;

CREATE TABLE IF NOT EXISTS password_reset_token (
  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL,
  expires_at  TIMESTAMPTZ  NOT NULL,
  consumed    BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_password_reset_token_user
  ON password_reset_token (user_id);

CREATE INDEX IF NOT EXISTS ix_password_reset_token_hash
  ON password_reset_token (token_hash)
  WHERE consumed = FALSE;

INSERT INTO permission (code, name_ar, name_en, sort_order)
SELECT 'LETTER_TEMPLATE_MANAGE', 'إدارة قوالب الخطابات', 'Manage letter templates', 860
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'LETTER_TEMPLATE_MANAGE' AND deleted_at IS NULL);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('SYS_ADMIN', 'CORRESP_MGR')
  AND p.code = 'LETTER_TEMPLATE_MANAGE'
  AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO ui_screen (
  code, route_path, name_ar, name_en, sort_order, show_in_shell_nav, required_permission_id
)
SELECT
  'letter_templates_admin',
  '/admin/letter-templates',
  'قوالب الخطابات',
  'Letter templates',
  860,
  TRUE,
  (SELECT id FROM permission WHERE code = 'LETTER_TEMPLATE_MANAGE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'letter_templates_admin');
