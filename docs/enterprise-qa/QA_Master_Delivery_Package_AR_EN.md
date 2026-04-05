<div align="center">

| | |
|:--:|--:|
| | **وثيقة رسمية — Official QA Deliverable** |
| | **Classification / التصنيف:** Internal — Quality Assurance |
| | **Reference SRS / مرجع المواصفات:** `SRS_نظام_الاتصالات_الادارية.docx` |

<br/>

# نظام الاتصالات الإدارية  
## Government Administrative Communications System

### حزمة تسليم ضمان الجودة الموحّدة  
### Master Quality Assurance (QA) Delivery Package

<br/>

| **المشروع / Project** | Administrative Communications Platform (Spring Boot · Camunda · Angular · PostgreSQL) |
| **إصدار الوثيقة / Document version** | **1.0** |
| **التاريخ / Date** | **5 April 2026** |
| **حالة الوثيقة / Status** | Draft for official submission — جاهز للمراجعة والاعتماد |

<br/>

---

*End of cover page — تنتهي صفحة الغلاف*

</div>

<div style="page-break-after: always;"></div>

<span id="sec-toc"></span>

## جدول المحتويات | Table of Contents

| # | Section (AR) | Section (EN) | Anchor |
|---|----------------|--------------|--------|
| — | [صفحة الغلاف](#نظام-الاتصالات-الإدارية) | Cover | (above) |
| 1 | [المقدمة ونطاق الوثيقة](#sec-1) | Scope & conventions | `sec-1` |
| 2 | [اختبارات قبول المستخدم (UAT)](#sec-2) | User Acceptance Test cases | `sec-2` |
| 3 | [سيناريوهات الحالات الحدية](#sec-3) | Edge case scenarios | `sec-3` |
| 4 | [سيناريوهات الفشل](#sec-4) | Failure scenarios (network · data · permissions) | `sec-4` |
| 5 | [سجل مخاطر الأداء](#sec-5) | Performance risks register | `sec-5` |
| 6 | [قائمة جاهزية الإنتاج (Go/No-Go)](#sec-6) | Production readiness checklist | `sec-6` |
| 7 | [التوقيع والاعتماد الرسمي](#sec-7) | Official sign-off (Business · QA · Technical Lead) | `sec-7` |

**ملاحظة للتصدير إلى Word/PDF:**  
1) افتح `word-export/QA_Master_ForWord.html` في Microsoft Word ثم **حفظ باسم** بصيغة `.docx` (موصى به).  
2) بعد أي تعديل على هذا الملف Markdown، نفّذ `word-export/Convert-MasterToWordHtml.ps1` لإعادة توليد الـ HTML.  
**Export:** Open `word-export/QA_Master_ForWord.html` in Word → **Save As** `.docx`. After editing this `.md`, run `Convert-MasterToWordHtml.ps1` to refresh the HTML.

---

<span id="sec-1"></span>

## 1. المقدمة ونطاق الوثيقة | Document scope and purpose

### 1.1 الغرض | Purpose

توحّد هذه الوثيقة مخرجات ضمان الجودة الخاصة بنظام الاتصالات الإدارية وفق **مواصفات المتطلبات البرمجية (SRS)** المشار إليها، وتشمل: **UAT**، **الحالات الحدية**، **سيناريوهات الفشل**، **مخاطر الأداء**، و**قائمة جاهزية الإنتاج**.  
This master document consolidates all QA deliverables for the Government Administrative Communications System in line with the referenced SRS, including UAT cases, edge cases, failure scenarios, performance risks, and production readiness.

### 1.2 الجمهور المستهدف | Audience

| الجهة | Role |
|--------|------|
| الأعمال / وحدات المراسلات | Business / correspondence units |
| ضمان الجودة | QA / UAT |
| الهندسة والتقنية | Technical Lead / Engineering |
| الأمن والامتثال | Security / compliance (as applicable) |

### 1.3 الاصطلاحات | Conventions (UAT)

| المصطلح | Definition |
|---------|------------|
| **TC-ID** | معرّف حالة الاختبار الفريد |
| **Priority** | P1 blocker · P2 high · P3 medium · P4 low |
| **SRS trace** | ارتباط بـ FR / UC / NFR في الـ SRS |
| **Expected result** | معيار القبول لإغلاق UAT |

---

<span id="sec-2"></span>

## 2. اختبارات قبول المستخدم (UAT) | User Acceptance Test Cases

*المتطلبات الوظيفية مذكورة بصيغة SRS (FR-100 … FR-800) حيث ينطبق ذلك.*

### 2.1 المصادقة والجلسة | Authentication & session (SRS §11.1, NFR-400)

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

### 2.2 التحكم بالوصول حسب الدور (RBAC) | RBAC & authorization (SRS §11.2)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| RBAC-001 | Correspondence Clerk — create inbound | P1 | UC-001, FR-101 | User = Clerk role | Create inbound correspondence | Created; reference matches BR-001 pattern where applicable |
| RBAC-002 | Case Handler — task inbox & actions | P1 | UC-001 | User = Handler | Open task list; complete workflow action | State transitions per BPMN; history recorded |
| RBAC-003 | Approver — approve / reject | P1 | UC-002 | User = Approver | Open pending approval; approve with comment | Status updated; notification rules fire if configured |
| RBAC-004 | Auditor — read-only audit | P1 | دور المراجع في SRS | User = Auditor | Access audit endpoints / UI | View only; no mutating actions |
| RBAC-005 | System Admin — users & permissions | P1 | دور مسؤول النظام | Admin | CRUD user; assign roles; manage permissions | Changes persist; least-privilege enforced |
| RBAC-006 | Negative — Clerk cannot admin console | P1 | §11.2 | Clerk session | `GET/POST /admin/**` | 403 Forbidden |
| RBAC-007 | Negative — cross-dept / visibility | P1 | BR-020–022 | User A in Dept X | Access correspondence owned by Dept Y | Denied or filtered per policy |

### 2.3 المراسلات الواردة | Inbound correspondence (FR-100)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| INB-001 | Create inbound with mandatory fields | P1 | FR-101 | Lookups loaded | Fill subject, type, classification, sender org → save | Record persisted; `created_by` set |
| INB-002 | Attach PDF within size limit | P1 | FR-102 | File ≤ policy (50 MB single per SRS) | Upload PDF | Stored; checksum/metadata recorded; download works |
| INB-003 | Multiple attachments — total size cap | P1 | FR-102 | 200 MB total per SRS | Add files until limit | Reject with clear message before corrupt state |
| INB-004 | Supported types: DOCX, XLSX | P1 | FR-102, §14.1 | — | Upload each allowed MIME | Accepted or rejected per whitelist with message |
| INB-005 | Scan ingest (TWAIN/WIA) — if integrated | P2 | FR-103 | Scanner driver | Scan to system | Image attached; orientation/metadata OK |
| INB-006 | Delegation while absent | P1 | FR-105 | Delegation configured | Delegate to user B | B receives tasks per rules |
| INB-007 | Barcode / QR association | P3 | FR-106 | Feature on | Link barcode | Searchable; print label if UI supports |

### 2.4 المراسلات الصادرة | Outbound correspondence (FR-200)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| OUT-001 | Create outbound | P1 | FR-201 | — | Create outbound | Reference BR-002 pattern where applicable |
| OUT-002 | Use letter template | P2 | FR-202 | Template exists | Select template; generate body | Merged content correct |
| OUT-003 | Routing — sequential path | P1 | FR-203 | BPMN sequential | Advance workflow | Order of assignments respected |
| OUT-004 | Routing — parallel branches | P1 | FR-203 | BPMN parallel | Complete parallel tasks | Join behavior correct |
| OUT-005 | Routing — conditional gateway | P1 | FR-203 | BPMN with condition | Trigger each branch | Correct path per variables |
| OUT-006 | PKI digital signature (if integrated) | P1 | FR-204 | PKI available | Sign outbound | Signature valid; verification passes |
| OUT-007 | Outbound reply / linkage | P1 | FR-205 | Parent exists | `POST .../reply` | Threading/reference preserved |

### 2.5 التداول الداخلي | Internal circulation (FR-300)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| INT-001 | Create internal memo | P1 | FR-301 | — | Create internal | BR-003 reference pattern |
| INT-002 | Route internally across departments | P1 | FR-302 | Users in two depts | Forward | Both see appropriate visibility |
| INT-003 | Internal announcement / circular | P2 | FR-303 | — | Use circular/broadcast APIs if present | Recipients notified |

### 2.6 سير العمل — Workflow (Camunda) | Workflow engine (FR-400)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| WF-001 | Process starts on submit | P1 | FR-402 | BPMN deployed | Submit correspondence | `workflow_instance` linked; Camunda instance id stored |
| WF-002 | SLA / escalation (e.g. BR-011 context) | P1 | FR-403, BR-011 | Timer/escalation in model | Wait past threshold | Escalation task or notification |
| WF-003 | Return to previous step | P1 | FR-404 | Process allows | Reject / send back | Correct activity; history line |
| WF-004 | Workflow delegate | P1 | FR-105 | API `workflow-delegate` | Delegate task | Assignee updated; audit trail |
| WF-005 | Workflow history API | P1 | — | Existing instance | `GET` workflow history | Ordered events; matches Camunda |

### 2.7 البحث والسجل | Search & registry (FR-500, FR-700)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| SRCH-001 | Filter list — status, type, date range | P1 | FR-501 | Seed data | `GET /correspondence?filters` | Correct subset; paging stable |
| SRCH-002 | Full-text / advanced search | P2 | FR-502, FR-701 | Elasticsearch if used | Query subject/body | Relevant hits; performance within NFR-102 |
| SRCH-003 | Search by reference number | P1 | BR-001–003 | Known ref | Search exact | Single match |

### 2.8 لوحة المعلومات والتقارير | Dashboard & reports (FR-600)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| DASH-001 | Dashboard KPIs load | P1 | FR-601 | Data exists | Open dashboard | Charts/tiles match DB aggregates |
| DASH-002 | Drill-down to correspondence | P2 | FR-602 | — | Click KPI | Navigates to filtered list |
| DASH-003 | SLA / overdue view | P1 | FR-603 | Items past `due_date` | Open overdue widget | List accurate |

### 2.9 التصدير | Export (FR-705)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| EXP-001 | Export Excel — reports | P1 | FR-705 | Permission | `GET .../export/excel` | File opens; columns match spec |
| EXP-002 | Export CSV / PDF (if implemented) | P1 | FR-705 | — | Export | Encoding UTF-8; Arabic renders |

### 2.10 الإشعارات | Notifications (FR-800)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| NOTIF-001 | In-app notification on assignment | P1 | FR-801 | — | Assign task | Notification created; mark read works |
| NOTIF-002 | Email dispatch (if configured) | P1 | FR-801 | SMTP | Trigger email path | Received; no secrets in body |
| NOTIF-003 | SMS dispatch | P2 | FR-801 | SMS provider | `dispatch/sms` | Delivery receipt or logged failure |
| NOTIF-004 | Notification preferences | P1 | FR-802 | User prefs | Toggle channel | Behavior respects prefs |

### 2.11 المرفقات | Attachments (API + security)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| ATT-001 | Upload multipart | P1 | FR-102 | Auth | `POST` upload | 201 + id |
| ATT-002 | Download authorized only | P1 | §11.3 | User without access | `GET .../download` | 403 |
| ATT-003 | Delete attachment | P2 | — | Owner/admin | `DELETE` | Removed; audit |

### 2.12 الإدارة والبيانات المرجعية | Admin & master data

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| ADM-001 | Lookup lists (priority, types, etc.) | P1 | Data model | — | `GET /lookups` | Codes + `name_ar` / `name_en` |
| ADM-002 | Departments list | P1 | — | — | `GET /departments` | Active depts |
| ADM-003 | Roles list | P1 | — | — | `GET /roles` | Matches RBAC matrix |
| ADM-004 | Permissions matrix CRUD | P1 | — | Admin | CRUD permissions & role-permissions | Consistent with SecurityConfig |
| ADM-005 | System issue report | P2 | — | User | `POST /report` | Ticket stored; admin can resolve |

### 2.13 التدقيق والامتثال | Audit & compliance

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| AUD-001 | Correspondence lifecycle audited | P1 | AuditLog model | — | Create → action → complete | Events in `POST/GET /audit/events` or DB |
| AUD-002 | Immutable audit — no delete by non-admin | P1 | — | Auditor | Attempt delete audit | Denied |

### 2.14 المتطلبات غير الوظيفية — قبول مختصر | Non-functional acceptance (SRS §5)

| TC-ID | Title | Priority | SRS trace | Preconditions | Steps | Expected result |
|-------|--------|----------|-----------|---------------|-------|-----------------|
| NFR-001 | Page/API response P95 ≤ 3s | P1 | NFR-101 | Load tool | Typical user journeys | Meets threshold |
| NFR-002 | List 10k rows P95 ≤ 5s | P1 | NFR-102 | Volume data | Open large list | Meets threshold |
| NFR-003 | RTL + Arabic UI | P1 | NFR-402 | — | Switch locale | Layout correct |
| NFR-004 | WCAG 2.1 AA spot check | P2 | NFR-403 | — | Keyboard + screen reader sample | No critical violations |

### 2.15 معايير إغلاق UAT | UAT exit criteria

- اجتياز جميع حالات **P1**؛ ومعالجة **P2** بتصحيح أو تنازل رسمي أو تأجيل مع توثيق.  
- All **P1** cases passed; **P2** items closed, waived with approval, or deferred with documented plan.

---

<span id="sec-3"></span>

## 3. سيناريوهات الحالات الحدية | Edge case scenarios

### 3.1 حدود البيانات والتنسيق | Data boundary & format

| ID | Area | Scenario | Expected behavior |
|----|------|----------|-------------------|
| EC-D-01 | Subject / text | Subject exactly 500 chars (SRS `VARCHAR(500)`) | Accept; no truncation without warning |
| EC-D-02 | Subject / text | Subject 501+ chars | Reject or truncate with validation message |
| EC-D-03 | Reference | Duplicate `reference_number` collision | Unique constraint; user-friendly error |
| EC-D-04 | Dates | `due_date` before `created_at` | Validation per BR |
| EC-D-05 | Dates | Timezone: user in UTC+3 vs server UTC | Stored instant consistent; display localized |
| EC-D-06 | UUID | Malformed `id` in path (`not-a-uuid`) | 400 Bad Request |
| EC-D-07 | UUID | Valid UUID non-existent | 404 Not Found |
| EC-D-08 | Filters | Empty string vs null query params | Consistent semantics (ignore vs match empty) |
| EC-D-09 | Filters | SQL/wildcard `%` and `_` in search | Escaped or documented behavior |
| EC-D-10 | Pagination | `page=-1`, `size=0`, `size=999999` | Bounded page size; safe defaults |
| EC-D-11 | Sort | Sort by non-whitelisted column | Ignored or 400 |
| EC-D-12 | JSON | Extra unknown fields in POST body | Ignored (forward compatible) or strict 400 |

### 3.2 المرفقات | Attachments

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-A-01 | File name 255+ chars, Arabic / Unicode | Sanitized storage name; original preserved in metadata if supported |
| EC-A-02 | Double extension `report.pdf.exe` | Blocked by MIME + extension policy |
| EC-A-03 | Polyglot file (valid PDF + script) | Content inspection if policy requires |
| EC-A-04 | Zero-byte file | Reject with clear reason |
| EC-A-05 | Single file 49 MB / 51 MB vs 50 MB SRS | Hard limit at policy |
| EC-A-06 | Concurrent uploads same correspondence | No lost updates; versioning if designed |
| EC-A-07 | Download interrupted (range request) | Resumable or clean error |
| EC-A-08 | MSG / TIFF (§14.1) | Accept or reject per whitelist |

### 3.3 سير العمل (Camunda) | Workflow

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-W-01 | Complete task twice (double submit) | Idempotent or second call fails safely |
| EC-W-02 | Process definition updated mid-flight | Running instances follow deployed version |
| EC-W-03 | External task timeout | Retry or incident; operator visibility |
| EC-W-04 | Delegate after task already completed | 409 / business error |
| EC-W-05 | Cancel correspondence mid-process | BPMN cancel; compensating actions |
| EC-W-06 | Parallel branch — one assignee on leave | Reassignment / escalation per BR |
| EC-W-07 | Message correlation duplicate | Single subscription match |

### 3.4 التزامن والاتساق | Concurrency & consistency

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-C-01 | Two users edit same correspondence | Optimistic lock or last-write policy documented |
| EC-C-02 | Admin disables user mid-active session | Next API call 401/403 |
| EC-C-03 | Role permissions changed mid-session | Next request reflects new matrix (or forced re-login) |
| EC-C-04 | Bulk import + UI edit same record | Serialization; no orphan rows |

### 3.5 التعريب وإتاحة الوصول | Internationalization & accessibility

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-I-01 | Mixed Arabic / English / numerals in subject | Correct shaping and storage |
| EC-I-02 | RTL form with LTR email / URL inside | Bidirectional layout correct |
| EC-I-03 | PDF export with Arabic | Embedded fonts; searchable text |
| EC-I-04 | Screen reader on workflow timeline | Meaningful labels (WCAG 2.1 AA) |

### 3.6 أمن الحالات الحدية | Security edge cases

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-S-01 | JWT `sub` valid UUID but user deleted | 401/403 |
| EC-S-02 | JWT algorithm `none` / wrong alg | Reject |
| EC-S-03 | Long JWT / cookie size | Failure handled |
| EC-S-04 | CSRF: state-changing request from foreign origin | Blocked per CORS + token design |
| EC-S-05 | Path traversal in attachment storage key | Reject |
| EC-S-06 | IDOR: guess another user’s attachment UUID | 403 |

### 3.7 التكامل الخارجي (SRS §12) | Integrations

| ID | Integration | Edge scenario | Expected behavior |
|----|-------------|---------------|-------------------|
| EC-X-01 | AD/LDAP | LDAP slow (>5s) | Timeout; graceful degradation |
| EC-X-02 | PKI | HSM unavailable | Queue or fail closed for signing |
| EC-X-03 | SMS | Provider rate limit | Backoff; user message |
| EC-X-04 | GSB / HR REST | Partial JSON | Schema validation error |
| EC-X-05 | Email | SMTP greylisting | Retry policy |

### 3.8 القواعد العملية (SRS §10) | Business rules — edge tests

| ID | Rule | Edge test |
|----|------|-----------|
| EC-B-01 | BR-010–015 | Each rule: minimal valid + one violating case |
| EC-B-02 | BR-020–022 | Confidentiality vs role matrix exhaustive for sample set |

### 3.9 دورية المراجعة | Review cadence

- **Sprint:** إضافة سيناريوهات للميزات الجديدة.  
- **Release:** مجموعة انحدار لحالات الحد من الأولوية P1.  
- **Post-incident:** توثيق السيناريوهات التي لم تُغطَّ سابقاً.

---

<span id="sec-4"></span>

## 4. سيناريوهات الفشل | Failure scenarios

*يشمل: الشبكة والبنية التحتائية، البيانات غير الصالحة، الصلاحيات والمصادقة.*

### 4.1 فشل الشبكة والبنية التحتائية | Network & infrastructure

#### 4.1.1 Client ↔ API

| ID | Symptom | Expected |
|----|---------|----------|
| FS-N-01 | User offline or DNS failure | UI shows offline/retry; no silent data loss on forms (draft if supported) |
| FS-N-02 | TLS handshake failure (cert expired, wrong hostname) | Browser error; monitoring alert on cert expiry |
| FS-N-03 | Intermittent packet loss | Idempotent retries for GET; duplicate detection for POST |
| FS-N-04 | HTTP 502/504 from reverse proxy | User-friendly message; correlation id in logs |
| FS-N-05 | Request timeout | UI timeout; reconcile via polling or idempotency key |
| FS-N-06 | Large upload timeout mid-flight | Resumable or clear restart; no corrupt partial attachment record |

#### 4.1.2 API ↔ PostgreSQL

| ID | Symptom | Expected |
|----|---------|----------|
| FS-N-10 | DB connection pool exhausted | 503 with retry-after; alert |
| FS-N-11 | DB failover (HA) | Brief errors; RPO ≤ 1h (NFR-203); reconnect |
| FS-N-12 | Long Flyway migration on deploy | Maintenance window or blue/green strategy |
| FS-N-13 | Deadlock on hot row | Service-layer retry; metrics |

#### 4.1.3 API ↔ Camunda

| ID | Symptom | Expected |
|----|---------|----------|
| FS-N-20 | Engine exception on transition | Rollback; consistent state; Cockpit incident |
| FS-N-21 | Job executor stuck | Monitoring; runbook |

#### 4.1.4 API ↔ File storage (local / S3 / MinIO)

| ID | Symptom | Expected |
|----|---------|----------|
| FS-N-30 | Disk full or S3 503 | Clear error; no DB row without blob |
| FS-N-31 | Antivirus async failure | Quarantine or block download until resolved |

### 4.2 بيانات غير صالحة أو خبيثة | Invalid / malicious data

#### 4.2.1 Request validation

| ID | Input | Expected |
|----|-------|----------|
| FS-D-01 | Missing required field on `POST /correspondence` | 400 + field errors |
| FS-D-02 | Wrong JSON type for coded field | 400; no 500 |
| FS-D-03 | Oversized payload | 413 |
| FS-D-04 | Invalid FK | 400/404; safe message |
| FS-D-05 | Invalid ISO date | 400 |
| FS-D-06 | multipart missing boundary | 400 |
| FS-D-07 | Content vs extension mismatch | Reject if policy mandates |

#### 4.2.2 Business rule violations

| ID | Input | Expected |
|----|-------|----------|
| FS-D-10 | Action invalid for workflow state | 409 / 422 `INVALID_STATE` |
| FS-D-11 | Approve without mandatory comment | 400 |
| FS-D-12 | Cancel finalized correspondence | Denied per BR-014 / policy |

#### 4.2.3 Injection & fuzzing

| ID | Input | Expected |
|----|-------|----------|
| FS-D-20 | XSS in fields shown in UI | Escaped; CSP in prod |
| FS-D-21 | SQL injection patterns in search | Parameterized queries; no leakage |
| FS-D-22 | SSRF via URL field (if any) | Allowlist or block private IPs |

### 4.3 الصلاحيات والمصادقة | Permissions & authentication

#### 4.3.1 Unauthenticated

| ID | Call | Expected |
|----|------|----------|
| FS-P-01 | Protected endpoint without `Authorization` | 401 |
| FS-P-02 | Malformed `Bearer` | 401 |
| FS-P-03 | Expired JWT | 401; refresh if applicable |

#### 4.3.2 Authenticated but not authorized

| ID | User action | Expected |
|----|-------------|----------|
| FS-P-10 | Clerk → `DELETE /users/{id}` | 403 |
| FS-P-11 | Handler → admin `PUT` | 403 |
| FS-P-12 | Access out-of-scope correspondence | 403 or empty list (document policy) |
| FS-P-13 | Download foreign attachment UUID | 403 |

#### 4.3.3 Privilege abuse

| ID | Tamper | Expected |
|----|--------|----------|
| FS-P-20 | Edited JWT (invalid signature) | 401 |
| FS-P-21 | Client-only role elevation | Server ignores; DB is source of truth |
| FS-P-22 | Old JWT after password reset | Invalid if versioning / revocation |

### 4.4 المراقبة عند الفشل | Observability & supportability

| Requirement | Detail |
|---------------|--------|
| Correlation ID | `X-Request-Id` through API → Camunda → logs |
| Structured logging | No PII/passwords; JWT never logged in full |
| Metrics | Error rate by endpoint, DB latency, Camunda incidents |
| Runbooks | FS-N-11, FS-N-20, FS-N-30 linked to on-call |

### 4.5 مصفوفة تتبع عيّنة | Traceability matrix (sample)

| Failure ID | Automated test | Load test | Security test |
|------------|----------------|-----------|-----------------|
| FS-N-04 | E2E mock 502 | ✓ | — |
| FS-D-01 | API contract test | — | — |
| FS-P-12 | RBAC integration | — | ✓ |

---

<span id="sec-5"></span>

## 5. سجل مخاطر الأداء | Performance risks register

### 5.1 خط الأساس من SRS | SRS performance baseline

| ID | Metric | Target |
|----|--------|--------|
| NFR-101 | Transaction response time | ≤ 3 seconds |
| NFR-102 | List query (10,000 records) | ≤ 5 seconds |
| NFR-103 | Concurrent users | 5,000 |
| NFR-104 | Correspondence volume | 50,000+ |
| NFR-105 | Retention | 10 years |
| NFR-201 | Availability | 99.9% |

### 5.2 سجل المخاطر | Risk register

| Risk ID | Area | Risk description | L | I | Mitigation | Owner | SRS |
|---------|------|------------------|---|---|------------|-------|-----|
| PERF-R01 | Database | Unindexed filters → full scans at scale | M | H | Composite indexes; EXPLAIN; read replicas | DBA / Dev | NFR-102 |
| PERF-R02 | Database | N+1 on detail view | H | M | Joins / DTOs / batch APIs | Dev | NFR-101 |
| PERF-R03 | Camunda | Job executor saturation | M | H | Pool tuning; scale-out story | Platform | NFR-103 |
| PERF-R04 | Camunda | Heavy parallel BPMN | L | M | Simplify; async | BA / Dev | FR-203 |
| PERF-R05 | File I/O | Slow NFS for attachments | M | H | Object storage; CDN | Infra | FR-102 |
| PERF-R06 | File I/O | Sync AV blocks upload | M | M | Async scan + status | Security | §14 |
| PERF-R07 | Search | LIKE-only full-text at scale | H | H | OpenSearch / ES | Arch | FR-502 |
| PERF-R08 | Reports | On-the-fly aggregates | M | H | Materialized views / Redis TTL | Dev | FR-601 |
| PERF-R09 | Export | Full dataset in memory | M | H | Streaming; async job | Dev | FR-705 |
| PERF-R10 | JWT | DB lookup every request | M | M | Short cache + invalidation | Dev | — |
| PERF-R11 | API | Chatty Angular | M | M | BFF; `forkJoin` | FE | NFR-101 |
| PERF-R12 | Network | Oversized JWT/cookies | L | M | Slim claims | Sec | — |
| PERF-R13 | Frontend | 10k rows unvirtualized | H | M | Paging + virtual scroll | FE | NFR-102 |
| PERF-R14 | Growth | 10-year retention volume | M | H | Partitioning; archival | DBA | NFR-105 |
| PERF-R15 | Integrations | Sync LDAP/SMS blocking | M | H | Timeouts; circuit breaker; queue | Dev | §12 |
| PERF-R16 | Backup | RPO pressure under write load | M | H | WAL / PITR; IOPS | Infra | NFR-203 |

*L = Likelihood · I = Impact (H/M/L)*

### 5.3 ملف اختبار الحمل المقترح | Load testing profile

| Scenario | Mix % | Notes |
|----------|-------|-------|
| Login + dashboard | 25 | Warm JWT cache |
| List correspondence (filtered) | 35 | NFR-102 |
| Open detail + attachments | 15 | Download separate |
| Workflow action | 15 | Camunda path |
| Report export | 10 | PERF-R09 |

**Ramp:** 0 → 5000 VUs over 30–60 min; soak 2–4 h.

### 5.4 مؤشرات المراقبة في الإنتاج | Production monitoring KPIs

| KPI | Alert threshold (example) |
|-----|---------------------------|
| API P95 latency | > 2.5s warn · > 3.5s crit vs NFR-101 |
| Error rate 5xx | > 0.1% / 5 min |
| DB CPU | > 70% sustained |
| Camunda incidents/hour | > 0 (business hours) |
| Disk usage attachments | > 80% |

### 5.5 المراجعة | Review

| Frequency | Action |
|-----------|--------|
| Each major release | Re-run load test; update scores |
| Architecture change | Add risks for new integrations |

---

<span id="sec-6"></span>

## 6. قائمة جاهزية الإنتاج (Go/No-Go) | Production readiness checklist

**وسم الحالة | Legend:** ☐ Not started · ◐ In progress · ☑ Done · N/A

### 6.1 الوظيفي وUAT | Functional & UAT

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.1.1 | All P1 UAT cases passed | ☐ | Run log | QA |
| 6.1.2 | P2 defects triaged (fix / waive / defer + CAB) | ☐ | Tracker | Product |
| 6.1.3 | BR-001–022 verified in staging | ☐ | Results | Business |
| 6.1.4 | BPMN versions tagged in release | ☐ | Git + Camunda record | Dev |

### 6.2 الأمن والامتثال (SRS §11) | Security & compliance

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.2.1 | TLS 1.2+ (target 1.3) | ☐ | Scan | Security |
| 6.2.2 | Secrets in vault — not in git/images | ☐ | CI secret scan | Security |
| 6.2.3 | Strong `AC_JWT_SECRET` + rotation plan | ☐ | Runbook | Security |
| 6.2.4 | RBAC matrix = `SecurityConfig` + DB | ☐ | Matrix | Security |
| 6.2.5 | OWASP ASVS spot check | ☐ | Report | Security |
| 6.2.6 | MFA / AD / SSO in scope tested | ☐ | Log | IAM |
| 6.2.7 | Attachment download — no IDOR | ☐ | Test | Security |
| 6.2.8 | CORS restricted (prod) | ☐ | Config | Dev |
| 6.2.9 | CSP, HSTS, X-Frame-Options | ☐ | Header scan | Dev |

### 6.3 البيانات والاستمرارية | Data & persistence

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.3.1 | Flyway clean on empty + prod-like DB | ☐ | CI + staging | DBA |
| 6.3.2 | Backup; RPO ≤ 1h (NFR-203) | ☐ | Job logs | Infra |
| 6.3.3 | Restore drill (quarterly) | ☐ | Report | Infra |
| 6.3.4 | RTO ≤ 4h (NFR-202) tested | ☐ | DR runbook | Infra |
| 6.3.5 | Retention 10y (NFR-105) — archival policy | ☐ | Policy | Legal / IM |

### 6.4 الأداء والسعة (SRS §5.1) | Performance & scale

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.4.1 | Load test to target concurrency | ☐ | k6/Gatling | QA |
| 6.4.2 | NFR-101 / NFR-102 under load | ☐ | Grafana | QA |
| 6.4.3 | DB indexes for top queries | ☐ | EXPLAIN | DBA |
| 6.4.4 | Pool sizing for pods × concurrency | ☐ | Sheet | Dev |

### 6.5 المراقبة والتشغيل | Observability & operations

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.5.1 | JSON logs + correlation id | ☐ | Sample | Dev |
| 6.5.2 | RED/USE metrics | ☐ | Dashboard | SRE |
| 6.5.3 | Alerts: errors, latency, disk, queues | ☐ | Ops | SRE |
| 6.5.4 | Runbooks: deploy, rollback, DB, Camunda | ☐ | Wiki | SRE |
| 6.5.5 | Health checks → load balancer | ☐ | Config | Infra |

### 6.6 التهيئة والبيئات | Configuration

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.6.1 | Prod config — no dev defaults | ☐ | Diff | Dev |
| 6.6.2 | PostgreSQL HA matches RTO/RPO | ☐ | Diagram | Infra |
| 6.6.3 | Camunda admin password not default | ☐ | Vault | Infra |
| 6.6.4 | Email/SMS prod creds + rate limits | ☐ | Vendor | Ops |
| 6.6.5 | File storage lifecycle | ☐ | IaC | Infra |

### 6.7 الواجهة الأمامية (Angular) | Frontend

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.7.1 | Prod build; source maps policy | ☐ | Pipeline | Dev |
| 6.7.2 | API base URL correct | ☐ | Config | Dev |
| 6.7.3 | RTL + i18n smoke | ☐ | Screenshots | QA |
| 6.7.4 | WCAG 2.1 AA criticals closed | ☐ | Audit | UX |

### 6.8 قانوني وخصوصية وتدقيق | Legal & privacy

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.8.1 | Audit tamper-evidence / retention | ☐ | Design | Compliance |
| 6.8.2 | PII documented | ☐ | DPIA | DPO |
| 6.8.3 | System issue reporting E2E | ☐ | Test | Support |

### 6.9 الإصدار | Release mechanics

| # | Item | Status | Evidence | Sign-off |
|---|------|--------|----------|----------|
| 6.9.1 | Rollback tested | ☐ | Drill | SRE |
| 6.9.2 | Feature flags (if used) | ☐ | List | Dev |
| 6.9.3 | Maintenance window communicated | ☐ | Comms | PMO |

### 6.10 قرار Go/No-Go الإداري | Extended go/no-go

| Role | Name | Go / No-Go | Date | Notes |
|------|------|------------|------|-------|
| Product Owner | | | | |
| Engineering Lead | | | | |
| QA Lead | | | | |
| Security | | | | |
| Infrastructure | | | | |

**No-Go** if any P1 security, data integrity, or unmitigated P1 defect remains.

---

<span id="sec-7"></span>

## 7. التوقيع والاعتماد الرسمي | Official sign-off

### 7.1 الاعتمادات الإلزامية للتسليم | Required approvals for this package

| # | الدور | Role | الاسم Name | التوقيع Signature | التاريخ Date |
|---|--------|------|-------------|---------------------|--------------|
| 1 | **ممثل الأعمال** | **Business representative** | | | |
| 2 | **ضمان الجودة (QA)** | **Quality Assurance** | | | |
| 3 | **القيادة التقنية** | **Technical Lead** | | | |

### 7.2 إقرار بالاستلام | Acknowledgement

نقر بموجب هذا بأن حزمة ضمان الجودة الموحّدة قد استُلمت للمراجعة، وأن المحتوى يعكس المتطلبات الواردة في وثيقة مواصفات المتطلبات البرمجية المعتمدة للنظام.  
We acknowledge receipt of the Master QA Delivery Package for review and confirm that the content reflects the approved SRS for the Administrative Communications System.

### 7.3 سجل المراجعات | Revision history

| Version | Date | Author / Owner | Summary of changes |
|---------|------|----------------|---------------------|
| 1.0 | 5 April 2026 | QA / Architecture | Initial consolidated release |

---

*نهاية الوثيقة — End of document*
