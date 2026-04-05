/**
 * k6 load profile for Administrative Communications API.
 *
 * Env:
 *   BASE_URL   API root (default http://localhost:8081/api/v1)
 *   API_USER   (default admin)
 *   API_PASS   (default admin)
 *   ADMIN_UUID workflow first assignee (default b0000001-0000-4000-8000-000000000001)
 *
 * Run: k6 run ac-api-load.js
 */

import http from "k6/http";
import { check, sleep, group } from "k6";

const base = __ENV.BASE_URL || "http://localhost:8080/api/v1";
const user = __ENV.API_USER || "admin";
const pass = __ENV.API_PASS || "admin";
const adminUuid =
  __ENV.ADMIN_UUID || "b0000001-0000-4000-8000-000000000001";

/** NFR-101: transaction response ≤ 3s — threshold on API requests (excludes iteration sleep). */
export const options = {
  scenarios: {
    mixed_api: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 5 },
        { duration: "60s", target: 20 },
        { duration: "30s", target: 20 },
        { duration: "20s", target: 0 },
      ],
      gracefulRampDown: "20s",
    },
  },
  // NFR-101: typical transaction ≤ 3s (p95 — compare on your hardware baseline).
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<3000"],
  },
};

export function setup() {
  const loginRes = http.post(
    `${base}/auth/login`,
    JSON.stringify({ username: user, password: pass }),
    { headers: { "Content-Type": "application/json" } }
  );
  if (loginRes.status !== 200) {
    throw new Error(
      `setup login failed: ${loginRes.status} body=${String(loginRes.body).slice(0, 200)}`
    );
  }
  const body = loginRes.json();
  if (!body.accessToken) {
    throw new Error("setup: no accessToken in login response");
  }
  return { token: body.accessToken };
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  };
}

export default function (data) {
  const h = authHeaders(data.token);

  group("login_warmup", () => {
    const r = http.post(
      `${base}/auth/login`,
      JSON.stringify({ username: user, password: pass }),
      { headers: { "Content-Type": "application/json" } }
    );
    check(r, { "login 200": (res) => res.status === 200 });
  });

  group("correspondence_list", () => {
    const r = http.get(`${base}/correspondence?page=0&size=20`, h);
    check(r, {
      "list 200": (res) => res.status === 200,
      "list has page": (res) => {
        if (res.status !== 200) return false;
        const j = res.json();
        return Array.isArray(j.content);
      },
    });
  });

  let detailId = null;
  group("correspondence_detail", () => {
    const lr = http.get(`${base}/correspondence?page=0&size=1`, h);
    if (lr.status !== 200) return;
    const j = lr.json();
    if (j.content && j.content.length > 0 && j.content[0].id) {
      detailId = j.content[0].id;
    }
    if (!detailId) return;
    const dr = http.get(`${base}/correspondence/${detailId}`, h);
    check(dr, { "detail 200": (res) => res.status === 200 });
  });

  group("workflow_create_and_approve", () => {
    const createBody = JSON.stringify({
      correspondenceTypeCode: "INBOUND",
      priorityCode: "NORMAL",
      confidentialityCode: "NORMAL",
      classificationCode: "GEN",
      subject: `k6-${__VU}-${__ITER}-${Date.now()}`,
      description: "k6 load",
      senderOrganizationId: 1,
      ownerDepartmentId: 1,
      workflowFirstAssigneeUserId: adminUuid,
    });
    const cr = http.post(`${base}/correspondence`, createBody, h);
    const okCreate = check(cr, {
      "create 201": (res) => res.status === 201,
    });
    if (!okCreate || cr.status !== 201) return;
    const cid = cr.json("id");
    if (!cid) return;
    const wf = http.post(
      `${base}/correspondence/${cid}/actions`,
      JSON.stringify({ action: "APPROVE", comment: null }),
      h
    );
    check(wf, {
      "workflow 204": (res) => res.status === 204,
    });
  });

  group("attachment_upload", () => {
    const payload = `k6-upload-${__VU}-${__ITER}-${Date.now()}\n`;
    const file = http.file(payload, "k6-upload.txt", "text/plain");
    const res = http.post(`${base}/attachments/upload`, file, {
      headers: { Authorization: `Bearer ${data.token}` },
    });
    check(res, {
      "upload 200": (r) => r.status === 200,
      "upload has key": (r) => {
        if (r.status !== 200) return false;
        const j = r.json();
        return j && typeof j.storageKey === "string";
      },
    });
  });

  group("reports_excel_export", () => {
    const r = http.get(`${base}/reports/export/excel`, {
      headers: { Authorization: `Bearer ${data.token}` },
    });
    check(r, {
      "export 200": (res) => res.status === 200 && res.body.length > 200,
    });
  });

  sleep(0.3);
}
