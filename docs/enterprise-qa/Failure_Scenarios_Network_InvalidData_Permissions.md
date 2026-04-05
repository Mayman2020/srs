# Failure Scenarios  
## Network, Invalid Data, and Permissions

| Document control | |
|------------------|---|
| **Source SRS** | `SRS_نظام_الاتصالات_الادارية.docx` |
| **Scope** | Negative testing, resilience, and security failure modes |
| **Version** | 1.0 |

---

## 1. Network & infrastructure failures

### 1.1 Client ↔ API

| FS-N-01 | **Symptom**: User offline or DNS failure. | **Expected**: UI shows offline/retry; no silent data loss on forms (draft save if FR supports). |
| FS-N-02 | **Symptom**: TLS handshake failure (cert expired, wrong hostname). | **Expected**: Browser error; monitoring alert on server cert expiry. |
| FS-N-03 | **Symptom**: Intermittent packet loss (flaky Wi‑Fi). | **Expected**: Idempotent retries for GET; caution on POST (duplicate detection). |
| FS-N-04 | **Symptom**: HTTP 502/504 from reverse proxy (backend down). | **Expected**: User-friendly maintenance message; correlation id in logs. |
| FS-N-05 | **Symptom**: Request timeout (client > server read timeout). | **Expected**: UI timeout message; server may still complete — reconcile via polling or idempotency key. |
| FS-N-06 | **Symptom**: Large upload slow link — timeout mid-upload. | **Expected**: Resumable upload or clear restart; no partial corrupt attachment record. |

### 1.2 API ↔ PostgreSQL

| FS-N-10 | **Symptom**: DB connection pool exhausted. | **Expected**: 503 with retry-after; autoscale or pool tuning; alert. |
| FS-N-11 | **Symptom**: DB failover (Patroni / cloud HA). | **Expected**: Brief errors; RPO ≤ 1h per NFR-203; app reconnect. |
| FS-N-12 | **Symptom**: Long-running migration (Flyway) on deploy. | **Expected**: Deploy strategy (maintenance window or blue/green). |
| FS-N-13 | **Symptom**: Deadlock on hot row. | **Expected**: Retry at service layer; low deadlock rate in metrics. |

### 1.3 API ↔ Camunda

| FS-N-20 | **Symptom**: Camunda engine exception on transition. | **Expected**: Transaction rollback; correspondence state consistent; incident visible in Cockpit. |
| FS-N-21 | **Symptom**: Job executor stuck. | **Expected**: Monitoring; manual intervention runbook. |

### 1.4 API ↔ File storage (local / S3 / MinIO)

| FS-N-30 | **Symptom**: Disk full or S3 503. | **Expected**: Upload fails with clear error; no DB row pointing to missing blob. |
| FS-N-31 | **Symptom**: Antivirus scan async failure. | **Expected**: Quarantine or block download until resolved. |

---

## 2. Invalid, malicious, or inconsistent data

### 2.1 Request validation

| FS-D-01 | **Input**: Missing required field on `POST /correspondence`. | **Expected**: 400 + field-level errors (RFC 7807 problem+json if used). |
| FS-D-02 | **Input**: Wrong JSON type (`"priority": "high"` vs code id). | **Expected**: 400; no 500. |
| FS-D-03 | **Input**: Oversized payload (> server limit). | **Expected**: 413; client message. |
| FS-D-04 | **Input**: Invalid FK (`classification_id` not in DB). | **Expected**: 400/404 with safe message. |
| FS-D-05 | **Input**: Date string invalid ISO format. | **Expected**: 400. |
| FS-D-06 | **Input**: multipart missing `Content-Type` boundary. | **Expected**: 400. |
| FS-D-07 | **Input**: File content does not match extension (magic byte check). | **Expected**: Reject if policy mandates. |

### 2.2 Business rule violations

| FS-D-10 | **Input**: Action not allowed in current workflow state. | **Expected**: 409 Conflict or 422 with code `INVALID_STATE`. |
| FS-D-11 | **Input**: Approve without mandatory comment (if BR requires). | **Expected**: 400. |
| FS-D-12 | **Input**: Cancel finalized correspondence. | **Expected**: Denied per BR-014 / policy. |

### 2.3 Injection & fuzzing

| FS-D-20 | **Input**: XSS strings in text fields displayed in UI. | **Expected**: Escaped; CSP headers in prod. |
| FS-D-21 | **Input**: SQL injection patterns in search. | **Expected**: Parameterized queries; no error leakage. |
| FS-D-22 | **Input**: SSRF via URL field (if any). | **Expected**: Allowlist or block private IPs. |

---

## 3. Permissions & authentication failures

### 3.1 Unauthenticated access

| FS-P-01 | **Call**: Any protected endpoint without `Authorization`. | **Expected**: 401. |
| FS-P-02 | **Call**: Malformed `Bearer` header. | **Expected**: 401. |
| FS-P-03 | **Call**: Expired JWT. | **Expected**: 401; refresh flow if applicable. |

### 3.2 Authenticated but not authorized

| FS-P-10 | **User**: Clerk tries `DELETE /users/{id}`. | **Expected**: 403. |
| FS-P-11 | **User**: Handler tries admin permission `PUT`. | **Expected**: 403. |
| FS-P-12 | **User**: Access correspondence outside visibility (department / classification). | **Expected**: 403 or empty list (document which). |
| FS-P-13 | **User**: Download attachment UUID from another case without ACL. | **Expected**: 403. |

### 3.3 Privilege abuse attempts

| FS-P-20 | **Tamper**: JWT claims edited (signature invalid). | **Expected**: 401. |
| FS-P-21 | **Tamper**: User elevates `role` in client-only state (if any). | **Expected**: Server ignores; source of truth is DB. |
| FS-P-22 | **Replay**: Old JWT after password reset. | **Expected**: Invalid if token versioning / revocation list. |

---

## 4. Observability & supportability (when failures occur)

| Requirement | Detail |
|-------------|--------|
| Correlation ID | Propagate `X-Request-Id` through API → Camunda → logs. |
| Structured logging | No PII/passwords; JWT never logged in full. |
| Metrics | Error rate by endpoint, DB latency, Camunda incidents. |
| Runbooks | FS-N-11, FS-N-20, FS-N-30 linked to on-call steps. |

---

## 5. Traceability matrix (sample)

| Failure ID | Automated test | Load test | Security test |
|------------|----------------|-----------|-----------------|
| FS-N-04 | E2E mock 502 | ✓ | — |
| FS-D-01 | API contract test | — | — |
| FS-P-12 | RBAC integration | — | ✓ |
