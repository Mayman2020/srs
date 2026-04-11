# Database architecture (enterprise baseline)

## Root cause of typical failures

| Symptom | Cause |
|--------|--------|
| Hibernate: missing table `app_user` (or others) | Flyway did not apply the full baseline on **this** database (partial run, wrong JDBC URL / `search_path`, or Flyway validation stopped startup before migrate). |
| Flyway “success” but verification: `srs_system.app_user` missing; logs show `app_user` in **public** | **Flyway’s migration JDBC session** did not use `search_path` → `srs_system` for unqualified `CREATE TABLE` in V1. Hikari `connection-init-sql` alone may not apply to Flyway’s connection. **Fix:** `spring.flyway.init-sqls: SET search_path TO srs_system, public` in `application.yml` (plus `SET search_path` at top of V1). Drop stray `public.app_user` or full reset. |
| `migrationsExecuted: 0` but tables missing; `Current version: 2` | History rows exist without DDL (drift). **Profile `local`:** startup runs an automatic self-heal (same as `srs_system_full_clean_reset.sql`) then reapplies V1/V2. Or reset manually on the **same** DB as the app. |
| Flyway checksum mismatch on version `1` | Database was migrated with an older `V1` script; the repo now ships `V1__srs_system_full_baseline.sql` with a different checksum. |
| Flyway “applied migration not resolved” / version > classpath | History still lists **V2..V30** while the repo contains **only one file** after consolidation. |
| Flyway says “Schema is up to date”, DB version **2**, only **V1** on classpath, `app_user` missing | History claims migrations beyond `V1` (or checksum drift); Flyway **does not re-run** `V1`. Reset with `docs/db/srs_system_full_clean_reset.sql`. |
| Tables only in `public` or “half schema” | JDBC URL missing `?currentSchema=srs_system` or connections not using `SET search_path` (see `application.yml` Hikari `connection-init-sql`). |

**Single source of truth:** all application DDL is in **Flyway** (`V1__srs_system_full_baseline.sql`). Hibernate **`ddl-auto: validate`** only checks entities against the database; it does **not** create or alter tables.

**There is no separate `user_profile` table** — profile fields live on `app_user` (and related RBAC tables `role`, `permission`, `user_role`, `role_permission`).

---

## What the codebase enforces

- **`application.yml`:** `spring.jpa.hibernate.ddl-auto: validate`, `hibernate.default_schema: srs_system`, datasource URL with `currentSchema=srs_system`, Flyway `schemas: srs_system`.
- **`application-local.yml`:** `spring.jpa.hibernate.ddl-auto: validate`. Do **not** enable `ignore-missing-migrations` — it can mask a corrupt history while Flyway skips `V1`.
- **`LocalFlywayMigrationStrategyConfig` (profile `local`):** `flyway.repair()` then `flyway.migrate()` so checksum changes on `V1` are reconciled.
- **`FlywayPostMigrateSchemaVerificationConfig`:** after migrate, fails fast if `srs_system.app_user` is missing (points to reset SQL below).
- **`DatabaseAutoCreation`:** ensures the **database** catalogue `postgres` exists (not the app schema — Flyway creates `srs_system`).

---

## Developer workflow (zero manual DDL)

1. PostgreSQL reachable (local install or Docker) with credentials matching `application-local` / secrets.
2. From `SRS_System_backend`:

   ```bash
   .\mvnw.cmd spring-boot:run
   ```

   or `.\run-backend.ps1` (profile `local`).

3. First run applies **`V1__srs_system_full_baseline.sql`** (if the schema is empty or Flyway history allows). Hibernate validation then succeeds.

Use the **same** JDBC URL pattern in DBeaver as the app: `jdbc:postgresql://host:port/postgres?currentSchema=srs_system`.

---

## Safe DB reset (development only)

Use when the database is **irrecoverably inconsistent** (mixed manual changes, aborted migrations, or impossible Flyway history).

**Option A — clean slate (recommended for dev)**

Run `docs/db/srs_system_full_clean_reset.sql` (or the shorter `docs/db/dev-full-reset.sql`) in psql or DBeaver against the same database the app uses. That drops and recreates the empty `srs_system` schema.

Restart the app; Flyway applies `V1__srs_system_full_baseline.sql` and rebuilds `flyway_schema_history`.

**Option B — new Docker volume**

```bash
docker compose down -v
docker compose up -d postgres
```

Then run the backend again.

**Do not** run `DROP SCHEMA` on production without backup and a controlled migration plan.

---

## Emergency: Hibernate validate while debugging

Only for local troubleshooting, you can temporarily disable validation (profile **`dbrepair`**):

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,dbrepair
```

Or a one-off argument override:

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none
```

Fix the schema with Flyway or reset the DB, then return to **`validate`**.

---

## Camunda

All engine tables (**`ACT_*`**) are created by **Flyway** (`V2__camunda_schema.sql`, official scripts from `camunda-engine` 7.22.0). **`camunda.bpm.database.schema-update` is `false`** — the engine must not alter schema at runtime.

Business bridge tables **`workflow_instance`** and **`workflow_history`** remain in **`V1__srs_system_full_baseline.sql`**; application code should use domain services and **`WorkflowService`** for engine operations instead of touching `ACT_*` directly.

BPMN process keys are defined in `CorrespondenceProcessDefinitionKeys` and match the `*.bpmn` files under `src/main/resources/processes/`.

---

## Summary checklist before a demo

- [ ] Flyway migrations present: `V1__srs_system_full_baseline.sql`, `V2__camunda_schema.sql`
- [ ] `srs_system.flyway_schema_history` shows successful apply of versions `1` and `2` (after repair/migrate)
- [ ] Tables `srs_system.app_user` and `srs_system.act_ge_property` exist
- [ ] App starts with `local` (or target profile) without Hibernate schema errors
