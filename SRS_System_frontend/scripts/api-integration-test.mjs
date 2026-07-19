/**
 * SRS System — API ↔ frontend integration smoke test.
 * Usage: node scripts/api-integration-test.mjs [baseUrl]
 * Default: http://localhost:8080/api/v1
 */
const BASE = (process.argv[2] || 'http://localhost:8080/api/v1').replace(/\/$/, '');
const ORIGIN = BASE.replace(/\/api\/v1$/, '');
const LOGIN = { username: 'admin', password: 'admin' };

const results = [];
let token = '';

function pass(name, detail = '') {
  results.push({ name, ok: true, detail });
  console.log(`  ✓ ${name}${detail ? ` — ${detail}` : ''}`);
}

function fail(name, detail = '') {
  results.push({ name, ok: false, detail });
  console.log(`  ✗ ${name}${detail ? ` — ${detail}` : ''}`);
}

async function request(method, path, { body, expect = [200, 201], auth = true, root = false } = {}) {
  const headers = { Accept: 'application/json' };
  if (auth && token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const url = root ? `${ORIGIN}${path}` : `${BASE}${path}`;
  const res = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });

  let json = null;
  const text = await res.text();
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = { raw: text };
  }

  const ok = expect.includes(res.status);
  return { ok, status: res.status, json, path, method };
}

async function check(name, method, path, opts = {}) {
  try {
    const r = await request(method, path, opts);
    if (r.ok) {
      pass(name, `${r.status}`);
      return r.json;
    }
    fail(name, `HTTP ${r.status} ${r.json?.message || r.json?.detail || ''}`.trim());
    return null;
  } catch (err) {
    fail(name, err.message);
    return null;
  }
}

async function main() {
  console.log(`\nSRS API Integration Test\nBase: ${BASE}\n`);

  await check('GET /actuator/health', 'GET', '/actuator/health', {
    auth: false,
    root: true,
    expect: [200]
  });

  const loginRes = await check('POST /auth/login', 'POST', '/auth/login', {
    body: LOGIN,
    auth: false
  });
  token = loginRes?.accessToken || '';
  if (!token) {
    console.error('\nLogin failed — cannot continue.\n');
    process.exit(1);
  }

  await check('GET /me/capabilities', 'GET', '/me/capabilities');
  await check('GET /profile/me/navigation', 'GET', '/profile/me/navigation');
  await check('GET /users', 'GET', '/users?page=0&size=5');
  const missingUserId = crypto.randomUUID();
  await check('PATCH /users/toggle-active rejects missing user', 'PATCH', `/users/${missingUserId}/toggle-active`, {
    expect: [404]
  });
  await check('GET /correspondence', 'GET', '/correspondence?page=0&size=5');
  await check('GET /lookups/correspondence_type', 'GET', '/lookups/correspondence_type');
  await check('GET /admin/lookup-tables/catalog', 'GET', '/admin/lookup-tables/catalog');
  await check('GET /notifications', 'GET', '/notifications?page=0&size=5');
  await check('GET /dashboard', 'GET', '/dashboard');

  const passed = results.filter((r) => r.ok).length;
  const total = results.length;
  console.log(`\n${passed}/${total} passed\n`);
  process.exit(passed === total ? 0 : 1);
}

main();
