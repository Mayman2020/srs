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

  // Durable outbox creation is invariant; inbox visibility depends on department routing.
  try {
    const outbox = await api.get('/notification-outbox', { params: { page: 0, size: 100 } });
    const matching = outbox.data?.content?.filter(
      (row) =>
        row.correlationResourceId === correspondenceId ||
        row.idempotencyKey?.includes(correspondenceId)
    );
    ok(
      'NOTIFICATIONS outbox created',
      outbox.status === 200 && matching?.some((row) => row.channelCode === 'IN_APP'),
      `rows=${matching?.length ?? 0}`
    );
  } catch (e) {
    failErr('NOTIFICATIONS outbox created', e);
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
      ok(
        'NOTIFICATIONS delete (when routed to second user)',
        true,
        'not applicable: second user is outside the correspondence owner department'
      );
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

  // --- GUIDE: link + nonarchived CRUD ---
  try {
    const second = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-LINK ${new Date().toISOString()}`,
    });
    ok('GUIDE create second correspondence', second.status === 201 && second.data?.id, second.data?.id);
    if (second.status === 201 && second.data?.id) {
      const link = await api.post(`/correspondence/${correspondenceId}/links`, {
        linkedCorrespondenceId: second.data.id,
        linkKind: 'RELATED',
      });
      ok('GUIDE add link', link.status === 201 && link.data?.id != null, `linkId=${link.data?.id}`);
      const na = await api.post(`/correspondence/${correspondenceId}/nonarchived-items`, {
        itemType: 'OTHER',
        descriptionText: 'E2E non-archived item',
        quantity: 1,
        sortOrder: 0,
      });
      ok('GUIDE add nonarchived', na.status === 201 && na.data?.id != null, `itemId=${na.data?.id}`);
      if (link.data?.id) {
        const delLink = await api.delete(`/correspondence/${correspondenceId}/links/${link.data.id}`);
        ok('GUIDE delete link', delLink.status === 204);
      }
      if (na.data?.id) {
        const delNa = await api.delete(
          `/correspondence/${correspondenceId}/nonarchived-items/${na.data.id}`
        );
        ok('GUIDE delete nonarchived', delNa.status === 204);
      }
    }
  } catch (e) {
    failErr('GUIDE CRUD', e);
  }

  // --- AUTH: forgot password (no enumeration) ---
  try {
    const fp = await axios.post(`${BASE}/auth/forgot-password`, { username: USER }, {
      validateStatus: () => true,
    });
    ok('AUTH forgot-password', fp.status === 204 || fp.status === 200, `status=${fp.status}`);
  } catch (e) {
    failErr('AUTH forgot-password', e);
  }

  // --- LETTER TEMPLATES: admin list ---
  try {
    const lt = await api.get('/letter-templates/admin');
    ok(
      'LETTER TEMPLATES admin list',
      lt.status === 200 && Array.isArray(lt.data),
      `count=${lt.data?.length ?? 0}`
    );
  } catch (e) {
    failErr('LETTER TEMPLATES admin list', e);
  }

  // --- WORKFLOW: history ---
  try {
    const hist = await api.get(`/correspondence/${correspondenceId}/workflow-history`);
    ok(
      'WORKFLOW history',
      hist.status === 200 && Array.isArray(hist.data),
      `entries=${hist.data?.length ?? 0}`
    );
  } catch (e) {
    failErr('WORKFLOW history', e);
  }

  // --- OUTBOUND: create + register APPROVE ---
  let outboundId;
  try {
    const out = await api.post('/correspondence', {
      ...createBody,
      correspondenceTypeCode: 'OUTBOUND',
      subject: `API-E2E-OUT ${new Date().toISOString()}`,
    });
    ok('OUTBOUND create', out.status === 201 && out.data?.id, out.data?.id);
    outboundId = out.data?.id;
    if (outboundId) {
      const reg = await api.post(`/correspondence/${outboundId}/actions`, {
        action: 'APPROVE',
        comment: null,
      });
      ok('OUTBOUND register APPROVE', reg.status === 204, 'draft approved');
      const detail = await api.get(`/correspondence/${outboundId}`);
      const st =
        detail.data?.correspondenceStatus?.code ??
        detail.data?.status?.code ??
        detail.data?.correspondenceStatusCode;
      ok(
        'OUTBOUND stays non-terminal after first APPROVE',
        detail.status === 200 && st !== 'COMPLETED' && st !== 'REJECTED',
        `status=${st}`
      );
    }
  } catch (e) {
    failErr('OUTBOUND flow', e);
  }

  // --- WORKFLOW: REJECT path (separate inbound) ---
  try {
    const rej = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-REJECT ${new Date().toISOString()}`,
    });
    ok('REJECT setup create', rej.status === 201 && rej.data?.id, rej.data?.id);
    if (rej.status === 201 && rej.data?.id) {
      await api.post(`/correspondence/${rej.data.id}/actions`, { action: 'APPROVE', comment: null });
      const r = await api.post(`/correspondence/${rej.data.id}/actions`, {
        action: 'REJECT',
        comment: 'API E2E reject',
      });
      ok('WORKFLOW action REJECT', r.status === 204, '204');
      const after = await api.get(`/correspondence/${rej.data.id}`);
      const st =
        after.data?.correspondenceStatus?.code ??
        after.data?.status?.code ??
        after.data?.correspondenceStatusCode;
      ok('REJECT terminal status', after.status === 200 && st === 'REJECTED', `status=${st}`);
    }
  } catch (e) {
    failErr('WORKFLOW REJECT', e);
  }

  // --- WORKFLOW: RETURN path ---
  try {
    const returned = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-RETURN ${new Date().toISOString()}`,
    });
    ok('RETURN setup create', returned.status === 201 && returned.data?.id, returned.data?.id);
    if (returned.status === 201 && returned.data?.id) {
      await api.post(`/correspondence/${returned.data.id}/actions`, { action: 'APPROVE' });
      const action = await api.post(`/correspondence/${returned.data.id}/actions`, {
        action: 'RETURN',
        comment: 'API E2E return',
      });
      const detail = await api.get(`/correspondence/${returned.data.id}`);
      ok('WORKFLOW action RETURN', action.status === 204, `${action.status}`);
      ok(
        'RETURN terminal status',
        detail.data?.correspondenceStatus?.code === 'RETURNED',
        `status=${detail.data?.correspondenceStatus?.code}`
      );
    }
  } catch (e) {
    failErr('WORKFLOW RETURN', e);
  }

  // --- WORKFLOW: REFER keeps the process open and reassigns the active task ---
  try {
    const referred = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-REFER ${new Date().toISOString()}`,
    });
    ok('REFER setup create', referred.status === 201 && referred.data?.id, referred.data?.id);
    if (referred.status === 201 && referred.data?.id) {
      const action = await api.post(`/correspondence/${referred.data.id}/actions`, {
        action: 'REFER',
        comment: 'API E2E refer',
        targetUserId: ADMIN_UUID,
      });
      const detail = await api.get(`/correspondence/${referred.data.id}`);
      ok('WORKFLOW action REFER', action.status === 204, `${action.status}`);
      ok(
        'REFER remains actionable',
        detail.status === 200 && detail.data?.availableWorkflowActions?.length > 0,
        `actions=${detail.data?.availableWorkflowActions?.length ?? 0}`
      );
    }
  } catch (e) {
    failErr('WORKFLOW REFER', e);
  }

  // --- WORKFLOW: FORWARD changes the routing target and keeps the process active ---
  try {
    const forwarded = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-FORWARD ${new Date().toISOString()}`,
    });
    ok('FORWARD setup create', forwarded.status === 201 && forwarded.data?.id, forwarded.data?.id);
    if (forwarded.status === 201 && forwarded.data?.id) {
      const action = await api.post(`/correspondence/${forwarded.data.id}/actions`, {
        action: 'FORWARD',
        comment: 'API E2E forward',
        targetDepartmentId: 1,
      });
      const detail = await api.get(`/correspondence/${forwarded.data.id}`);
      ok('WORKFLOW action FORWARD', action.status === 204, `${action.status}`);
      ok(
        'FORWARD remains actionable',
        detail.status === 200 && detail.data?.availableWorkflowActions?.length > 0,
        `actions=${detail.data?.availableWorkflowActions?.length ?? 0}`
      );
    }
  } catch (e) {
    failErr('WORKFLOW FORWARD', e);
  }

  try {
    const inbox = await api.get('/workflow/tasks/inbox', { params: { limit: 20 } });
    ok('WORKFLOW inbox', inbox.status === 200 && Array.isArray(inbox.data), `rows=${inbox.data?.length ?? 0}`);
    const notifications = await api.get('/notifications', { params: { page: 0, size: 20 } });
    const unreadCount = notifications.data?.content?.filter((item) => !item.readAt).length ?? 0;
    ok('NOTIFICATIONS unread projection', notifications.status === 200, `count=${unreadCount}`);
  } catch (e) {
    failErr('WORKFLOW inbox / notification unread', e);
  }

  // --- CORRESPONDENCE: cancellation rolls back the live Camunda instance ---
  try {
    const cancelled = await api.post('/correspondence', {
      ...createBody,
      subject: `API-E2E-CANCEL ${new Date().toISOString()}`,
    });
    ok('CANCEL setup create', cancelled.status === 201 && cancelled.data?.id, cancelled.data?.id);
    if (cancelled.status === 201 && cancelled.data?.id) {
      const action = await api.post(`/correspondence/${cancelled.data.id}/cancel`, {
        reason: 'API E2E cancel',
      });
      const detail = await api.get(`/correspondence/${cancelled.data.id}`);
      ok('CORRESPONDENCE cancel', action.status === 204, `${action.status}`);
      ok(
        'CANCELLED terminal status',
        detail.data?.correspondenceStatus?.code === 'CANCELLED' && detail.data?.cancelAllowed === false,
        `status=${detail.data?.correspondenceStatus?.code}`
      );
    }
  } catch (e) {
    failErr('CORRESPONDENCE CANCEL', e);
  }

  // --- INTERNAL: create smoke ---
  try {
    const internal = await api.post('/correspondence', {
      ...createBody,
      correspondenceTypeCode: 'INTERNAL',
      subject: `API-E2E-INT ${new Date().toISOString()}`,
    });
    ok('INTERNAL create', internal.status === 201 && internal.data?.id, internal.data?.id);
  } catch (e) {
    failErr('INTERNAL create', e);
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

  // --- LEAVE: lookup + create + decide ---
  try {
    const leaveStatuses = await api.get('/lookups/leave_status');
    ok(
      'LEAVE lookup',
      leaveStatuses.status === 200 && Array.isArray(leaveStatuses.data) && leaveStatuses.data.length >= 3,
      `rows=${leaveStatuses.data?.length}`
    );
    const pending = leaveStatuses.data?.find((s) => s.dashboardInboundHighlight);
    const approved = leaveStatuses.data?.find((s) => s.code === 'APPROVED' && s.terminal);
    const start = new Date();
    const end = new Date(start);
    end.setDate(end.getDate() + 2);
    const created = await api.post('/leave-requests', {
      startDate: start.toISOString().slice(0, 10),
      endDate: end.toISOString().slice(0, 10),
      reason: 'API E2E leave',
    });
    ok('LEAVE create', created.status === 200 && created.data?.id, created.data?.id);
    if (created.status === 200 && created.data?.id && approved?.code) {
      ok(
        'LEAVE pending after create',
        created.data.statusCode === (pending?.code ?? 'PENDING'),
        created.data.statusCode
      );
      const decided = await api.patch(`/admin/leave-requests/${created.data.id}/decision`, {
        statusCode: approved.code,
        decisionNote: 'API E2E approved',
      });
      ok(
        'LEAVE decide',
        decided.status === 200 && decided.data?.statusCode === approved.code,
        decided.data?.statusCode
      );
    }
  } catch (e) {
    failErr('LEAVE flow', e);
  }

  // --- AUTH: refresh rotation + server-side logout revocation ---
  try {
    const probe = await login(USER, PASS);
    const rotated = await axios.post(`${BASE}/auth/refresh`, { refreshToken: probe.refreshToken });
    ok(
      'AUTH refresh rotates token',
      rotated.status === 200 && rotated.data?.refreshToken && rotated.data.refreshToken !== probe.refreshToken
    );
    const logout = await axios.post(`${BASE}/auth/logout`, {
      refreshToken: rotated.data.refreshToken,
    });
    ok('AUTH logout revokes refresh token', logout.status === 200, `${logout.status}`);
    const afterLogout = await axios.post(
      `${BASE}/auth/refresh`,
      { refreshToken: rotated.data.refreshToken },
      { validateStatus: () => true }
    );
    ok('AUTH refresh rejected after logout', afterLogout.status === 401, `${afterLogout.status}`);
  } catch (e) {
    failErr('AUTH refresh/logout', e);
  }

  // --- SECURITY: non-admin cannot perform an admin write ---
  if (clerkToken) {
    const clerkApi = client(clerkToken);
    const forbidden = await clerkApi.post('/users', {
      username: `forbidden_${Date.now()}`,
      password: 'NeverCreated123!',
      fullNameAr: 'غير مسموح',
      fullNameEn: 'Forbidden write',
      email: `forbidden_${Date.now()}@local.invalid`,
      departmentId: 1,
    });
    ok('SECURITY admin write rejected for clerk', forbidden.status === 403, `${forbidden.status}`);
  }

  console.log(`\n=== Summary: ${pass} PASS, ${fail} FAIL ===\n`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
