# Administrative Communications — API E2E (real HTTP)

Validated against Spring Boot controllers under **`/api/v1`** (JWT, correspondence, Camunda workflow, attachments, notifications, reports).

## Prerequisites

1. PostgreSQL running; backend started with Flyway migrations applied (seed user **`admin` / `admin`** from `V10__admin_login_user.sql`). If login returns **401** despite Flyway, run **`node fix-dev-credentials.mjs`** once (resets `{noop}admin`, MFA off, ensures `clerk`).
2. Default API base: **`http://localhost:8080/api/v1`** (matches `run-backend.ps1` default port; use **8081** only if you passed `-Port 8081`).

## Notification delete test (second user)

In-app notifications for “correspondence created” go to **other active users in `ownerDepartmentId`**, not the creator. For a **real** list + delete test:

1. Run **`seed-second-user.sql`** on your database (creates **`clerk` / `clerk`** in department `1`).
2. The Node script uses `API_SECOND_USERNAME` / `API_SECOND_PASSWORD` (defaults: `clerk` / `clerk`).

Without the clerk user, **NOTIFICATIONS delete** will **fail** until a second recipient exists.

## Run Node.js script

```powershell
cd tests\api-e2e
npm install
# optional: $env:API_BASE_URL = "http://localhost:8081/api/v1"
npm test
```

Environment variables:

| Variable | Default |
|----------|---------|
| `API_BASE_URL` | `http://localhost:8080/api/v1` |
| `API_USERNAME` | `admin` |
| `API_PASSWORD` | `admin` |
| `API_SECOND_USERNAME` | `clerk` |
| `API_SECOND_PASSWORD` | `clerk` |

Console output: **PASS / FAIL** per step. Excel is written to **`correspondences-export.xlsx`** in this folder on success.

## Import Postman

1. **Import** `postman/Administrative_Communications_API.postman_collection.json`.
2. **Import** `postman/Local-8080.postman_environment.json`.
3. Select environment **Local AC API — 8080**.
4. In **Collection variables** (or environment), set **`adminUsername`**, **`adminPassword`**, **`clerkUsername`**, **`clerkPassword`** if different from defaults.
5. For **Attachments → Upload file**, choose a small file in the **Body → form-data** row `file` (type: File).
6. Run the folder **in order** (Auth → Correspondence → Notifications (clerk) → Attachments → Workflow → Reports), or run the whole collection.

Collection **Tests** tabs persist `token`, `correspondenceId`, `storageKey`, `attachmentId`, `notificationId` into **environment** where noted.

## Workflow action

The API accepts **`APPROVE`**, **`REJECT`**, **`RETURN`**, **`REFER`** (`WorkflowActionRequest`). The collection uses **`APPROVE`** on the first user task. **`REFER`** requires a **comment** per backend validation; you can duplicate the request and set `"action": "REFER", "comment": "API test refer"`.

## Real paths (no placeholders)

- `POST /auth/login`
- `POST /correspondence`, `GET /correspondence/{id}`, `POST /correspondence/{id}/actions`
- `POST /attachments/upload`, `POST /correspondence/{id}/attachments`, `DELETE /attachments/{id}`
- `GET /notifications`, `DELETE /notifications/{id}`
- `GET /reports/export/excel`
