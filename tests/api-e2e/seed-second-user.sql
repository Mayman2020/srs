-- Optional: second user in department 1 so "correspondence created" notifications are delivered
-- (recipients = active users in owner department EXCLUDING the actor). Run against your ac_communications DB.
-- Password is {noop}clerk (Spring Security noop encoder).

INSERT INTO app_user (
  id, username, password_hash, full_name_ar, full_name_en, email, department_id, is_active
)
SELECT
  'c0000001-0000-4000-8000-000000000001',
  'clerk',
  '{noop}clerk',
  'موظف اختبار',
  'QA clerk',
  'clerk@local.invalid',
  1,
  TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'clerk' AND deleted_at IS NULL);

INSERT INTO user_role (app_user_id, role_id)
SELECT 'c0000001-0000-4000-8000-000000000001', r.id
FROM role r
WHERE r.code = 'CORRESP_CLERK'
  AND EXISTS (SELECT 1 FROM app_user u WHERE u.id = 'c0000001-0000-4000-8000-000000000001')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur
    WHERE ur.app_user_id = 'c0000001-0000-4000-8000-000000000001' AND ur.role_id = r.id
  );
