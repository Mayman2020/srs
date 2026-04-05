/**
 * Real API validation — Administrative Communications System
 * Base URL default: http://localhost:8080/api/v1
 *
 * Env:
 *   API_BASE_URL   (default http://localhost:8080/api/v1)
 *   API_USERNAME   (default admin)
 *   API_PASSWORD   (default admin)
 *   API_SECOND_USERNAME / API_SECOND_PASSWORD — optional "clerk" for inbox + delete notification test
 */

import axios from 'axios';
import FormData from 'form-data';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const BASE =
  process.env.API_BASE_URL?.replace(/\/$/, '') || 'http://localhost:8080/api/v1';
const USER = process.env.API_USERNAME || 'admin';
const PASS = process.env.API_PASSWORD || 'admin';
const SECOND_USER = process.env.API_SECOND_USERNAME || 'clerk';
const SECOND_PASS = process.env.API_SECOND_PASSWORD || 'clerk';

const ADMIN_UUID = 'b0000001-0000-4000-8000-000000000001';

let pass = 0;
let fail = 0;

function ok(name, condition, detail = '') {
  if (condition) {
    console.log(`PASS  ${name}${detail ? ` — ${detail}` : ''}`);
    pass++;
  } else {
    console.log(`FAIL  ${name}${detail ? ` — ${detail}` : ''}`);
    fail++;
  }
}

function failErr(name, err) {
  const st = err.response?.status;
  const body = err.response?.data;
  const msg =
    st != null
      ? `HTTP ${st} ${typeof body === 'object' ? JSON.stringify(body) : body}`
      : err.message;
  console.log(`FAIL  ${name} — ${msg}`);
  fail++;
}

async function login(username, password) {
  const { data } = await axios.post(`${BASE}/auth/login`, { username, password });
  if (!data.accessToken) throw new Error('No accessToken in login response');
  return data;
}

function client(token) {
  return axios.create({
    baseURL: BASE,
    headers: { Authorization: `Bearer ${token}` },
    validateStatus: () => true,
  });
}

async function main() {
  console.log(`\n=== API E2E ===\nBASE: ${BASE}\n`);

  let token;
  let correspondenceId;
  let attachmentRowId;
  let clerkToken;
  let notificationId;

  // --- AUTH: login ---
  try {
    const session = await login(USER, PASS);
    token = session.accessToken;
    ok(
      'AUTH login',
      token.length > 20,
      `userId=${session.userId} role=${session.currentRole}`
    );
  } catch (e) {
    failErr('AUTH login', e);
    console.error('\nAbort: cannot authenticate.');
    process.exit(1);
  }

  const api = client(token);

  // --- CORRESPONDENCE: create ---
  const createBody = {
    correspondenceTypeCode: 'INBOUND',
    priorityCode: 'NORMAL',
    confidentialityCode: 'NORMAL',
    classificationCode: 'GEN',
    subject: `API-E2E ${new Date().toISOString()}`,
    description: 'Automated API test run',
    senderOrganizationId: 1,
    ownerDepartmentId: 1,
    workflowFirstAssigneeUserId: ADMIN_UUID,
  };

  try {
    const cr = await api.post('/correspondence', createBody);
    ok(
      'CORRESPONDENCE create',
      cr.status === 201 && cr.data?.id,
      `id=${cr.data?.id} ref=${cr.data?.referenceNumber}`
    );
    if (cr.status !== 201) {
      console.log('      body:', cr.data);
    } else {
      correspondenceId = cr.data.id;
    }
  } catch (e) {
    failErr('CORRESPONDENCE create', e);
  }

  if (!correspondenceId) {
    console.error('\nAbort: no correspondence id.');
    process.exit(1);
  }

  // --- NOTIFICATIONS (clerk inbox): list + delete — recipients = owner dept excluding actor ---
  try {
    const s = await login(SECOND_USER, SECOND_PASS);
    clerkToken = s.accessToken;
    const clerkApi = client(clerkToken);
    const list = await clerkApi.get('/notifications', { params: { page: 0, size: 50 } });
    ok(
      'NOTIFICATIONS list (second user)',
      list.status === 200 && Array.isArray(list.data?.content),
      `count=${list.data?.content?.length ?? 'n/a'}`
    );
    const first = list.data?.content?.[0];
    if (first?.id) {
      notificationId = first.id;
      const del = await clerkApi.delete(`/notifications/${notificationId}`);
      ok('NOTIFICATIONS delete', del.status === 204, `id=${notificationId}`);
    } else {
      console.log(
        'FAIL  NOTIFICATIONS delete — no inbox row (run seed-second-user.sql; clerk must share ownerDepartmentId with admin create)'
      );
      fail++;
    }
  } catch (e) {
    console.log(
      'FAIL  NOTIFICATIONS (second user) — login or request failed. Apply seed-second-user.sql and set API_SECOND_USERNAME/PASSWORD if needed.'
    );
    failErr('NOTIFICATIONS second-user flow', e);
  }

  // --- CORRESPONDENCE: get ---
  try {
    const g = await api.get(`/correspondence/${correspondenceId}`);
    ok(
      'CORRESPONDENCE get',
      g.status === 200 && g.data?.id === correspondenceId,
      g.data?.referenceNumber
    );
  } catch (e) {
    failErr('CORRESPONDENCE get', e);
  }

  // --- ATTACHMENTS: upload (before workflow complete) ---
  let storageKey;
  let uploadSize;
  let uploadMime;
  try {
    const form = new FormData();
    const buf = Buffer.from(`e2e-${Date.now()}\n`, 'utf8');
    form.append('file', buf, { filename: 'api-e2e.txt', contentType: 'text/plain' });
    const up = await axios.post(`${BASE}/attachments/upload`, form, {
      headers: { ...form.getHeaders(), Authorization: `Bearer ${token}` },
      maxBodyLength: Infinity,
      maxContentLength: Infinity,
    });
    ok(
      'ATTACHMENTS upload',
      up.status === 200 && up.data?.storageKey,
      `key=${up.data?.storageKey}`
    );
    storageKey = up.data.storageKey;
    uploadSize = up.data.byteSize;
    uploadMime = up.data.mimeType;
  } catch (e) {
    failErr('ATTACHMENTS upload', e);
  }

  // --- ATTACHMENTS: link ---
  if (storageKey) {
    try {
      const link = await api.post(`/correspondence/${correspondenceId}/attachments`, {
        displayName: 'api-e2e.txt',
        storageKey,
        byteSize: uploadSize,
        mimeType: uploadMime || 'text/plain',
      });
      ok(
        'ATTACHMENTS link to correspondence',
        link.status === 201 && link.data?.id != null,
        `attachmentId=${link.data?.id}`
      );
      attachmentRowId = link.data?.id;
    } catch (e) {
      failErr('ATTACHMENTS link', e);
    }
  }

  // --- WORKFLOW: action (APPROVE) ---
  try {
    const wf = await api.post(`/correspondence/${correspondenceId}/actions`, {
      action: 'APPROVE',
      comment: null,
    });
    ok('WORKFLOW action APPROVE', wf.status === 204, '204 No Content');
  } catch (e) {
    failErr('WORKFLOW action APPROVE', e);
  }

  // --- ATTACHMENTS: delete ---
  if (attachmentRowId != null) {
    try {
      const del = await api.delete(`/attachments/${attachmentRowId}`);
      ok('ATTACHMENTS delete', del.status === 204, `id=${attachmentRowId}`);
    } catch (e) {
      failErr('ATTACHMENTS delete', e);
    }
  } else {
    console.log('FAIL  ATTACHMENTS delete — no attachment id');
    fail++;
  }

  // --- REPORTS: excel ---
  try {
    const ex = await axios.get(`${BASE}/reports/export/excel`, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'arraybuffer',
      validateStatus: () => true,
    });
    const ct = ex.headers['content-type'] || '';
    const isXlsx =
      ct.includes('spreadsheetml') || ct.includes('octet-stream');
    const outPath = path.join(__dirname, 'correspondences-export.xlsx');
    if (ex.status === 200 && ex.data?.byteLength > 100) {
      fs.writeFileSync(outPath, Buffer.from(ex.data));
      ok(
        'REPORTS export excel',
        isXlsx || ex.data.byteLength > 500,
        `saved ${outPath} (${ex.data.byteLength} bytes)`
      );
    } else {
      console.log(`FAIL  REPORTS export excel — status=${ex.status} bytes=${ex.data?.byteLength}`);
      fail++;
    }
  } catch (e) {
    failErr('REPORTS export excel', e);
  }

  console.log(`\n=== Summary: ${pass} PASS, ${fail} FAIL ===\n`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
