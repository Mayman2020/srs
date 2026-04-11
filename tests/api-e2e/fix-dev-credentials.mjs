/**
 * One-off: align admin password with V10 seed and ensure clerk exists for notification E2E.
 * Uses same defaults as application.yml (user postgres, password admin; DB postgres, schema srs_system).
 */
import pg from 'pg';

const c = new pg.Client({
  host: process.env.PGHOST || 'localhost',
  port: Number(process.env.PGPORT || 5432),
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD || 'admin',
  database: process.env.PGDATABASE || 'postgres',
});

await c.connect();
const u = await c.query(
  `UPDATE app_user SET password_hash = $1, mfa_enabled = false
   WHERE username = 'admin' AND deleted_at IS NULL RETURNING username`,
  ['{noop}admin']
);
console.log('admin password reset rows:', u.rowCount);

await c.query(`
INSERT INTO app_user (id, username, password_hash, full_name_ar, full_name_en, email, department_id, is_active)
SELECT 'c0000001-0000-4000-8000-000000000001', 'clerk', '{noop}clerk', 'Clerk', 'Clerk', 'clerk@local.invalid', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'clerk' AND deleted_at IS NULL)
`);
const r2 = await c.query(`
INSERT INTO user_role (app_user_id, role_id)
SELECT 'c0000001-0000-4000-8000-000000000001', r.id FROM role r WHERE r.code = 'CORRESP_CLERK'
AND EXISTS (SELECT 1 FROM app_user u WHERE u.id = 'c0000001-0000-4000-8000-000000000001')
AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.app_user_id = 'c0000001-0000-4000-8000-000000000001' AND ur.role_id = r.id)
`);
console.log('clerk role insert rows:', r2.rowCount);
await c.end();
