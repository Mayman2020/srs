-- V25: dashboard KPI alignment + optional workflow action targets metadata
SET search_path TO srs_system, public;

-- RETURNED is terminal (V24); exclude from active pipeline KPI bucket.
UPDATE correspondence_status
SET kpi_segment = 'SLA_DONE',
    updated_at = now()
WHERE UPPER(code) = 'RETURNED' AND deleted_at IS NULL;
