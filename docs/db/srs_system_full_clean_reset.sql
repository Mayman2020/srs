-- =============================================================================
-- DEVELOPMENT / DISASTER RECOVERY — full clean reset of SRS (schema srs_system)
-- =============================================================================
-- VERIFY TARGET (run first — confirm this matches spring.datasource.url in application.yml):
--   SELECT current_database() AS db, current_user AS role, current_setting('search_path') AS path;
-- Expected db: same catalog as your JDBC URL (often "postgres"). If this is wrong, the reset
-- did not hit the database the app uses.
-- =============================================================================
-- Use when Flyway history is corrupted, tables were created manually, Hibernate
-- validation fails, or flyway_schema_history shows a version > 1 while the repo
-- only contains V1__srs_system_full_baseline.sql (Flyway will skip V1 and leave
-- tables missing).
--
-- Prerequisites:
--   1. Stop Spring Boot (and any process using this database).
--   2. Connect to the SAME database the app uses (catalog), e.g. postgres on localhost.
--
-- After this script:
--   Start the app: mvnw spring-boot:run  (profile local)
--   Flyway will apply V1 (app baseline) and V2 (Camunda ACT_* DDL), and recreate
--   srs_system.flyway_schema_history. Camunda runs with schema-update disabled.
-- =============================================================================

-- Removes all SRS objects + Flyway history stored inside this schema.
DROP SCHEMA IF EXISTS srs_system CASCADE;

-- Empty schema before the app starts (Flyway also uses create-schemas: true).
CREATE SCHEMA srs_system;

-- Remove ANY legacy Flyway history that lived in public (wrong default-schema).
-- Without this, Flyway can still think old versions were applied.
DROP TABLE IF EXISTS public.flyway_schema_history;

-- Optional: if Camunda tables were mistakenly created in public (wrong search_path),
-- identify them with:
--   SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'act_%';
-- Then drop them explicitly, e.g.:
-- DROP TABLE IF EXISTS public.act_ge_property CASCADE;

COMMENT ON SCHEMA srs_system IS 'SRS application schema; DDL from Flyway V1 baseline only.';
