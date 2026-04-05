# Edge Case Scenarios  
## Government Administrative Communications System

| Document control | |
|------------------|---|
| **Source SRS** | `SRS_نظام_الاتصالات_الادارية.docx` |
| **Purpose** | Structured edge-case catalog for QA, dev, and design reviews |
| **Version** | 1.0 |

---

## 1. Data boundary & format

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

---

## 2. Attachments

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

---

## 3. Workflow (Camunda)

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-W-01 | Complete task twice (double submit) | Idempotent or second call fails safely |
| EC-W-02 | Process definition updated mid-flight | Running instances follow deployed version |
| EC-W-03 | External task timeout | Retry or incident; operator visibility |
| EC-W-04 | Delegate after task already completed | 409 / business error |
| EC-W-05 | Cancel correspondence mid-process | BPMN cancel; compensating actions |
| EC-W-06 | Parallel branch — one assignee on leave | Reassignment / escalation per BR |
| EC-W-07 | Message correlation duplicate | Single subscription match |

---

## 4. Concurrency & consistency

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-C-01 | Two users edit same correspondence | Optimistic lock or last-write policy documented |
| EC-C-02 | Admin disables user mid-active session | Next API call 401/403 |
| EC-C-03 | Role permissions changed mid-session | Next request reflects new matrix (or forced re-login) |
| EC-C-04 | Bulk import + UI edit same record | Serialization; no orphan rows |

---

## 5. Internationalization & accessibility

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-I-01 | Mixed Arabic / English / numerals in subject | Correct shaping and storage |
| EC-I-02 | RTL form with LTR email / URL inside | Bidirectional layout correct |
| EC-I-03 | PDF export with Arabic | Embedded fonts; searchable text |
| EC-I-04 | Screen reader on workflow timeline | Meaningful labels (WCAG 2.1 AA) |

---

## 6. Security edge cases

| ID | Scenario | Expected behavior |
|----|----------|-------------------|
| EC-S-01 | JWT `sub` valid UUID but user deleted | 401/403 |
| EC-S-02 | JWT algorithm `none` / wrong alg | Reject |
| EC-S-03 | Long JWT / cookie size | Failure handled |
| EC-S-04 | CSRF: state-changing request from foreign origin | Blocked per CORS + token design |
| EC-S-05 | Path traversal in attachment storage key | Reject |
| EC-S-06 | IDOR: guess another user’s attachment UUID | 403 |

---

## 7. Integrations (SRS §12)

| ID | Integration | Edge scenario | Expected behavior |
|----|-------------|---------------|-------------------|
| EC-X-01 | AD/LDAP | LDAP slow (>5s) | Timeout; graceful degradation |
| EC-X-02 | PKI | HSM unavailable | Queue or fail closed for signing |
| EC-X-03 | SMS | Provider rate limit | Backoff; user message |
| EC-X-04 | GSB / HR REST | Partial JSON | Schema validation error |
| EC-X-05 | Email | SMTP greylisting | Retry policy |

---

## 8. Business rules (SRS §10)

| ID | Rule | Edge test |
|----|------|-----------|
| EC-B-01 | BR-010–015 | Each rule: minimal valid + one violating case |
| EC-B-02 | BR-020–022 | Confidentiality vs role matrix exhaustive for sample set |

---

## Review cadence

- **Sprint**: New features add rows under relevant section.  
- **Release**: Full regression subset of P1 edge cases.  
- **Post-incident**: Add scenario that escaped testing.
