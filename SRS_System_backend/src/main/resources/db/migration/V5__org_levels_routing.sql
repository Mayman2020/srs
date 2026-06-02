-- =============================================================================
-- V5__org_levels_routing.sql
--
-- Introduce Q/L/K/S organizational level model (Saudi Ministry of Defense
-- routing hierarchy) and extend workflow_instance with routing-chain columns so
-- BPMN can walk a precomputed chain instead of guessing at runtime.
--
-- Concept:
--   Q  = Headquarters       (القيادة العامة)
--   L  = Brigade / Sector   (اللواء / القطاع)
--   K  = Battalion / Dept   (الكتيبة / الإدارة)
--   S  = Company / Sub-unit (السرية / الوحدة الفرعية)
--
-- Routing rules (computed in OrgRoutingService, not in DB):
--   - Q -> any: chain = [target]
--   - same K, S->S: chain = [target]
--   - same L, different K: chain = [originator.K, target.K, target]
--   - different L: chain = [originator.K, originator.L, Q, target.L, target.K, target]
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- organizational_unit_level: lookup of military hierarchy levels.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizational_unit_level (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(8)   NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description TEXT,
  rank_order  INTEGER      NOT NULL,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_organizational_unit_level_rank CHECK (rank_order BETWEEN 1 AND 10)
);

COMMENT ON TABLE organizational_unit_level IS
  'Q/L/K/S military org hierarchy. rank_order is small integer; lower = higher authority (Q=1).';
COMMENT ON COLUMN organizational_unit_level.code IS
  'Short stable code (Q, L, K, S). Used as Camunda candidate-group suffix and routing key.';

-- Case-insensitive uniqueness for active rows (application may insert mixed case).
CREATE UNIQUE INDEX IF NOT EXISTS ux_org_unit_level_code_active
  ON organizational_unit_level (UPPER(code)) WHERE deleted_at IS NULL;

-- Seed canonical 4-level hierarchy.
INSERT INTO organizational_unit_level (code, name_ar, name_en, description, rank_order)
SELECT 'Q', 'القيادة العامة', 'Headquarters',
       'Top of the chain; can dispatch to any subordinate level directly.', 1
WHERE NOT EXISTS (
  SELECT 1 FROM organizational_unit_level WHERE UPPER(code) = 'Q' AND deleted_at IS NULL
);

INSERT INTO organizational_unit_level (code, name_ar, name_en, description, rank_order)
SELECT 'L', 'اللواء / القطاع', 'Brigade / Sector',
       'Reports to Q; aggregates multiple K-level battalions / departments.', 2
WHERE NOT EXISTS (
  SELECT 1 FROM organizational_unit_level WHERE UPPER(code) = 'L' AND deleted_at IS NULL
);

INSERT INTO organizational_unit_level (code, name_ar, name_en, description, rank_order)
SELECT 'K', 'الكتيبة / الإدارة', 'Battalion / Department',
       'Reports to L; aggregates multiple S-level companies / sub-units.', 3
WHERE NOT EXISTS (
  SELECT 1 FROM organizational_unit_level WHERE UPPER(code) = 'K' AND deleted_at IS NULL
);

INSERT INTO organizational_unit_level (code, name_ar, name_en, description, rank_order)
SELECT 'S', 'السرية / الوحدة الفرعية', 'Company / Sub-unit',
       'Leaf level; reports to K. Cannot have organizational descendants below.', 4
WHERE NOT EXISTS (
  SELECT 1 FROM organizational_unit_level WHERE UPPER(code) = 'S' AND deleted_at IS NULL
);

-- FK references organizational_unit_level(code) — PostgreSQL requires a UNIQUE or PK
-- on the *same* column(s), not a unique index on an expression (UPPER(code)).
UPDATE organizational_unit_level SET code = UPPER(BTRIM(code)) WHERE code IS NOT NULL;

ALTER TABLE organizational_unit_level
  DROP CONSTRAINT IF EXISTS uq_organizational_unit_level_code;

ALTER TABLE organizational_unit_level
  ADD CONSTRAINT uq_organizational_unit_level_code UNIQUE (code);

-- -----------------------------------------------------------------------------
-- department.level_code: military level of this department node.
--   NULL until populated via admin UI (Phase 5 OrgLevelsAdminComponent).
-- -----------------------------------------------------------------------------
ALTER TABLE department
  ADD COLUMN IF NOT EXISTS level_code VARCHAR(8);

COMMENT ON COLUMN department.level_code IS
  'Q/L/K/S code for this department; FK to organizational_unit_level.code. Required for routing.';

ALTER TABLE department
  DROP CONSTRAINT IF EXISTS fk_department_level_code;

ALTER TABLE department
  ADD CONSTRAINT fk_department_level_code
  FOREIGN KEY (level_code) REFERENCES organizational_unit_level (code)
  DEFERRABLE INITIALLY DEFERRED;

-- Best-effort seed: assign ROOT to Q (top of hierarchy).
UPDATE department SET level_code = 'Q'
  WHERE code = 'ROOT' AND level_code IS NULL AND deleted_at IS NULL;

-- Direct children of ROOT default to L (admin can re-assign).
UPDATE department d SET level_code = 'L'
  WHERE d.level_code IS NULL AND d.deleted_at IS NULL
    AND EXISTS (
      SELECT 1 FROM department p
      WHERE p.id = d.parent_id AND p.code = 'ROOT' AND p.deleted_at IS NULL
    );

-- Grandchildren default to K.
UPDATE department d SET level_code = 'K'
  WHERE d.level_code IS NULL AND d.deleted_at IS NULL
    AND EXISTS (
      SELECT 1 FROM department p
      JOIN department gp ON gp.id = p.parent_id
      WHERE p.id = d.parent_id AND gp.code = 'ROOT' AND p.deleted_at IS NULL
    );

-- Anything deeper defaults to S.
UPDATE department SET level_code = 'S'
  WHERE level_code IS NULL AND deleted_at IS NULL;

-- The FK above is DEFERRABLE INITIALLY DEFERRED; the preceding UPDATEs leave
-- pending trigger events on `department` that PostgreSQL must clear before any
-- DDL (e.g. CREATE INDEX) on the same table. Force constraint validation now.
SET CONSTRAINTS ALL IMMEDIATE;

CREATE INDEX IF NOT EXISTS ix_department_level_code
  ON department (level_code) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- workflow_instance routing extensions: chain snapshot + current pointer + SLA.
-- -----------------------------------------------------------------------------
ALTER TABLE workflow_instance
  ADD COLUMN IF NOT EXISTS routing_chain_json JSONB,
  ADD COLUMN IF NOT EXISTS current_level_code VARCHAR(8),
  ADD COLUMN IF NOT EXISTS current_department_id BIGINT REFERENCES department (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS escalation_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS originator_department_id BIGINT REFERENCES department (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS target_department_id BIGINT REFERENCES department (id) ON DELETE SET NULL;

COMMENT ON COLUMN workflow_instance.routing_chain_json IS
  'JSON array of routing stops: [{departmentId, levelCode, roleCode}]. Audit trail of the precomputed chain.';
COMMENT ON COLUMN workflow_instance.current_level_code IS
  'Q/L/K/S level the workflow is currently sitting at (driven by advanceChainDelegate).';
COMMENT ON COLUMN workflow_instance.escalation_count IS
  'Number of SLA breaches recorded against this instance (bumped by escalation boundary listener).';

ALTER TABLE workflow_instance
  DROP CONSTRAINT IF EXISTS fk_workflow_instance_current_level;

ALTER TABLE workflow_instance
  ADD CONSTRAINT fk_workflow_instance_current_level
  FOREIGN KEY (current_level_code) REFERENCES organizational_unit_level (code)
  DEFERRABLE INITIALLY DEFERRED;

-- Clear any pending deferred trigger events on workflow_instance before DDL.
SET CONSTRAINTS ALL IMMEDIATE;

CREATE INDEX IF NOT EXISTS ix_workflow_instance_current_level
  ON workflow_instance (current_level_code) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_workflow_instance_current_dept
  ON workflow_instance (current_department_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- correspondence routing snapshot columns (read-only; mirror workflow_instance
-- for fast list filtering without a join to active_workflow_instance).
-- -----------------------------------------------------------------------------
ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS target_department_id BIGINT REFERENCES department (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS originator_user_id   UUID   REFERENCES app_user   (id) ON DELETE SET NULL;

COMMENT ON COLUMN correspondence.target_department_id IS
  'Selected final recipient department at create time. Drives Q/L/K/S routing chain resolution.';
COMMENT ON COLUMN correspondence.originator_user_id IS
  'User who originated this correspondence (creator); separate from owner_department_id.';

-- Best-effort backfill from existing audit column.
UPDATE correspondence SET originator_user_id = created_by
  WHERE originator_user_id IS NULL AND created_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_correspondence_target_dept
  ON correspondence (target_department_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_correspondence_originator_user
  ON correspondence (originator_user_id) WHERE deleted_at IS NULL;
