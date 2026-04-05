# k6 — API load tests

## Prerequisites

- [k6](https://k6.io/docs/get-started/installation/) installed (`k6 version`).
- Running API + PostgreSQL with seeded **`admin`** user (same as `tests/api-e2e`).

## Run

```powershell
cd tests\load\k6
$env:BASE_URL = "http://localhost:8080/api/v1"
$env:API_USER = "admin"
$env:API_PASS = "admin"
k6 run ac-api-load.js
```

Linux/macOS:

```bash
export BASE_URL=http://localhost:8080/api/v1
k6 run ac-api-load.js
```

## What the script exercises

| Group | Endpoints |
|-------|-----------|
| `login_warmup` | `POST /auth/login` |
| `correspondence_list` | `GET /correspondence?page=0&size=20` |
| `correspondence_detail` | `GET /correspondence/{id}` (first page item) |
| `workflow_create_and_approve` | `POST /correspondence` + `POST .../actions` (APPROVE) |
| `attachment_upload` | `POST /attachments/upload` (multipart) |
| `reports_excel_export` | `GET /reports/export/excel` |

## Interpreting results

- **http_req_duration.p95** — compare to **NFR-101** (≤ 3s) from `docs/enterprise-qa/Performance_Risks_Register.md`. k6 prints `http_req_duration` percentiles in the end-of-test summary.
- **http_reqs** / wall-clock — rough **throughput** (requests per second).
- **http_req_failed** — failure rate; threshold defaults to **&lt; 5%** (tunable for greenfield DB).
- **CPU / memory** — k6 does not measure server-side resources; use `docker stats`, host metrics, or APM while the test runs.

**NFR-102** (10k-row list ≤ 5s) is **not** fully represented: the script uses `size=20`. Add a dedicated scenario with a large page size or seeded volume when data exists.

## Thresholds

Edit `ac-api-load.js` `options.thresholds` to tighten or relax `p(95)` and `http_req_failed` after baseline capture.
