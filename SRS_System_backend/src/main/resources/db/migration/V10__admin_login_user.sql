-- Dev / bootstrap human login (change password in non-dev environments).
INSERT INTO app_user (
  id, username, password_hash, full_name_ar, full_name_en, email, department_id, is_active
)
SELECT
  'b0000001-0000-4000-8000-000000000001',
  'admin',
  '{noop}admin',
  'مدير النظام',
  'System administrator',
  'admin@local.invalid',
  1,
  TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'admin' AND deleted_at IS NULL);

INSERT INTO user_role (app_user_id, role_id)
SELECT 'b0000001-0000-4000-8000-000000000001', r.id
FROM role r
WHERE r.code = 'SYS_ADMIN'
  AND EXISTS (SELECT 1 FROM app_user u WHERE u.id = 'b0000001-0000-4000-8000-000000000001')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur
    WHERE ur.app_user_id = 'b0000001-0000-4000-8000-000000000001' AND ur.role_id = r.id
  );
