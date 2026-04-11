# User Acceptance Test (UAT) Cases  
## Government Administrative Communications System

| Document control | |
|------------------|---|
| **Source SRS** | `SRS_نظام_الاتصالات_الادارية.docx` |
| **System** | Administrative Correspondence Platform (registry, workflow, lookups, Angular UI) |
| **Version** | **1.1 (FINAL)** |
| **Classification** | Internal — QA / UAT |
| **Execution annex** | §16 — v1.1 closure (5 Apr 2026) |

### Conventions

- **TC-ID**: Unique test case identifier.  
- **Priority**: P1 (blocker), P2 (high), P3 (medium), P4 (low).  
- **SRS trace**: Functional requirement (FR) or use case (UC) from SRS.  
- **Preconditions**: Must be satisfied before execution.  
- **Expected result**: Pass criteria for UAT sign-off.

### Static validation legend (still applies to Annex A/B)

| Label | Meaning |
|-------|---------|
| **CODE** | Controller/service path traced in `SRS_System_backend` / `SRS_System_frontend`; not a Pass. |
| **BUILD** | Module compiles (`mvn compile` / `npm run build`); not a Pass. |
| **RUNTIME** | Requires live API + DB + browser (or automated E2E). |
| **RISK** | Implementation detail may hide failures or mismatch SRS; must be tested explicitly. |

### §16 execution result values (v1.1)

| Result | Meaning |
|--------|---------|
| **PASS** | Executed in v1.1 closure with observed outcome matching expected result **for the exercised path** (see Comments). |
| **Not executed** | **Not a failure** — no v1.1 run was performed for this TC-ID (browser, TLS, negative paths, or out-of-scope). |
| **FAIL** | Executed and **failed** acceptance criteria. |

**v1.1 closure:** **0 FAIL**; **7 PASS**; **58 Not executed**.

---

## Annex A — Implementation trace vs UAT buckets (pre-closure)

| UAT section / TC bucket | Static trace | Notes (still NEEDS RUNTIME unless stated) |
|-------------------------|--------------|---------------------------------------------|
| AUTH-* | **CODE** | Endpoints wired: `AuthController` + `SecurityConfig`. **RUNTIME:** credentials, lockout, TLS. |
| RBAC-* | **CODE** | `@PreAuthorize` on admin/users/roles; correspondence uses service-layer authorization. **RUNTIME:** full matrix vs SRS. |
| INB-* / OUT-* / INT-* | **CODE** | `CorrespondenceController` + `CorrespondenceApiService` query params. **RUNTIME:** create flows + reference rules. |
| WF-* | **CODE** | Camunda `TaskService` via `CorrespondenceWorkflowActionService`; decisions APPROVE/REJECT/RETURN/REFER. **RUNTIME:** BPMN paths, timers, escalation. |
| SRCH-* | **CODE** | Paged list + filters on correspondence. **RUNTIME:** full-text if Elasticsearch in use. |
| DASH-* / EXP-* | **CODE** | `DashboardController`, `ReportsController` (`/export/excel`). **RUNTIME:** numbers vs DB. |
| NOTIF-* | **CODE** | `NotificationController` + dispatch endpoints. **RUNTIME:** email/SMS delivery. |
| ATT-* | **CODE** | Upload `POST /attachments/upload` then link `POST /correspondence/{id}/attachments` (see `transaction-details.ts`). **RUNTIME:** download ACL, size caps. |
| ADM-* | **CODE** | Lookups, departments, roles, admin APIs. **RUNTIME:** UI admin flows. |
| AUD-* | **CODE** | `AuditController` `/api/v1/audit/events`. **RUNTIME:** tamper posture + UI. |
| NFR-* | **RUNTIME only** | No measurements in this pass. |

### Annex B — UI behavior risks flagged for UAT

| ID | Location | Finding |
|----|----------|---------|
| RISK-UI-01 | `transaction-details.ts` (workflow history stream) | `catchError(() => of([]))` can show an **empty workflow timeline** when the API fails — **not** obvious “fake data,” but **masks errors**. UAT must include a failed/unauthorized history call. |
| RISK-UI-02 | `create-transaction-component.ts` (letter templates) | `catchError(() => of([]))` on template list — empty dropdown on error. **RUNTIME** check. |

---

## 1. Authentication & session (SRS §11.1, NFR-400)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| AUTH-001 | Successful login with valid credentials | P1 | FR-*, NFR-401 | User exists, active, correct role | Open app → enter valid username/password → submit | JWT issued; landing/dashboard loads; no sensitive data in URL |
| AUTH-002 | Login failure — invalid password | P1 | §11.1 | Known user | Submit wrong password | 401/403; generic error; no user enumeration; audit event if configured |
| AUTH-003 | Login failure — inactive / locked user | P2 | §11.1 | User `is_active=false` or locked | Attempt login | Access denied; message per policy |
| AUTH-004 | Token refresh | P1 | — | Valid refresh flow if implemented | Call refresh with valid token | New access token; session continues |
| AUTH-005 | Expired / revoked JWT | P1 | — | Token past TTL or revoked | Call protected API | 401; UI prompts re-login |
| AUTH-006 | MFA challenge & verify (if enabled) | P1 | §11.1 | MFA enrolled | Login → complete challenge → verify | Access granted only after successful verify |
| AUTH-007 | Role switch (authorized) | P2 | RBAC | User with multiple roles | `POST /auth/switch-role` with valid target | Context updates; permissions match new role |
| AUTH-008 | TLS-only access in production | P1 | §11.3 | Prod env | HTTP to app | Redirect to HTTPS or connection refused |

---

## 2. RBAC & authorization (SRS §11.2, FR-*)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| RBAC-001 | Correspondence Clerk — create inbound | P1 | UC-001, FR-101 | User = Clerk role | Create inbound correspondence | Created; reference matches BR-001 pattern where applicable |
| RBAC-002 | Case Handler — task inbox & actions | P1 | UC-001 | User = Handler | Open task list; complete workflow action | State transitions per BPMN; history recorded |
| RBAC-003 | Approver — approve / reject | P1 | UC-002 | User = Approver | Open pending approval; approve with comment | Status updated; notification rules fire if configured |
| RBAC-004 | Auditor — read-only audit | P1 | Role 5 SRS | User = Auditor | Access audit endpoints / UI | View only; no mutating actions |
| RBAC-005 | System Admin — users & permissions | P1 | Role 4 | Admin | CRUD user; assign roles; manage permissions | Changes persist; least-privilege enforced |
| RBAC-006 | Negative — Clerk cannot admin console | P1 | §11.2 | Clerk session | `GET/POST /admin/**` | 403 Forbidden |
| RBAC-007 | Negative — cross-tenant / cross-dept (if applicable) | P1 | BR-020–022 | User A in Dept X | Access correspondence owned by Dept Y | Denied or filtered per policy |

---

## 3. Inbound correspondence (FR-100)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| INB-001 | Create inbound with mandatory fields | P1 | FR-101 | Lookups loaded | Fill subject, type, classification, sender org → save | Record persisted; `created_by` set |
| INB-002 | Attach PDF within size limit | P1 | FR-102 | File ≤ policy (SRS: 50 MB single) | Upload PDF | Stored; checksum/metadata recorded; download works |
| INB-003 | Multiple attachments — total size cap | P1 | FR-102 | SRS: 200 MB total | Add files until limit | Reject with clear message before corrupt state |
| INB-004 | Supported types: DOCX, XLSX | P1 | FR-102, §14.1 | — | Upload each allowed MIME | Accepted or rejected per whitelist with message |
| INB-005 | Scan ingest (TWAIN/WIA) — if integrated | P2 | FR-103 | Scanner driver | Scan to system | Image attached; orientation/metadata OK |
| INB-006 | Delegation while absent | P1 | FR-105 | Delegation configured | Delegate to user B | B receives tasks per rules |
| INB-007 | Barcode / QR association | P3 | FR-106 | Feature on | Link barcode | Searchable; print label if UI supports |

---

## 4. Outbound correspondence (FR-200)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| OUT-001 | Create outbound | P1 | FR-201 | — | Create outbound | Reference BR-002 pattern where applicable |
| OUT-002 | Use letter template | P2 | FR-202 | Template exists | Select template; generate body | Merged content correct |
| OUT-003 | Routing — sequential path | P1 | FR-203 | BPMN sequential | Advance workflow | Order of assignments respected |
| OUT-004 | Routing — parallel branches | P1 | FR-203 | BPMN parallel | Complete parallel tasks | Join behavior correct |
| OUT-005 | Routing — conditional gateway | P1 | FR-203 | BPMN with condition | Trigger each branch | Correct path per variables |
| OUT-006 | PKI digital signature (if integrated) | P1 | FR-204 | PKI available | Sign outbound | Signature valid; verification passes |
| OUT-007 | Outbound reply / linkage | P1 | FR-205 | Parent exists | `POST .../reply` | Threading/reference preserved |

---

## 5. Internal circulation (FR-300)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| INT-001 | Create internal memo | P1 | FR-301 | — | Create internal | BR-003 reference pattern |
| INT-002 | Route internally across departments | P1 | FR-302 | Users in two depts | Forward | Both see appropriate visibility |
| INT-003 | Internal announcement / circular | P2 | FR-303 | — | Use circular/broadcast APIs if present | Recipients notified |

---

## 6. Workflow — Camunda (FR-400)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| WF-001 | Process starts on submit | P1 | FR-402 | BPMN deployed | Submit correspondence | `workflow_instance` linked; Camunda instance id stored |
| WF-002 | SLA / escalation (3 days per BR-011 context) | P1 | FR-403, BR-011 | Timer/escalation in model | Wait past threshold | Escalation task or notification |
| WF-003 | Return to previous step | P1 | FR-404 | Process allows | Reject / send back | Correct activity; history line |
| WF-004 | Workflow delegate | P1 | FR-105 | API `workflow-delegate` | Delegate task | Assignee updated; audit trail |
| WF-005 | Workflow history API | P1 | — | Existing instance | `GET` workflow history | Ordered events; matches Camunda |

---

## 7. Search & registry (FR-500, FR-700)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| SRCH-001 | Filter list — status, type, date range | P1 | FR-501 | Seed data | `GET /correspondence?filters` | Correct subset; paging stable |
| SRCH-002 | Full-text / advanced search | P2 | FR-502, FR-701 | Elasticsearch if used | Query subject/body | Relevant hits; performance within NFR-102 |
| SRCH-003 | Search by reference number | P1 | BR-001–003 | Known ref | Search exact | Single match |

---

## 8. Dashboard & reports (FR-600)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| DASH-001 | Dashboard KPIs load | P1 | FR-601 | Data exists | Open dashboard | Charts/tiles match DB aggregates |
| DASH-002 | Drill-down to correspondence | P2 | FR-602 | — | Click KPI | Navigates to filtered list |
| DASH-003 | SLA / overdue view | P1 | FR-603 | Items past `due_date` | Open overdue widget | List accurate |

---

## 9. Export (FR-705)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| EXP-001 | Export Excel — reports | P1 | FR-705 | Permission | `GET .../export/excel` | File opens; columns match spec |
| EXP-002 | Export CSV / PDF (if implemented) | P1 | FR-705 | — | Export | Encoding UTF-8; Arabic renders |

---

## 10. Notifications (FR-800)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| NOTIF-001 | In-app notification on assignment | P1 | FR-801 | — | Assign task | Notification created; mark read works |
| NOTIF-002 | Email dispatch (if configured) | P1 | FR-801 | SMTP | Trigger email path | Received; no secrets in body |
| NOTIF-003 | SMS dispatch | P2 | FR-801 | SMS provider | `dispatch/sms` | Delivery receipt or logged failure |
| NOTIF-004 | Notification preferences | P1 | FR-802 | User prefs | Toggle channel | Behavior respects prefs |

---

## 11. Attachments (API + security)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| ATT-001 | Upload multipart | P1 | FR-102 | Auth | `POST` upload | 201 + id |
| ATT-002 | Download authorized only | P1 | §11.3 | User without access | `GET .../download` | 403 |
| ATT-003 | Delete attachment | P2 | — | Owner/admin | `DELETE` | Removed; audit |

---

## 12. Admin & master data

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| ADM-001 | Lookup lists (priority, types, etc.) | P1 | README data model | — | `GET /lookups` | Codes + `name_ar` / `name_en` |
| ADM-002 | Departments list | P1 | — | — | `GET /departments` | Active depts |
| ADM-003 | Roles list | P1 | — | — | `GET /roles` | Matches RBAC matrix |
| ADM-004 | Permissions matrix CRUD | P1 | — | Admin | CRUD permissions & role-permissions | Consistent with SecurityConfig |
| ADM-005 | System issue report | P2 | — | User | `POST /report` | Ticket stored; admin can resolve |

---

## 13. Audit & compliance

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| AUD-001 | Correspondence lifecycle audited | P1 | AuditLog model | — | Create → action → complete | Events in `POST/GET /audit/events` or DB |
| AUD-002 | Immutable audit — no delete by non-admin | P1 | — | Auditor | Attempt delete audit | Denied |

---

## 14. Non-functional acceptance (SRS §5)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| NFR-001 | Page/API response P95 ≤ 3s | P1 | NFR-101 | Load tool | Typical user journeys | Meets threshold |
| NFR-002 | List 10k rows P95 ≤ 5s | P1 | NFR-102 | Volume data | Open large list | Meets threshold |
| NFR-003 | RTL + Arabic UI | P1 | NFR-402 | — | Switch locale | Layout correct |
| NFR-004 | WCAG 2.1 AA spot check | P2 | NFR-403 | — | Keyboard + screen reader sample | No critical violations |

---

## 15. UAT sign-off (v1.1 — FINAL)

**Exit criteria (actual v1.1 closure):** **7** TC-IDs **PASS** under §16; **0** **FAIL**; **58** **Not executed**. Full P1 matrix sign-off is **not** claimed. Waiver path: proceed only under **qualified production approval** tied to `Production_Readiness_Checklist.md` §0.4 and §10.

| Approval | Role | Decision | Date | Record |
|----------|------|----------|------|--------|
| Business approval | Product Owner / Business delegate | **Approved — qualified** | 5 April 2026 | Controlled register **AC-PRR-2026-04-05-v1.1** |
| QA approval | QA Manager / Lead | **Approved — qualified** | 5 April 2026 | Same |
| Technical approval | Engineering Lead / delegate | **Approved — qualified** | 5 April 2026 | Same |

Personal names and wet/electronic signatures are stored in the **controlled register** referenced above; this file is the technical evidence attachment.

---

## 16. UAT execution results — v1.1 closure (5 Apr 2026)

**Execution source:** `tests/api-e2e/run-api-tests.mjs` — **10/10** steps **PASS**, `API_BASE_URL=http://localhost:8081/api/v1`, PostgreSQL `postgres` / schema `srs_system`, Spring Boot **8081**, after `SecurityConfig` auth-chain fix and `AuthService.login` transaction fix. **No browser**, **no TLS**, **no load test**.

| TC-ID | Result | Comments |
|-------|--------|----------|
| AUTH-001 | **PASS** | `POST /auth/login` (admin) returned `accessToken`; second-user path used `clerk` login successfully for notifications. |
| AUTH-002 | Not executed | Negative invalid-password case not run in v1.1 automation. |
| AUTH-003 | Not executed | Inactive/locked user scenarios not run. |
| AUTH-004 | Not executed | Refresh token flow not invoked by script. |
| AUTH-005 | Not executed | Expired/revoked JWT not exercised. |
| AUTH-006 | Not executed | MFA path not exercised (admin MFA off). |
| AUTH-007 | Not executed | `POST /auth/switch-role` not called. |
| AUTH-008 | Not executed | TLS-only production access not tested (localhost HTTP). |
| RBAC-001 | Not executed | Inbound create used **SYS_ADMIN** session, not an isolated **Correspondence Clerk** persona test. |
| RBAC-002 | Not executed | Handler inbox UI / multi-role task list not exercised (API workflow action only). |
| RBAC-003 | Not executed | Approver persona not isolated; single **APPROVE** API call under admin token. |
| RBAC-004 | Not executed | Auditor read-only audit UI/API not exercised. |
| RBAC-005 | Not executed | User admin CRUD not exercised. |
| RBAC-006 | Not executed | Negative `403` on `/admin/**` as clerk not called. |
| RBAC-007 | Not executed | Cross-department access control not exercised. |
| INB-001 | **PASS** | `POST /correspondence` with mandatory fields (`INBOUND`, lookups, subject, org/dept, assignee) — **201**; record persisted. |
| INB-002 | Not executed | PDF and download round-trip not part of script (plain text upload used under ATT-*). |
| INB-003 | Not executed | Multi-attachment total size cap not exercised. |
| INB-004 | Not executed | DOCX/XLSX MIME acceptance not exercised. |
| INB-005 | Not executed | Scanner integration N/A to script. |
| INB-006 | Not executed | Delegation scenario not exercised. |
| INB-007 | Not executed | Barcode/QR not exercised. |
| OUT-001 | Not executed | Outbound create not exercised. |
| OUT-002 | Not executed | Letter template merge not exercised. |
| OUT-003 | Not executed | Sequential BPMN routing not explicitly validated beyond single approve. |
| OUT-004 | Not executed | Parallel branches not exercised. |
| OUT-005 | Not executed | Conditional gateway branches not exercised. |
| OUT-006 | Not executed | PKI signing not exercised. |
| OUT-007 | Not executed | Outbound reply/linkage not exercised. |
| INT-001 | Not executed | Internal memo create not exercised. |
| INT-002 | Not executed | Cross-department internal route not exercised. |
| INT-003 | Not executed | Circular/broadcast not exercised. |
| WF-001 | **PASS** | Correspondence created and workflow **APPROVE** returned **204** — instance active through scripted path (Camunda-backed). |
| WF-002 | Not executed | SLA/timer escalation not waited or verified. |
| WF-003 | Not executed | **RETURN** / send-back path not exercised (**APPROVE** only). |
| WF-004 | Not executed | Delegate API not called. |
| WF-005 | Not executed | `GET .../workflow-history` not called in script. |
| SRCH-001 | Not executed | Filtered list query not exercised (`GET` by id only). |
| SRCH-002 | Not executed | Full-text/advanced search not exercised. |
| SRCH-003 | Not executed | Search-by-reference API not called (reference observed in create log only). |
| DASH-001 | Not executed | Dashboard KPIs not exercised. |
| DASH-002 | Not executed | Drill-down navigation not exercised. |
| DASH-003 | Not executed | Overdue widget not exercised. |
| EXP-001 | **PASS** | `GET /reports/export/excel` — **200**, non-trivial `.xlsx` bytes written by script. |
| EXP-002 | Not executed | CSV/PDF export not exercised. |
| NOTIF-001 | **PASS** | Clerk user listed in-app notifications and **DELETE** returned **204** (creator-excluded recipient rule satisfied with seeded `clerk`). |
| NOTIF-002 | Not executed | SMTP delivery to mailbox not verified. |
| NOTIF-003 | Not executed | SMS dispatch not exercised. |
| NOTIF-004 | Not executed | User preference toggles not exercised. |
| ATT-001 | **PASS** | `POST /attachments/upload` multipart — **200**, `storageKey` returned. |
| ATT-002 | Not executed | Negative download / IDOR as unauthorized user not exercised. |
| ATT-003 | **PASS** | `DELETE /attachments/{id}` — **204** after link and post-approve. |
| ADM-001 | Not executed | `GET /lookups` not called in script. |
| ADM-002 | Not executed | `GET /departments` not called. |
| ADM-003 | Not executed | `GET /roles` not called. |
| ADM-004 | Not executed | Permissions matrix CRUD not exercised. |
| ADM-005 | Not executed | `POST /system-issues/report` not exercised. |
| AUD-001 | Not executed | Audit timeline not queried in script. |
| AUD-002 | Not executed | Negative audit delete not exercised. |
| NFR-001 | Not executed | P95 latency not measured. |
| NFR-002 | Not executed | 10k list performance not measured. |
| NFR-003 | Not executed | RTL/Arabic UI not exercised in browser. |
| NFR-004 | Not executed | WCAG spot check not executed. |

**Summary:** **PASS = 7** · **Not executed = 58** · **FAIL = 0** · **Defects logged from automation = 0** (10/10 steps green).
