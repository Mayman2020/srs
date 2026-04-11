-- =============================================================================
-- DEVELOPMENT ONLY — minimal portable reset (DBeaver, psql, any SQL client)
-- =============================================================================
-- Full runbook: srs_system_full_clean_reset.sql
-- =============================================================================

DROP SCHEMA IF EXISTS srs_system CASCADE;
CREATE SCHEMA srs_system;
DROP TABLE IF EXISTS public.flyway_schema_history;
