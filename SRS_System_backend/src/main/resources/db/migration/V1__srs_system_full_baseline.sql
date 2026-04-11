-- =============================================================================
-- SRS Flyway baseline V1 (single file — enterprise consolidated DDL for schema srs_system).
-- Merged former V1..V30 in order. Extensions remain in schema `public`; all tables/constraints in `srs_system`.
--
-- New database: run the app or Flyway with `spring.flyway.schemas: srs_system` (see application.yml).
--
-- Existing DBs that already applied old multi-file migrations: do not mix. Backup, then either
--   `DROP SCHEMA srs_system CASCADE` and re-run, or use a fresh database.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS srs_system;

SET search_path TO srs_system, public;

-- ---------- source: V1__init_schema.sql ----------

-- =============================================================================
-- V1__init_schema.sql
-- Extensions, shared functions, and global conventions for
-- Government Administrative Communications System (PostgreSQL + Flyway)
-- =============================================================================

-- UUID generation (correspondence.id, app_user.id, etc.)
-- Flyway runs with default schema srs_system; extensions must land in public so
-- migrations that reference public.citext (V3+) resolve consistently.
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

-- Optional: case-insensitive reference lookups (reference_number, username)
CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;

-- -----------------------------------------------------------------------------
-- updated_at maintenance (application may still set explicitly)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ac_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

COMMENT ON FUNCTION ac_set_updated_at() IS 'Sets NEW.updated_at to transaction timestamp before row update.';

-- ---------- source: V2__lookup_tables.sql ----------

-- =============================================================================
-- V2__lookup_tables.sql
-- All formerly-ENUM concepts and other enumerated master data as lookup tables.
-- No PostgreSQL ENUM types — codes are stable application keys (VARCHAR).
-- Audit columns: FK to app_user added in V6 (app_user does not exist yet).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- correspondence_type (وارد / صادر / داخلية / خارجية / تعميم / قرار …)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_type (
  id                 BIGSERIAL PRIMARY KEY,
  code               VARCHAR(64)  NOT NULL,
  name_ar           VARCHAR(200) NOT NULL,
  name_en           VARCHAR(200) NOT NULL,
  description        TEXT,
  sort_order         INTEGER      NOT NULL DEFAULT 0,
  is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by         UUID,
  CONSTRAINT ck_correspondence_type_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE correspondence_type IS 'Dynamic correspondence category (inbound, outbound, internal, circular, etc.).';

-- -----------------------------------------------------------------------------
-- correspondence_status (lifecycle; may be scoped per correspondence type)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_status (
  id                      BIGSERIAL PRIMARY KEY,
  correspondence_type_id  BIGINT REFERENCES correspondence_type (id) ON DELETE SET NULL,
  code                    VARCHAR(64)  NOT NULL,
  name_ar                VARCHAR(200) NOT NULL,
  name_en                VARCHAR(200) NOT NULL,
  description             TEXT,
  sort_order              INTEGER      NOT NULL DEFAULT 0,
  is_terminal             BOOLEAN      NOT NULL DEFAULT FALSE,
  is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at              TIMESTAMPTZ,
  deleted_by              UUID,
  created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by              UUID,
  updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by              UUID,
  CONSTRAINT ck_correspondence_status_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE correspondence_status IS 'Lifecycle status; optional FK to correspondence_type when status set is type-specific.';
COMMENT ON COLUMN correspondence_status.is_terminal IS 'TRUE when correspondence cannot transition further without archival rules.';

-- -----------------------------------------------------------------------------
-- priority (درجة الاستعجال)
-- -----------------------------------------------------------------------------
CREATE TABLE priority (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  sla_days    INTEGER,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_priority_sort_non_negative CHECK (sort_order >= 0),
  CONSTRAINT ck_priority_sla_positive CHECK (sla_days IS NULL OR sla_days > 0)
);

COMMENT ON TABLE priority IS 'Urgency / priority; optional sla_days for auto-escalation rules (SRS FR-403 / BR-011–012).';
COMMENT ON COLUMN priority.sla_days IS 'Optional business-day hint for escalation timers; enforced in application/workflow.';

-- -----------------------------------------------------------------------------
-- confidentiality (درجة السرية)
-- -----------------------------------------------------------------------------
CREATE TABLE confidentiality (
  id                   BIGSERIAL PRIMARY KEY,
  code                 VARCHAR(64)  NOT NULL,
  name_ar             VARCHAR(200) NOT NULL,
  name_en             VARCHAR(200) NOT NULL,
  description          TEXT,
  sort_order           INTEGER      NOT NULL DEFAULT 0,
  restricts_export     BOOLEAN      NOT NULL DEFAULT FALSE,
  requires_clearance   BOOLEAN      NOT NULL DEFAULT FALSE,
  is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at           TIMESTAMPTZ,
  deleted_by           UUID,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by           UUID,
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by           UUID,
  CONSTRAINT ck_confidentiality_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE confidentiality IS 'Secrecy level; flags map to SRS BR-020–022 behavior in application layer.';
COMMENT ON COLUMN confidentiality.restricts_export IS 'When TRUE, print/export requires elevated permission.';
COMMENT ON COLUMN confidentiality.requires_clearance IS 'When TRUE, only explicitly cleared users may view.';

-- -----------------------------------------------------------------------------
-- workflow_action_type (إحالة، اعتماد، رفض، إعادة، …) — FR-402
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_action_type (
  id           BIGSERIAL PRIMARY KEY,
  code         VARCHAR(64)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description  TEXT,
  sort_order   INTEGER      NOT NULL DEFAULT 0,
  is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at   TIMESTAMPTZ,
  deleted_by   UUID,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by   UUID,
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by   UUID,
  CONSTRAINT ck_workflow_action_type_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE workflow_action_type IS 'Catalog of workflow / history action codes (no DB ENUM).';

-- -----------------------------------------------------------------------------
-- notification_channel (داخلية، بريد، SMS، push) — FR-801
-- -----------------------------------------------------------------------------
CREATE TABLE notification_channel (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_notification_channel_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE notification_channel IS 'Delivery channel for notifications.';

-- -----------------------------------------------------------------------------
-- notification_event_type (ورود، إحالة، اعتماد، …) — FR-802
-- -----------------------------------------------------------------------------
CREATE TABLE notification_event_type (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_notification_event_type_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE notification_event_type IS 'Business event that may spawn notifications.';

-- -----------------------------------------------------------------------------
-- attachment_content_type (PDF, DOCX, …) — SRS §14.1 allowed formats
-- -----------------------------------------------------------------------------
CREATE TABLE attachment_content_type (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  mime_types  TEXT[],
  max_bytes   BIGINT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_attachment_content_type_sort_non_negative CHECK (sort_order >= 0),
  CONSTRAINT ck_attachment_content_type_max_bytes CHECK (max_bytes IS NULL OR max_bytes > 0)
);

COMMENT ON TABLE attachment_content_type IS 'Logical file categories and optional MIME whitelist / per-type size cap.';

-- -----------------------------------------------------------------------------
-- workflow_instance_status (running, completed, terminated, suspended)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_instance_status (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_workflow_instance_status_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE workflow_instance_status IS 'State of a workflow instance bridge row (Camunda external state mirror).';

-- -----------------------------------------------------------------------------
-- notification_delivery_status (pending, sent, failed, skipped)
-- -----------------------------------------------------------------------------
CREATE TABLE notification_delivery_status (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar    VARCHAR(200) NOT NULL,
  name_en    VARCHAR(200) NOT NULL,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_notification_delivery_status_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE notification_delivery_status IS 'Per-channel delivery outcome.';

-- -----------------------------------------------------------------------------
-- Seed data (minimal bootstrap codes; expand via admin UI / later migrations)
-- -----------------------------------------------------------------------------
INSERT INTO correspondence_type (code, name_ar, name_en, sort_order) VALUES
  ('INBOUND',  'وارد',   'Inbound',  10),
  ('OUTBOUND', 'صادر',   'Outbound', 20),
  ('INTERNAL', 'داخلي',  'Internal', 30),
  ('EXTERNAL', 'خارجي',  'External', 40),
  ('CIRCULAR', 'تعميم',  'Circular', 50),
  ('DECISION', 'قرار إداري', 'Administrative decision', 60);

INSERT INTO correspondence_status (correspondence_type_id, code, name_ar, name_en, sort_order, is_terminal) VALUES
  (NULL, 'NEW',        'جديدة',          'New',        10, FALSE),
  (NULL, 'IN_PROGRESS','قيد الإجراء',   'In progress',20, FALSE),
  (NULL, 'RETURNED',   'معادة',         'Returned',   30, FALSE),
  (NULL, 'COMPLETED',  'منجزة',         'Completed',  40, TRUE),
  (NULL, 'REJECTED',   'مرفوضة',        'Rejected',   50, TRUE),
  (NULL, 'ARCHIVED',   'مؤرشفة',        'Archived',   60, TRUE),
  (NULL, 'DEFERRED',   'مؤجلة',         'Deferred',   70, FALSE),
  (NULL, 'PENDING_APPROVAL', 'بانتظار الاعتماد', 'Pending approval', 80, FALSE);

INSERT INTO priority (code, name_ar, name_en, sort_order, sla_days) VALUES
  ('LOW',       'منخفضة',  'Low',       40, NULL),
  ('NORMAL',    'عادية',    'Normal',    30, NULL),
  ('HIGH',      'عالية',    'High',      20, 3),
  ('URGENT',    'عاجلة',    'Urgent',    10, 3),
  ('VERY_URGENT','عاجلة جداً', 'Very urgent', 5, 1);

INSERT INTO confidentiality (code, name_ar, name_en, sort_order, restricts_export, requires_clearance) VALUES
  ('NORMAL',     'عادي',       'Normal',      50, FALSE, FALSE),
  ('LIMITED',    'محدود',      'Limited',     40, FALSE, FALSE),
  ('SECRET',     'سري',        'Secret',      30, TRUE,  FALSE),
  ('TOP_SECRET', 'سري للغاية', 'Top secret',  10, TRUE,  TRUE);

INSERT INTO workflow_action_type (code, name_ar, name_en, sort_order) VALUES
  ('RECEIVE',      'استلام',       'Receive',       10),
  ('REGISTER',     'تسجيل',        'Register',      20),
  ('ROUTE',        'توجيه',        'Route',         30),
  ('REFER',        'إحالة',        'Refer',         40),
  ('APPROVE',      'اعتماد',       'Approve',       50),
  ('REJECT',       'رفض',          'Reject',        60),
  ('RETURN',       'إعادة',        'Return',        70),
  ('ARCHIVE',      'أرشفة',        'Archive',       80),
  ('DEFER',        'تأجيل',        'Defer',         90),
  ('ESCALATE',     'تصعيد',        'Escalate',      100),
  ('REQUEST_OPINION','طلب رأي',    'Request opinion', 110),
  ('FYI',          'إخطار للعلم',  'FYI',           120),
  ('CLOSE',        'إغلاق',        'Close',         130),
  ('SIGN',         'توقيع',        'Sign',          140),
  ('SCAN',         'مسح ضوئي',     'Scan',          150);

INSERT INTO notification_channel (code, name_ar, name_en, sort_order) VALUES
  ('IN_APP', 'داخل النظام', 'In-app', 10),
  ('EMAIL',  'البريد الإلكتروني', 'Email', 20),
  ('SMS',    'رسالة نصية', 'SMS', 30),
  ('PUSH',   'إشعار الجوال', 'Push', 40);

INSERT INTO notification_event_type (code, name_ar, name_en, sort_order) VALUES
  ('CORRESPONDENCE_CREATED', 'إنشاء معاملة', 'Correspondence created', 10),
  ('CORRESPONDENCE_INBOUND', 'ورود معاملة', 'Inbound correspondence', 20),
  ('ASSIGNED',               'إحالة / تعيين', 'Assigned / referred', 30),
  ('APPROVED',               'اعتماد', 'Approved', 40),
  ('REJECTED',               'رفض', 'Rejected', 50),
  ('RETURNED',               'إعادة', 'Returned', 60),
  ('DUE_SOON',               'اقتراب الاستحقاق', 'Due soon', 70),
  ('OVERDUE',                'تأخر', 'Overdue', 80),
  ('COMMENT_ADDED',          'تعليق جديد', 'Comment added', 90),
  ('DELEGATION_STARTED',     'بدء تفويض', 'Delegation started', 100),
  ('DELEGATION_ENDED',       'انتهاء تفويض', 'Delegation ended', 110);

INSERT INTO attachment_content_type (code, name_ar, name_en, mime_types, max_bytes, sort_order) VALUES
  ('PDF',  'PDF',  'PDF',  ARRAY['application/pdf'], 52428800, 10),
  ('DOCX', 'Word', 'Word', ARRAY['application/vnd.openxmlformats-officedocument.wordprocessingml.document'], 52428800, 20),
  ('XLSX', 'Excel','Excel',ARRAY['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'], 52428800, 30),
  ('PPTX', 'عروض','Presentations',ARRAY['application/vnd.openxmlformats-officedocument.presentationml.presentation'],52428800,40),
  ('IMAGE','صور', 'Images',ARRAY['image/jpeg','image/png','image/tiff'], 52428800, 50),
  ('MSG',  'Outlook','Outlook',ARRAY['application/vnd.ms-outlook'], 52428800, 60);

INSERT INTO workflow_instance_status (code, name_ar, name_en, sort_order) VALUES
  ('RUNNING',    'قيد التشغيل', 'Running',    10),
  ('COMPLETED',  'مكتمل',       'Completed',  20),
  ('TERMINATED', 'منهي',        'Terminated', 30),
  ('SUSPENDED',  'معلق',        'Suspended',  40);

INSERT INTO notification_delivery_status (code, name_ar, name_en, sort_order) VALUES
  ('PENDING', 'قيد الإرسال', 'Pending', 10),
  ('SENT',    'تم الإرسال',  'Sent',    20),
  ('FAILED',  'فشل',         'Failed',  30),
  ('SKIPPED', 'تخطي',        'Skipped', 40);

-- ---------- source: V3__core_tables.sql ----------

-- =============================================================================
-- V3__core_tables.sql
-- Core business tables: org, users, RBAC, correspondence, attachments,
-- comments, delegation, templates, retention, audit log.
-- Audit user FKs (created_by/updated_by/deleted_by) → app_user added in V6.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- department (internal organizational hierarchy)
-- -----------------------------------------------------------------------------
CREATE TABLE department (
  id              BIGSERIAL PRIMARY KEY,
  parent_id       BIGINT REFERENCES department (id) ON DELETE SET NULL,
  code            VARCHAR(64)  NOT NULL,
  name_ar         VARCHAR(250) NOT NULL,
  name_en         VARCHAR(250) NOT NULL,
  description     TEXT,
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_department_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE department IS 'Internal hierarchy for routing, circular distribution, and dashboards (FR-701).';

-- -----------------------------------------------------------------------------
-- organization (government / external entities — FR-101 / FR-201)
-- -----------------------------------------------------------------------------
CREATE TABLE organization (
  id              BIGSERIAL PRIMARY KEY,
  parent_id       BIGINT REFERENCES organization (id) ON DELETE SET NULL,
  code            VARCHAR(64),
  name_ar         VARCHAR(250) NOT NULL,
  name_en         VARCHAR(250) NOT NULL,
  is_external     BOOLEAN      NOT NULL DEFAULT FALSE,
  description     TEXT,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID
);

COMMENT ON TABLE organization IS 'Sender/recipient governmental or partner entity; distinct from internal department.';

-- -----------------------------------------------------------------------------
-- classification (hierarchical indexing — FR-601)
-- -----------------------------------------------------------------------------
CREATE TABLE classification (
  id              BIGSERIAL PRIMARY KEY,
  parent_id       BIGINT REFERENCES classification (id) ON DELETE SET NULL,
  code            VARCHAR(64)  NOT NULL,
  name_ar         VARCHAR(250) NOT NULL,
  name_en         VARCHAR(250) NOT NULL,
  description     TEXT,
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_classification_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE classification IS 'Configurable multi-level classification plan for correspondence and archives.';

-- -----------------------------------------------------------------------------
-- permission (fine-grained authorization — SRS §11.2)
-- -----------------------------------------------------------------------------
CREATE TABLE permission (
  id              BIGSERIAL PRIMARY KEY,
  code            VARCHAR(128) NOT NULL,
  name_ar        VARCHAR(200) NOT NULL,
  name_en        VARCHAR(200) NOT NULL,
  description     TEXT,
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_permission_sort_non_negative CHECK (sort_order >= 0)
);

-- -----------------------------------------------------------------------------
-- role
-- -----------------------------------------------------------------------------
CREATE TABLE role (
  id              BIGSERIAL PRIMARY KEY,
  code            VARCHAR(64)  NOT NULL,
  name_ar        VARCHAR(200) NOT NULL,
  name_en        VARCHAR(200) NOT NULL,
  description     TEXT,
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_role_sort_non_negative CHECK (sort_order >= 0)
);

-- -----------------------------------------------------------------------------
-- role_permission (M:N)
-- -----------------------------------------------------------------------------
CREATE TABLE role_permission (
  role_id         BIGINT NOT NULL REFERENCES role (id) ON DELETE CASCADE,
  permission_id   BIGINT NOT NULL REFERENCES permission (id) ON DELETE CASCADE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by      UUID,
  PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE role_permission IS 'Maps roles to permissions; rows are revoked by DELETE (history via audit_log).';

-- -----------------------------------------------------------------------------
-- app_user (avoid reserved word "user")
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  -- public.citext: JDBC currentSchema=srs_system limits search_path; type lives in public after V1.
  username           public.citext NOT NULL,
  password_hash      TEXT,
  full_name_ar       VARCHAR(200) NOT NULL,
  full_name_en       VARCHAR(200) NOT NULL,
  email              public.citext NOT NULL,
  phone              VARCHAR(32),
  national_id        VARCHAR(32),
  department_id      BIGINT       NOT NULL REFERENCES department (id) ON DELETE RESTRICT,
  is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
  locked_until       TIMESTAMPTZ,
  failed_login_count INTEGER      NOT NULL DEFAULT 0,
  last_login_at      TIMESTAMPTZ,
  password_changed_at TIMESTAMPTZ,
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by         UUID,
  CONSTRAINT ck_app_user_failed_login_non_negative CHECK (failed_login_count >= 0)
);

COMMENT ON TABLE app_user IS 'Interactive user accounts; integrates with AD/LDAP/SSO in application layer (SRS §11.1).';

-- -----------------------------------------------------------------------------
-- user_role (M:N)
-- -----------------------------------------------------------------------------
CREATE TABLE user_role (
  app_user_id     UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  role_id         BIGINT NOT NULL REFERENCES role (id) ON DELETE CASCADE,
  valid_from      TIMESTAMPTZ NOT NULL DEFAULT now(),
  valid_to        TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by      UUID,
  PRIMARY KEY (app_user_id, role_id)
);

COMMENT ON TABLE user_role IS 'Role grants; optional validity window for temporary assignments.';

-- -----------------------------------------------------------------------------
-- correspondence (main entity — SRS §8.1 + FR-100/200/300)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence (
  id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reference_number            VARCHAR(64)  NOT NULL,
  correspondence_type_id    BIGINT       NOT NULL REFERENCES correspondence_type (id) ON DELETE RESTRICT,
  correspondence_status_id  BIGINT       NOT NULL REFERENCES correspondence_status (id) ON DELETE RESTRICT,
  priority_id                 BIGINT       NOT NULL REFERENCES priority (id) ON DELETE RESTRICT,
  confidentiality_id          BIGINT       NOT NULL REFERENCES confidentiality (id) ON DELETE RESTRICT,
  classification_id           BIGINT       NOT NULL REFERENCES classification (id) ON DELETE RESTRICT,
  subject                     VARCHAR(500) NOT NULL,
  description                 TEXT,
  body_html                   TEXT,
  sender_organization_id      BIGINT REFERENCES organization (id) ON DELETE SET NULL,
  recipient_organization_id   BIGINT REFERENCES organization (id) ON DELETE SET NULL,
  external_reference_number   VARCHAR(128),
  external_reference_date     DATE,
  owner_department_id         BIGINT REFERENCES department (id) ON DELETE SET NULL,
  due_date                    TIMESTAMPTZ,
  barcode_value               VARCHAR(100),
  total_attachment_bytes      BIGINT       NOT NULL DEFAULT 0,
  deleted_at                  TIMESTAMPTZ,
  deleted_by                  UUID,
  created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                  UUID,
  updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                  UUID,
  CONSTRAINT ck_correspondence_total_attachment_bytes CHECK (total_attachment_bytes >= 0)
);

COMMENT ON TABLE correspondence IS 'Official correspondence record; workflow state also mirrored in workflow_* tables.';
COMMENT ON COLUMN correspondence.reference_number IS 'Human-visible registry number (SRS numbering rules BR-001–003).';
COMMENT ON COLUMN correspondence.owner_department_id IS 'Current owning / primary routing department.';
COMMENT ON COLUMN correspondence.total_attachment_bytes IS 'Sum of current attachment versions; enforce 200MB cap in application (SRS §14.1).';

-- -----------------------------------------------------------------------------
-- correspondence_recipient (circular distribution + read tracking — FR-302)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_recipient (
  id                 BIGSERIAL PRIMARY KEY,
  correspondence_id  UUID   NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  department_id      BIGINT NOT NULL REFERENCES department (id) ON DELETE CASCADE,
  first_read_at      TIMESTAMPTZ,
  last_read_at       TIMESTAMPTZ,
  read_count         INTEGER NOT NULL DEFAULT 0,
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by         UUID,
  CONSTRAINT uq_correspondence_recipient UNIQUE (correspondence_id, department_id),
  CONSTRAINT ck_correspondence_recipient_read_count CHECK (read_count >= 0)
);

COMMENT ON TABLE correspondence_recipient IS 'Per-department circular distribution and read receipt.';

-- -----------------------------------------------------------------------------
-- attachment (logical document; versions in attachment_version — FR-602/603)
-- -----------------------------------------------------------------------------
CREATE TABLE attachment (
  id                     BIGSERIAL PRIMARY KEY,
  correspondence_id      UUID   NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  content_type_id        BIGINT REFERENCES attachment_content_type (id) ON DELETE SET NULL,
  display_name           VARCHAR(500) NOT NULL,
  current_version_id     BIGINT,
  is_active              BOOLEAN NOT NULL DEFAULT TRUE,
  deleted_at             TIMESTAMPTZ,
  deleted_by             UUID,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by             UUID,
  updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by             UUID
);

COMMENT ON TABLE attachment IS 'Logical attachment; binary blobs referenced by attachment_version.storage_key.';

-- -----------------------------------------------------------------------------
-- attachment_version (immutable version chain — FR-603)
-- -----------------------------------------------------------------------------
CREATE TABLE attachment_version (
  id                 BIGSERIAL PRIMARY KEY,
  attachment_id      BIGINT NOT NULL REFERENCES attachment (id) ON DELETE CASCADE,
  version_number     INTEGER NOT NULL,
  storage_key        TEXT   NOT NULL,
  byte_size          BIGINT NOT NULL,
  mime_type          VARCHAR(200),
  checksum_sha256    VARCHAR(64),
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by         UUID,
  CONSTRAINT uq_attachment_version UNIQUE (attachment_id, version_number),
  CONSTRAINT ck_attachment_version_positive CHECK (version_number > 0 AND byte_size >= 0)
);

COMMENT ON TABLE attachment_version IS 'Single stored blob per version; compare/restore via version_number.';

-- FK attachment.current_version_id → attachment_version (defer circular: add after attachment_version exists)
ALTER TABLE attachment
  ADD CONSTRAINT fk_attachment_current_version
  FOREIGN KEY (current_version_id) REFERENCES attachment_version (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- correspondence_comment (notes / threaded discussion — FR-802 comment event)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_comment (
  id                 BIGSERIAL PRIMARY KEY,
  correspondence_id  UUID   NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  author_user_id     UUID   NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  body               TEXT   NOT NULL,
  parent_comment_id  BIGINT REFERENCES correspondence_comment (id) ON DELETE CASCADE,
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by         UUID
);

COMMENT ON TABLE correspondence_comment IS 'User-visible comments; audit_log captures edits/deletes.';

-- -----------------------------------------------------------------------------
-- delegation (FR-404, BR-014/015)
-- -----------------------------------------------------------------------------
CREATE TABLE delegation (
  id                 BIGSERIAL PRIMARY KEY,
  delegator_user_id  UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  delegate_user_id   UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  start_at           TIMESTAMPTZ NOT NULL,
  end_at             TIMESTAMPTZ NOT NULL,
  reason             TEXT,
  is_revoked         BOOLEAN NOT NULL DEFAULT FALSE,
  revoked_at         TIMESTAMPTZ,
  revoked_by         UUID,
  deleted_at         TIMESTAMPTZ,
  deleted_by         UUID,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         UUID,
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by         UUID,
  CONSTRAINT ck_delegation_window CHECK (end_at > start_at),
  CONSTRAINT ck_delegation_distinct_users CHECK (delegator_user_id <> delegate_user_id)
);

COMMENT ON TABLE delegation IS 'Temporary transfer of authority; at most one active row per delegator enforced in V6 (partial index).';

-- -----------------------------------------------------------------------------
-- template (FR-202)
-- -----------------------------------------------------------------------------
CREATE TABLE template (
  id                      BIGSERIAL PRIMARY KEY,
  code                    VARCHAR(64) NOT NULL,
  name_ar                 VARCHAR(250) NOT NULL,
  name_en                 VARCHAR(250) NOT NULL,
  correspondence_type_id  BIGINT REFERENCES correspondence_type (id) ON DELETE SET NULL,
  body_html               TEXT NOT NULL,
  is_active               BOOLEAN NOT NULL DEFAULT TRUE,
  deleted_at              TIMESTAMPTZ,
  deleted_by              UUID,
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by              UUID,
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by              UUID
);

COMMENT ON TABLE template IS 'Official correspondence templates (letters, memos, circulars).';

-- -----------------------------------------------------------------------------
-- retention_policy (FR-602)
-- -----------------------------------------------------------------------------
CREATE TABLE retention_policy (
  id                      BIGSERIAL PRIMARY KEY,
  classification_id       BIGINT REFERENCES classification (id) ON DELETE CASCADE,
  correspondence_type_id  BIGINT REFERENCES correspondence_type (id) ON DELETE CASCADE,
  retention_days          INTEGER NOT NULL,
  warn_before_days        INTEGER NOT NULL DEFAULT 30,
  deleted_at              TIMESTAMPTZ,
  deleted_by              UUID,
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by              UUID,
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by              UUID,
  CONSTRAINT ck_retention_policy_scope CHECK (
    classification_id IS NOT NULL OR correspondence_type_id IS NOT NULL
  ),
  CONSTRAINT ck_retention_policy_days CHECK (retention_days > 0 AND warn_before_days >= 0)
);

COMMENT ON TABLE retention_policy IS 'Retention duration by classification and/or correspondence type.';

-- -----------------------------------------------------------------------------
-- audit_log (Auditor role — SRS §3.2; immutable business/security trail)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
  id              BIGSERIAL PRIMARY KEY,
  occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  actor_user_id   UUID REFERENCES app_user (id) ON DELETE SET NULL,
  entity_type     VARCHAR(128) NOT NULL,
  entity_id       VARCHAR(128) NOT NULL,
  action_code     VARCHAR(64)  NOT NULL,
  ip_address      INET,
  user_agent      TEXT,
  payload         JSONB,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID
);

COMMENT ON TABLE audit_log IS 'Append-heavy audit trail; prefer insert-only in application layer.';
COMMENT ON COLUMN audit_log.entity_id IS 'String to allow UUID or bigint references.';

-- =============================================================================
-- Bootstrap rows (synthetic department + system user for FK seeds & Flyway)
-- =============================================================================
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
VALUES ('ROOT', 'الجهة', 'Organization root', NULL, 0);

INSERT INTO classification (code, name_ar, name_en, parent_id, sort_order)
VALUES ('GEN', 'عام', 'General', NULL, 0);

INSERT INTO organization (code, name_ar, name_en, is_external)
VALUES ('INT', 'الجهة الحالية', 'Current entity', FALSE);

-- Well-known system principal (Flyway seeds, async jobs, integration adapters)
INSERT INTO app_user (
  id, username, password_hash, full_name_ar, full_name_en, email, department_id, is_active
) VALUES (
  '00000000-0000-0000-0000-000000000001',
  'system',
  NULL,
  'نظام',
  'System',
  'system@local.invalid',
  1,
  TRUE
);

INSERT INTO role (code, name_ar, name_en, sort_order) VALUES
  ('SYS_ADMIN',     'مدير النظام',           'System administrator',       10),
  ('CORRESP_MGR',   'مدير الاتصالات',        'Correspondence manager',       20),
  ('CORRESP_CLERK', 'موظف اتصالات إدارية',   'Correspondence clerk',         30),
  ('DEPT_MANAGER',  'مدير إدارة',            'Department manager',           40),
  ('STAFF',         'موظف',                  'Staff',                        50),
  ('APPROVER',      'معتمد',                 'Approver',                     60),
  ('AUDITOR',       'مدقق',                  'Auditor',                      70);

INSERT INTO permission (code, name_ar, name_en, sort_order) VALUES
  ('correspondence.view',       'عرض المعاملات',       'View correspondence',       10),
  ('correspondence.create',     'إنشاء معاملات',       'Create correspondence',     20),
  ('correspondence.edit',       'تعديل معاملات',       'Edit correspondence',       30),
  ('correspondence.delete',     'حذف معاملات',         'Delete correspondence',     40),
  ('correspondence.approve',    'اعتماد معاملات',      'Approve correspondence',    50),
  ('workflow.execute',          'تنفيذ مهام سير العمل','Execute workflow tasks',    60),
  ('user.manage',               'إدارة المستخدمين',    'Manage users',              70),
  ('role.manage',               'إدارة الأدوار',       'Manage roles',              80),
  ('report.view',               'عرض التقارير',        'View reports',              90),
  ('audit.view',                'عرض سجل التدقيق',     'View audit log',           100),
  ('admin.settings',            'إعدادات النظام',      'System settings',          110),
  ('lookup.manage',             'إدارة القوائم',       'Manage lookups',           120);

-- SYS_ADMIN: all permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN';

-- AUDITOR: read-only audit + reports
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('audit.view', 'report.view', 'correspondence.view')
WHERE r.code = 'AUDITOR';

-- APPROVER
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'correspondence.view', 'correspondence.approve', 'workflow.execute', 'report.view'
)
WHERE r.code = 'APPROVER';

-- CORRESP_CLERK
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'correspondence.view', 'correspondence.create', 'correspondence.edit', 'workflow.execute'
)
WHERE r.code = 'CORRESP_CLERK';

-- STAFF
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'correspondence.view', 'correspondence.create', 'correspondence.edit', 'workflow.execute'
)
WHERE r.code = 'STAFF';

-- DEPT_MANAGER
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'correspondence.view', 'correspondence.create', 'correspondence.edit',
  'correspondence.approve', 'workflow.execute', 'report.view'
)
WHERE r.code = 'DEPT_MANAGER';

-- CORRESP_MGR
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'correspondence.view', 'correspondence.create', 'correspondence.edit', 'correspondence.delete',
  'correspondence.approve', 'workflow.execute', 'report.view', 'lookup.manage'
)
WHERE r.code = 'CORRESP_MGR';

INSERT INTO user_role (app_user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM role WHERE code = 'SYS_ADMIN';

-- ---------- source: V4__workflow_tables.sql ----------

-- =============================================================================
-- V4__workflow_tables.sql
-- Workflow bridge to Camunda + append-only action history (SRS FR-400, FR-105).
-- Process state of record remains in Camunda; this schema supports APIs & audit.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- workflow_instance (one active bridge row per correspondence recommended)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_instance (
  id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  correspondence_id          UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  process_definition_key     VARCHAR(255) NOT NULL,
  process_instance_id        VARCHAR(64)  NOT NULL,
  workflow_instance_status_id BIGINT      NOT NULL REFERENCES workflow_instance_status (id) ON DELETE RESTRICT,
  started_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  ended_at                   TIMESTAMPTZ,
  business_key               VARCHAR(128),
  deleted_at                 TIMESTAMPTZ,
  deleted_by                 UUID,
  created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                 UUID,
  updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by                 UUID,
  CONSTRAINT uq_workflow_instance_process UNIQUE (process_instance_id),
  CONSTRAINT ck_workflow_instance_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

COMMENT ON TABLE workflow_instance IS 'Links correspondence to Camunda processInstanceId and definition key.';
COMMENT ON COLUMN workflow_instance.business_key IS 'Optional Camunda business key (often reference_number).';

-- -----------------------------------------------------------------------------
-- workflow_action (immutable business steps; mirrors Camunda history + SRS table)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_action (
  id                       BIGSERIAL PRIMARY KEY,
  workflow_instance_id     UUID        NOT NULL REFERENCES workflow_instance (id) ON DELETE CASCADE,
  correspondence_id        UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  workflow_action_type_id  BIGINT      NOT NULL REFERENCES workflow_action_type (id) ON DELETE RESTRICT,
  actor_user_id            UUID REFERENCES app_user (id) ON DELETE SET NULL,
  comment_text             TEXT,
  payload                  JSONB,
  camunda_task_id          VARCHAR(64),
  camunda_activity_id      VARCHAR(128),
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by               UUID,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by               UUID
);

COMMENT ON TABLE workflow_action IS 'Each row records one workflow action for timeline APIs (FR-105, FR-402).';
COMMENT ON COLUMN workflow_action.payload IS 'Variable snapshot or structured metadata (decision, targets, etc.).';

-- ---------- source: V5__notification_tables.sql ----------

-- =============================================================================
-- V5__notification_tables.sql
-- In-app notification store + per-channel delivery attempts (SRS FR-801/802).
-- Email/SMS/Push adapters record outcomes in notification_delivery.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- notification
-- -----------------------------------------------------------------------------
CREATE TABLE notification (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_user_id        UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  notification_event_type_id BIGINT     NOT NULL REFERENCES notification_event_type (id) ON DELETE RESTRICT,
  correspondence_id        UUID REFERENCES correspondence (id) ON DELETE CASCADE,
  title_ar                 VARCHAR(500) NOT NULL,
  title_en                 VARCHAR(500) NOT NULL,
  body_ar                  TEXT,
  body_en                  TEXT,
  data                     JSONB,
  read_at                  TIMESTAMPTZ,
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by               UUID,
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by               UUID
);

COMMENT ON TABLE notification IS 'User notification inbox; channel dispatch tracked in notification_delivery.';
COMMENT ON COLUMN notification.data IS 'Structured payload for UI rendering or downstream template merge.';

-- -----------------------------------------------------------------------------
-- notification_delivery
-- -----------------------------------------------------------------------------
CREATE TABLE notification_delivery (
  id                           BIGSERIAL PRIMARY KEY,
  notification_id              UUID        NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
  notification_channel_id      BIGINT      NOT NULL REFERENCES notification_channel (id) ON DELETE RESTRICT,
  notification_delivery_status_id BIGINT NOT NULL REFERENCES notification_delivery_status (id) ON DELETE RESTRICT,
  attempt_count                INTEGER     NOT NULL DEFAULT 0,
  last_error                   TEXT,
  sent_at                      TIMESTAMPTZ,
  external_message_id          VARCHAR(256),
  deleted_at                   TIMESTAMPTZ,
  deleted_by                   UUID,
  created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                   UUID,
  updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                   UUID,
  CONSTRAINT ck_notification_delivery_attempts CHECK (attempt_count >= 0)
);

COMMENT ON TABLE notification_delivery IS 'One row per targeted channel; supports retries and failure diagnostics.';

-- ---------- source: V6__indexes.sql ----------

-- =============================================================================
-- V6__indexes.sql
-- Search & FK performance indexes, partial uniques (soft-delete safe),
-- audit column foreign keys â†’ app_user, and updated_at triggers.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Partial UNIQUE: lookup & master "code" columns (active rows only)
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_correspondence_type_code_active
  ON correspondence_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_status_global_code_active
  ON correspondence_status (code)
  WHERE correspondence_type_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_status_type_code_active
  ON correspondence_status (correspondence_type_id, code)
  WHERE correspondence_type_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_priority_code_active
  ON priority (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_confidentiality_code_active
  ON confidentiality (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_workflow_action_type_code_active
  ON workflow_action_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_channel_code_active
  ON notification_channel (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_event_type_code_active
  ON notification_event_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_attachment_content_type_code_active
  ON attachment_content_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_workflow_instance_status_code_active
  ON workflow_instance_status (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_delivery_status_code_active
  ON notification_delivery_status (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_department_code_active
  ON department (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_organization_code_active
  ON organization (code) WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_classification_root_code_active
  ON classification (code) WHERE parent_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_classification_child_code_active
  ON classification (parent_id, code) WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_permission_code_active
  ON permission (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_role_code_active
  ON role (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_template_code_active
  ON template (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_app_user_username_active
  ON app_user (username) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_app_user_email_active
  ON app_user (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_reference_active
  ON correspondence (reference_number) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_barcode_active
  ON correspondence (barcode_value) WHERE deleted_at IS NULL AND barcode_value IS NOT NULL;

CREATE UNIQUE INDEX ux_workflow_instance_correspondence_active
  ON workflow_instance (correspondence_id) WHERE deleted_at IS NULL;

-- BR-014: at most one non-revoked, non-deleted delegation row per delegator
CREATE UNIQUE INDEX ux_delegation_one_active_per_delegator
  ON delegation (delegator_user_id)
  WHERE NOT is_revoked AND deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Foreign keys: audit & soft-delete actor columns â†’ app_user
-- -----------------------------------------------------------------------------
ALTER TABLE correspondence_type
  ADD CONSTRAINT fk_correspondence_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_status
  ADD CONSTRAINT fk_correspondence_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE priority
  ADD CONSTRAINT fk_priority_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_priority_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_priority_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE confidentiality
  ADD CONSTRAINT fk_confidentiality_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_confidentiality_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_confidentiality_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_action_type
  ADD CONSTRAINT fk_workflow_action_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_channel
  ADD CONSTRAINT fk_notification_channel_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_channel_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_channel_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_event_type
  ADD CONSTRAINT fk_notification_event_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_event_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_event_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment_content_type
  ADD CONSTRAINT fk_attachment_content_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_content_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_content_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_instance_status
  ADD CONSTRAINT fk_workflow_instance_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_delivery_status
  ADD CONSTRAINT fk_notification_delivery_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE department
  ADD CONSTRAINT fk_department_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_department_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_department_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE organization
  ADD CONSTRAINT fk_organization_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_organization_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_organization_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE classification
  ADD CONSTRAINT fk_classification_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_classification_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_classification_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE permission
  ADD CONSTRAINT fk_permission_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_permission_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_permission_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE role
  ADD CONSTRAINT fk_role_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE role_permission
  ADD CONSTRAINT fk_role_permission_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_permission_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE app_user
  ADD CONSTRAINT fk_app_user_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_app_user_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_app_user_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE user_role
  ADD CONSTRAINT fk_user_role_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_user_role_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence
  ADD CONSTRAINT fk_correspondence_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_recipient
  ADD CONSTRAINT fk_correspondence_recipient_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_recipient_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_recipient_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment
  ADD CONSTRAINT fk_attachment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment_version
  ADD CONSTRAINT fk_attachment_version_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_version_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_version_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_comment
  ADD CONSTRAINT fk_correspondence_comment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_comment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_comment_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE delegation
  ADD CONSTRAINT fk_delegation_revoked_by FOREIGN KEY (revoked_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE template
  ADD CONSTRAINT fk_template_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_template_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_template_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE retention_policy
  ADD CONSTRAINT fk_retention_policy_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_retention_policy_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_retention_policy_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE audit_log
  ADD CONSTRAINT fk_audit_log_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_audit_log_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_instance
  ADD CONSTRAINT fk_workflow_instance_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_action
  ADD CONSTRAINT fk_workflow_action_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification
  ADD CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_delivery
  ADD CONSTRAINT fk_notification_delivery_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- Secondary indexes (queries, joins, reporting)
-- -----------------------------------------------------------------------------
CREATE INDEX ix_department_parent ON department (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_organization_parent ON organization (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_classification_parent ON classification (parent_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_correspondence_type ON correspondence (correspondence_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_status ON correspondence (correspondence_status_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_priority ON correspondence (priority_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_confidentiality ON correspondence (confidentiality_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_classification ON correspondence (classification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_owner_dept ON correspondence (owner_department_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_sender_org ON correspondence (sender_organization_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_recipient_org ON correspondence (recipient_organization_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_created_at ON correspondence (created_at) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_due_date ON correspondence (due_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_created_by ON correspondence (created_by) WHERE deleted_at IS NULL;

CREATE INDEX ix_correspondence_recipient_dept ON correspondence_recipient (department_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_recipient_corr ON correspondence_recipient (correspondence_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_attachment_correspondence ON attachment (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_attachment_content_type ON attachment (content_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_attachment_version_attachment ON attachment_version (attachment_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_comment_correspondence ON correspondence_comment (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_comment_author ON correspondence_comment (author_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_comment_parent ON correspondence_comment (parent_comment_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_delegation_delegator ON delegation (delegator_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_delegation_delegate ON delegation (delegate_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_delegation_window ON delegation (start_at, end_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_template_type ON template (correspondence_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_retention_classification ON retention_policy (classification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_retention_corr_type ON retention_policy (correspondence_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_audit_occurred ON audit_log (occurred_at);
CREATE INDEX ix_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX ix_audit_actor ON audit_log (actor_user_id);

CREATE INDEX ix_workflow_instance_corr ON workflow_instance (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_instance_status ON workflow_instance (workflow_instance_status_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_instance_started ON workflow_instance (started_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_workflow_action_instance ON workflow_action (workflow_instance_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_corr ON workflow_action (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_type ON workflow_action (workflow_action_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_actor ON workflow_action (actor_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_created ON workflow_action (created_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_notification_recipient ON notification (recipient_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_event ON notification (notification_event_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_corr ON notification (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_unread ON notification (recipient_user_id) WHERE read_at IS NULL AND deleted_at IS NULL;
CREATE INDEX ix_notification_created ON notification (created_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_notification_delivery_notification ON notification_delivery (notification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_delivery_channel ON notification_delivery (notification_channel_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_delivery_status ON notification_delivery (notification_delivery_status_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_app_user_department ON app_user (department_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- BEFORE UPDATE triggers: maintain updated_at via ac_set_updated_at()
-- -----------------------------------------------------------------------------
CREATE TRIGGER tr_correspondence_type_updated_at BEFORE UPDATE ON correspondence_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_status_updated_at BEFORE UPDATE ON correspondence_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_priority_updated_at BEFORE UPDATE ON priority FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_confidentiality_updated_at BEFORE UPDATE ON confidentiality FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_action_type_updated_at BEFORE UPDATE ON workflow_action_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_channel_updated_at BEFORE UPDATE ON notification_channel FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_event_type_updated_at BEFORE UPDATE ON notification_event_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_content_type_updated_at BEFORE UPDATE ON attachment_content_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_instance_status_updated_at BEFORE UPDATE ON workflow_instance_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_delivery_status_updated_at BEFORE UPDATE ON notification_delivery_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_department_updated_at BEFORE UPDATE ON department FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_organization_updated_at BEFORE UPDATE ON organization FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_classification_updated_at BEFORE UPDATE ON classification FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_permission_updated_at BEFORE UPDATE ON permission FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_role_updated_at BEFORE UPDATE ON role FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_role_permission_updated_at BEFORE UPDATE ON role_permission FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_user_role_updated_at BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_correspondence_updated_at BEFORE UPDATE ON correspondence FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_recipient_updated_at BEFORE UPDATE ON correspondence_recipient FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_updated_at BEFORE UPDATE ON attachment FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_version_updated_at BEFORE UPDATE ON attachment_version FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_comment_updated_at BEFORE UPDATE ON correspondence_comment FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_delegation_updated_at BEFORE UPDATE ON delegation FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_template_updated_at BEFORE UPDATE ON template FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_retention_policy_updated_at BEFORE UPDATE ON retention_policy FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_audit_log_updated_at BEFORE UPDATE ON audit_log FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_instance_updated_at BEFORE UPDATE ON workflow_instance FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_action_updated_at BEFORE UPDATE ON workflow_action FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_notification_updated_at BEFORE UPDATE ON notification FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_delivery_updated_at BEFORE UPDATE ON notification_delivery FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- ---------- source: V7__workflow_history.sql ----------

-- =============================================================================
-- V7__workflow_history.sql
-- Dedicated timeline / SLA / audit trail (NOT a substitute for workflow_action).
-- workflow_action  = canonical step tied to Camunda task completion.
-- workflow_history = rich chronological feed (user + system + SLA + comments).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- workflow_history_event_type (lookup â€” no ENUM)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history_event_type (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_workflow_history_event_type_sort CHECK (sort_order >= 0)
);

COMMENT ON TABLE workflow_history_event_type IS 'Classifies timeline rows (user action, SLA tick, system event, etc.).';

-- -----------------------------------------------------------------------------
-- workflow_history (immutable-style timeline row; do not soft-delete â€” append corrections as new rows)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history (
  id                              BIGSERIAL PRIMARY KEY,
  correspondence_id               UUID        NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  workflow_instance_id            UUID REFERENCES workflow_instance (id) ON DELETE SET NULL,
  workflow_history_event_type_id  BIGINT      NOT NULL REFERENCES workflow_history_event_type (id) ON DELETE RESTRICT,
  workflow_action_type_id         BIGINT REFERENCES workflow_action_type (id) ON DELETE SET NULL,
  workflow_action_id              BIGINT REFERENCES workflow_action (id) ON DELETE SET NULL,
  actor_user_id                   UUID REFERENCES app_user (id) ON DELETE SET NULL,
  occurred_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
  sequence_no                     INTEGER     NOT NULL,
  primary_comment_text            TEXT,
  detail                          JSONB,
  sla_due_at                      TIMESTAMPTZ,
  sla_expected_at                 TIMESTAMPTZ,
  sla_breached_at                 TIMESTAMPTZ,
  actual_duration_ms              BIGINT,
  remaining_sla_ms                BIGINT,
  previous_correspondence_status_id BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  new_correspondence_status_id    BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  priority_id_at_event            BIGINT REFERENCES priority (id) ON DELETE SET NULL,
  camunda_task_id                 VARCHAR(64),
  camunda_activity_id             VARCHAR(128),
  source_system                   VARCHAR(64) NOT NULL DEFAULT 'AC_APP',
  created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                      UUID,
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                      UUID,
  CONSTRAINT ck_workflow_history_sequence_positive CHECK (sequence_no > 0),
  CONSTRAINT ck_workflow_history_duration CHECK (actual_duration_ms IS NULL OR actual_duration_ms >= 0),
  CONSTRAINT ck_workflow_history_remaining_sla CHECK (remaining_sla_ms IS NULL OR remaining_sla_ms >= 0)
);

COMMENT ON TABLE workflow_history IS 'Full timeline: human actions, status moves, SLA evaluation, integrations.';
COMMENT ON COLUMN workflow_history.sequence_no IS 'Monotonic per correspondence; allocate in application via counter/lock.';
COMMENT ON COLUMN workflow_history.primary_comment_text IS 'Main comment for this timeline point (distinct from threaded rows in workflow_history_comment).';
COMMENT ON COLUMN workflow_history.detail IS 'Structured audit payload (before/after, assignees, rule id, Camunda variables snapshot).';

CREATE UNIQUE INDEX uq_workflow_history_corr_sequence
  ON workflow_history (correspondence_id, sequence_no);

-- -----------------------------------------------------------------------------
-- workflow_history_comment (additional comments attached to one timeline entry)
-- -----------------------------------------------------------------------------
CREATE TABLE workflow_history_comment (
  id                   BIGSERIAL PRIMARY KEY,
  workflow_history_id  BIGINT NOT NULL REFERENCES workflow_history (id) ON DELETE CASCADE,
  author_user_id       UUID   NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
  body                 TEXT   NOT NULL,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by           UUID,
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by           UUID
);

COMMENT ON TABLE workflow_history_comment IS 'Threaded / supplemental comments for a single workflow_history row.';

-- -----------------------------------------------------------------------------
-- Seed: workflow_history_event_type
-- -----------------------------------------------------------------------------
INSERT INTO workflow_history_event_type (code, name_ar, name_en, sort_order) VALUES
  ('USER_ACTION',          'إجراء �&ستخد�&',        'User action',           10),
  ('SYSTEM_EVENT',         'حدث � ظا�&',            'System event',          20),
  ('STATUS_CHANGE',        'تغ�`�`ر حا�ة',          'Status change',         30),
  ('SLA_MILESTONE',        '�&ع��& SLA',            'SLA milestone',         40),
  ('SLA_BREACH',           'تجا��ز SLA',           'SLA breach',            50),
  ('TASK_ASSIGNED',        'تع�`�`�  �&�!�&ة',          'Task assigned',         60),
  ('TASK_COMPLETED',       'إْ�&ا� �&�!�&ة',          'Task completed',        70),
  ('COMMENT',              'تع��`�',               'Comment',               80),
  ('DELEGATION',           'تف���`ض',               'Delegation',            90),
  ('ESCALATION',           'تصع�`د',               'Escalation',            100),
  ('CAMUNDA_TRANSITION',   'ا� ت�ا� س�`ر ع�&�',      'Workflow transition',   110),
  ('ATTACHMENT',           '�&رف�',                'Attachment',            120),
  ('CORRESPONDENCE_LINK',  'ارتباط �&عا�&�ة',       'Correspondence link',   130);

-- -----------------------------------------------------------------------------
-- Foreign keys: audit columns â†’ app_user
-- -----------------------------------------------------------------------------
ALTER TABLE workflow_history_event_type
  ADD CONSTRAINT fk_wf_hist_evt_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_wf_hist_evt_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_wf_hist_evt_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_history
  ADD CONSTRAINT fk_workflow_history_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_history_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_history_comment
  ADD CONSTRAINT fk_workflow_history_comment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_history_comment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- Indexes (timeline & SLA dashboards)
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_workflow_history_event_type_code_active
  ON workflow_history_event_type (code) WHERE deleted_at IS NULL;

CREATE INDEX ix_workflow_history_correspondence_time
  ON workflow_history (correspondence_id, occurred_at DESC);

CREATE INDEX ix_workflow_history_correspondence_seq
  ON workflow_history (correspondence_id, sequence_no);

CREATE INDEX ix_workflow_history_instance
  ON workflow_history (workflow_instance_id) WHERE workflow_instance_id IS NOT NULL;

CREATE INDEX ix_workflow_history_actor
  ON workflow_history (actor_user_id) WHERE actor_user_id IS NOT NULL;

CREATE INDEX ix_workflow_history_event_type
  ON workflow_history (workflow_history_event_type_id);

CREATE INDEX ix_workflow_history_sla_breach
  ON workflow_history (sla_breached_at) WHERE sla_breached_at IS NOT NULL;

CREATE INDEX ix_workflow_history_action_link
  ON workflow_history (workflow_action_id) WHERE workflow_action_id IS NOT NULL;

CREATE INDEX ix_workflow_history_comment_parent
  ON workflow_history_comment (workflow_history_id);

CREATE INDEX ix_workflow_history_comment_author
  ON workflow_history_comment (author_user_id);

-- -----------------------------------------------------------------------------
-- updated_at triggers
-- -----------------------------------------------------------------------------
CREATE TRIGGER tr_workflow_history_event_type_updated_at
  BEFORE UPDATE ON workflow_history_event_type
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_history_updated_at
  BEFORE UPDATE ON workflow_history
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_history_comment_updated_at
  BEFORE UPDATE ON workflow_history_comment
  FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- ---------- source: V8__correspondence_create_support.sql ----------

-- =============================================================================
-- V8__correspondence_create_support.sql
-- Monotonic reference numbers + explicit CREATE action/event for workflow_history.
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS correspondence_reference_seq AS BIGINT START WITH 1 INCREMENT BY 1;

COMMENT ON SEQUENCE correspondence_reference_seq IS 'Application allocates reference_number via nextval in a transaction.';

-- Timeline classification for correspondence creation (distinct from generic USER_ACTION).
INSERT INTO workflow_history_event_type (code, name_ar, name_en, sort_order) VALUES
  ('CREATE', 'إ� شاء �&عا�&�ة', 'Correspondence created', 5);

-- Canonical workflow action code for "create" (API / business semantics).
INSERT INTO workflow_action_type (code, name_ar, name_en, sort_order) VALUES
  ('CREATE', 'إ� شاء', 'Create', 5);

-- ---------- source: V9__notification_i18n_keys.sql ----------

-- =============================================================================
-- V9__notification_i18n_keys.sql
-- i18n: store UI message keys + params; titles optional for in-app inbox.
-- =============================================================================

ALTER TABLE notification
  ADD COLUMN message_key VARCHAR(256),
  ADD COLUMN message_params JSONB;

ALTER TABLE notification ALTER COLUMN title_ar DROP NOT NULL;
ALTER TABLE notification ALTER COLUMN title_en DROP NOT NULL;

COMMENT ON COLUMN notification.message_key IS 'Frontend i18n key; human text resolved in UI.';
COMMENT ON COLUMN notification.message_params IS 'Interpolation payload for the message key (JSON object).';

-- ---------- source: V10__admin_login_user.sql ----------

-- Dev / bootstrap human login (change password in non-dev environments).
INSERT INTO app_user (
  id, username, password_hash, full_name_ar, full_name_en, email, department_id, is_active
)
SELECT
  'b0000001-0000-4000-8000-000000000001',
  'admin',
  '{noop}admin',
  '�&د�`ر ا�� ظا�&',
  'System administrator',
  'admin@local.invalid',
  1,
  TRUE
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'admin' AND deleted_at IS NULL);

INSERT INTO user_role (app_user_id, role_id)
SELECT 'b0000001-0000-4000-8000-000000000001', r.id
FROM role r
WHERE r.code = 'SYS_ADMIN'
  AND EXISTS (SELECT 1 FROM app_user u WHERE u.id = 'b0000001-0000-4000-8000-000000000001')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur
    WHERE ur.app_user_id = 'b0000001-0000-4000-8000-000000000001' AND ur.role_id = r.id
  );

-- ---------- source: V11__correspondence_letter_template.sql ----------

-- Letter templates for correspondence editor (loaded by API; replaces hardcoded frontend HTML).

CREATE TABLE correspondence_letter_template (
  id              BIGSERIAL PRIMARY KEY,
  code            VARCHAR(64)  NOT NULL,
  name_ar         VARCHAR(250) NOT NULL,
  name_en         VARCHAR(250) NOT NULL,
  body_html       TEXT         NOT NULL DEFAULT '',
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_correspondence_letter_template_sort CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX ux_correspondence_letter_template_code_active
  ON correspondence_letter_template (code) WHERE deleted_at IS NULL;

COMMENT ON TABLE correspondence_letter_template IS 'Rich-text letter bodies for create/reply flows; keyed by stable code.';

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order) VALUES
  ('default', 'خطاب رس�&�` عا�&', 'Standard official letter',
   '<p><strong>إ��0:</strong> ⬦</p><p><strong>ا��&��ض��ع:</strong> ⬦</p><p>ا�س�ا�& ع��`ْ�& ��رح�&ة ا���! ��برْات�!�R ��بعد:</p><p>� ص ا�خطاب⬦</p>', 10),
  ('reminder', 'خطاب تذْ�`ر', 'Reminder letter',
   '<p><strong>إ��0:</strong> ⬦</p><p><strong>ا��&��ض��ع:</strong> تذْ�`ر</p><p>� ��د تذْ�`رْ�& با�إفادة⬦</p>', 20),
  ('approval', 'خطاب �&��اف�ة', 'Approval letter',
   '<p><strong>إ��0:</strong> ⬦</p><p><strong>ا��&��ض��ع:</strong> إفادة با��&��اف�ة</p><p>� ف�`دْ�& با��&��اف�ة⬦</p>', 30),
  ('rejection', 'خطاب اعتذار / رفض', 'Rejection letter',
   '<p><strong>إ��0:</strong> ⬦</p><p><strong>ا��&��ض��ع:</strong> اعتذار</p><p>� عتذر ع�  عد�& إ�&ْا� �`ة ا��&��اف�ة⬦</p>', 40),
  ('admin-circular', 'تع�&�`�& إدار�`', 'Administrative circular',
   '<p style="text-align:center"><strong>تع�&�`�& إدار�`</strong></p><p>� ص ا�تع�&�`�&⬦</p>', 50),
  ('ministerial-circular', 'تع�&�`�& ��زار�`', 'Ministerial circular',
   '<p style="text-align:center"><strong>تع�&�`�& ��زار�`</strong></p><p>� ص ا�تع�&�`�&⬦</p>', 60),
  ('no-letter', 'بد���  خطاب', 'No letter', '', 70);

-- ---------- source: V12__role_switch_audit.sql ----------

-- Audit trail for JWT active-role switches (enterprise accountability).
CREATE TABLE IF NOT EXISTS role_switch_audit (
    id BIGSERIAL PRIMARY KEY,
    app_user_id UUID NOT NULL REFERENCES app_user (id),
    old_role_code VARCHAR(100),
    new_role_code VARCHAR(100) NOT NULL,
    switched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_role_switch_audit_user ON role_switch_audit (app_user_id);
CREATE INDEX IF NOT EXISTS idx_role_switch_audit_time ON role_switch_audit (switched_at DESC);

-- ---------- source: V13__ui_screen_and_system_issue.sql ----------

-- UI screen registry for RBAC documentation / future route guards (admin CRUD).
CREATE TABLE IF NOT EXISTS ui_screen (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(128)  NOT NULL,
    route_path      VARCHAR(512)  NOT NULL,
    name_ar         VARCHAR(200)  NOT NULL,
    name_en         VARCHAR(200)  NOT NULL,
    description     TEXT,
    sort_order      INTEGER       NOT NULL DEFAULT 0,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      UUID,
    CONSTRAINT uq_ui_screen_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS ix_ui_screen_route ON ui_screen (route_path) WHERE deleted_at IS NULL;

COMMENT ON TABLE ui_screen IS 'Register of application screens/routes for administration (SRS RBAC / UI inventory).';

-- Client and server captured problems for admin triage.
CREATE TABLE IF NOT EXISTS system_issue (
    id              BIGSERIAL PRIMARY KEY,
    source          VARCHAR(16)   NOT NULL,
    severity        VARCHAR(16)   NOT NULL DEFAULT 'ERROR',
    message         TEXT          NOT NULL,
    detail          TEXT,
    page_url        VARCHAR(2000),
    user_id         UUID          REFERENCES app_user (id) ON DELETE SET NULL,
    http_status     INTEGER,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    resolved_by     UUID          REFERENCES app_user (id) ON DELETE SET NULL,
    resolution_note TEXT,
    CONSTRAINT ck_system_issue_source CHECK (source IN ('CLIENT', 'SERVER'))
);

CREATE INDEX IF NOT EXISTS ix_system_issue_created ON system_issue (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_system_issue_open ON system_issue (created_at DESC) WHERE resolved_at IS NULL;

INSERT INTO ui_screen (code, route_path, name_ar, name_en, sort_order)
VALUES
    ('dashboard', '/dashboard', '���حة ا�تحْ�&', 'Dashboard', 10),
    ('transactions', '/transactions', 'إدارة ا��&عا�&�ات', 'Transactions', 20),
    ('create_transaction', '/create-transaction', 'إ� شاء �&عا�&�ة', 'Create transaction', 30),
    ('reports', '/reports', 'ا�ت�ار�`ر', 'Reports', 40),
    ('notifications', '/notifications', 'ا�إشعارات', 'Notifications', 50),
    ('admin_main', '/admin-communications-main', 'إدارة ا�� ظا�&', 'System administration', 60)
ON CONFLICT (code) DO NOTHING;

-- ---------- source: V14__auth_refresh_mfa_circular_audit.sql ----------

-- Auth: MFA flag, refresh tokens, OTP challenges
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    jti UUID NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_token (expires_at);

CREATE TABLE IF NOT EXISTS mfa_otp_challenge (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    channel VARCHAR(32) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mfa_otp_user ON mfa_otp_challenge (user_id);

-- Communication: circulars (multi-recipient + broadcast)
CREATE TABLE IF NOT EXISTS circular (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    is_broadcast BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS circular_recipient (
    circular_id UUID NOT NULL REFERENCES circular (id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    read_at TIMESTAMPTZ,
    PRIMARY KEY (circular_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_circular_created_at ON circular (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_circular_recipient_user ON circular_recipient (user_id);

-- Audit: generic user-action trail (in addition to role_switch_audit)
CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id VARCHAR(255) NOT NULL,
    action_code VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128),
    resource_id VARCHAR(255),
    detail_json TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_audit_event_actor_time ON audit_event (actor_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_action ON audit_event (action_code, occurred_at DESC);

-- ---------- source: V15__cancelled_status_reply_draft.sql ----------

-- Cancelled lifecycle + editor draft storage for replies
INSERT INTO correspondence_status (correspondence_type_id, code, name_ar, name_en, sort_order, is_terminal)
SELECT NULL, 'CANCELLED', '�&�غاة', 'Cancelled', 95, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM correspondence_status cs
    WHERE UPPER(cs.code) = 'CANCELLED' AND cs.deleted_at IS NULL
);

ALTER TABLE correspondence
    ADD COLUMN IF NOT EXISTS reply_draft_html TEXT;

COMMENT ON COLUMN correspondence.reply_draft_html IS 'Unpublished reply/editor HTML; cleared when reply is sent.';

-- ---------- source: V16__department_hierarchy_seed.sql ----------

-- Sample organizational hierarchy under ROOT for department tree UI (demo / dev alignment with legacy app).

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_HR',
       'ا�إدارة ا�عا�&ة ���&��ارد ا�بشر�`ة',
       'General Directorate of Human Resources',
       d.id,
       10
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_HR' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_IT',
       'ا�إدارة ا�عا�&ة �ت�� �`ة ا��&ع����&ات',
       'General Directorate of Information Technology',
       d.id,
       20
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_IT' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_FIN',
       'ا�إدارة ا�عا�&ة ��شؤ���  ا��&ا��`ة',
       'General Directorate of Financial Affairs',
       d.id,
       30
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_FIN' AND x.deleted_at IS NULL)
LIMIT 1;

-- HR children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'HR_RECRUIT',
       '�س�& ا�ت��ظ�`ف',
       'Recruitment',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_HR'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'HR_RECRUIT' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'HR_TRAIN',
       '�س�& ا�تدر�`ب ��ا�تط���`ر',
       'Training and Development',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_HR'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'HR_TRAIN' AND x.deleted_at IS NULL)
LIMIT 1;

-- IT children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'IT_INFRA',
       '�س�& ا�ب� �`ة ا�تحت�`ة',
       'Infrastructure',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_IT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'IT_INFRA' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'IT_CYBER',
       '�س�& ا�أ�&�  ا�س�`برا� �`',
       'Cybersecurity',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_IT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'IT_CYBER' AND x.deleted_at IS NULL)
LIMIT 1;

-- Finance children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'FIN_BUDGET',
       '�س�& ا��&�`زا� �`ة',
       'Budget',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_FIN'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'FIN_BUDGET' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'FIN_ACCT',
       '�س�& ا�حسابات',
       'Accounts',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_FIN'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'FIN_ACCT' AND x.deleted_at IS NULL)
LIMIT 1;

-- ---------- V17 superseded: baseline creates all objects under srs_system ----------
COMMENT ON SCHEMA srs_system IS 'Dedicated application schema for the SRS administrative communications platform.';


-- ---------- source: V18__ui_screen_seed_refresh.sql ----------

INSERT INTO srs_system.ui_screen (
    code,
    route_path,
    name_ar,
    name_en,
    description,
    sort_order,
    is_active
)
VALUES
    ('login', '/login', 'تسج�`� ا�دخ���', 'Login', 'Authentication entry screen.', 5, TRUE),
    ('dashboard', '/dashboard', '���حة ا�تحْ�&', 'Dashboard', 'Operational summary and KPIs.', 10, TRUE),
    ('transactions', '/transactions', 'ا��&عا�&�ات', 'Transactions', 'Primary correspondence registry.', 20, TRUE),
    ('transactions_list', '/transactions/list/:type', '�ائ�&ة ا��&عا�&�ات', 'Transactions list', 'Filtered list by correspondence type.', 25, TRUE),
    ('create_transaction', '/create-transaction', 'إ� شاء �&عا�&�ة', 'Create transaction', 'Create a new correspondence item.', 30, TRUE),
    ('transaction_details', '/transactions/:id', 'تفاص�`� ا��&عا�&�ة', 'Transaction details', 'Read and act on a single correspondence.', 35, TRUE),
    ('notifications', '/notifications', 'ا�إشعارات', 'Notifications', 'User notifications and alerts.', 40, TRUE),
    ('circulars', '/circulars', 'ا�تعا�&�`�&', 'Circulars', 'Circular inbox and acknowledgements.', 45, TRUE),
    ('reports', '/reports', 'ا�ت�ار�`ر', 'Reports', 'Operational and management reports.', 50, TRUE),
    ('admin_main', '/admin-communications-main', 'إدارة ا�� ظا�&', 'System administration', 'Administrative console and system setup.', 60, TRUE),
    ('users', '/users', 'ا��&ستخد�&��� ', 'Users', 'User administration and assignments.', 70, TRUE),
    ('roles', '/roles', 'ا�أد��ار ��ا�ص�اح�`ات', 'Roles and permissions', 'Role and permission administration.', 80, TRUE),
    ('profile', '/profile', 'ا��&�ف ا�شخص�`', 'Profile', 'Current user profile and preferences.', 90, TRUE)
ON CONFLICT (code) DO UPDATE
SET route_path  = EXCLUDED.route_path,
    name_ar     = EXCLUDED.name_ar,
    name_en     = EXCLUDED.name_en,
    description = EXCLUDED.description,
    sort_order  = EXCLUDED.sort_order,
    is_active   = EXCLUDED.is_active,
    updated_at  = now();

-- ---------- source: V19__app_user_ui_preferences.sql ----------

-- UI preferences persisted per user (language + light/dark theme), synced with the Angular shell.

ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS ui_theme VARCHAR(16) NOT NULL DEFAULT 'light',
  ADD COLUMN IF NOT EXISTS ui_locale VARCHAR(8) NOT NULL DEFAULT 'ar';

COMMENT ON COLUMN app_user.ui_theme IS 'light | dark â€” matches ThemeService on the frontend.';
COMMENT ON COLUMN app_user.ui_locale IS 'ar | en â€” matches I18nService language.';

ALTER TABLE app_user
  ADD CONSTRAINT ck_app_user_ui_theme CHECK (ui_theme IN ('light', 'dark'));

ALTER TABLE app_user
  ADD CONSTRAINT ck_app_user_ui_locale CHECK (ui_locale IN ('ar', 'en'));

-- ---------- source: V20__ui_screen_shell_nav.sql ----------

-- Shell sidebar: optional required permission, Material icon key, flag for main nav visibility.

ALTER TABLE ui_screen
  ADD COLUMN IF NOT EXISTS required_permission_id BIGINT REFERENCES permission (id) ON DELETE SET NULL;

ALTER TABLE ui_screen
  ADD COLUMN IF NOT EXISTS icon_key VARCHAR(64) NOT NULL DEFAULT 'apps';

ALTER TABLE ui_screen
  ADD COLUMN IF NOT EXISTS show_in_shell_nav BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN ui_screen.required_permission_id IS 'If set, the user''s active role must include this permission to see the item in the shell sidebar.';
COMMENT ON COLUMN ui_screen.icon_key IS 'Material icon ligature name served to the Angular shell (e.g. dashboard, description).';
COMMENT ON COLUMN ui_screen.show_in_shell_nav IS 'When TRUE, row is eligible for GET /api/v1/profile/me/navigation (still filtered by permission).';

-- Seed nav metadata (names/routes remain authoritative from ui_screen rows).
UPDATE ui_screen SET show_in_shell_nav = FALSE WHERE deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'dashboard',
    show_in_shell_nav = TRUE,
    required_permission_id = NULL
WHERE code = 'dashboard' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'description',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'correspondence.view' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'transactions' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'mark_email_unread',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'correspondence.view' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'circulars' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'group',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'user.manage' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'users' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'admin_panel_settings',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'role.manage' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'roles' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'tune',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'lookup.manage' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'admin_main' AND deleted_at IS NULL;

UPDATE ui_screen
SET icon_key = 'bar_chart',
    show_in_shell_nav = TRUE,
    required_permission_id = (SELECT id FROM permission WHERE code = 'report.view' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE code = 'reports' AND deleted_at IS NULL;

-- ---------- source: V21__workflow_routes_leave_beneficiary_letter_path.sql ----------

-- Workflow routes per correspondence type (Camunda process_definition_key), beneficiary/supply flags,
-- leave requests, optional letter template file path, and new permissions.

-- -----------------------------------------------------------------------------
-- service_workflow_route: selectable Camunda process per correspondence type
-- -----------------------------------------------------------------------------
CREATE TABLE service_workflow_route (
  id                       BIGSERIAL PRIMARY KEY,
  correspondence_type_id   BIGINT       NOT NULL REFERENCES correspondence_type (id) ON DELETE RESTRICT,
  process_definition_key     VARCHAR(128) NOT NULL,
  name_ar                  VARCHAR(250) NOT NULL,
  name_en                  VARCHAR(250) NOT NULL,
  is_default_route         BOOLEAN      NOT NULL DEFAULT FALSE,
  sort_order               INTEGER      NOT NULL DEFAULT 0,
  is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at               TIMESTAMPTZ,
  deleted_by               UUID,
  created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by               UUID,
  updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by               UUID,
  CONSTRAINT ck_service_workflow_route_sort CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX ux_service_workflow_route_default_per_type
  ON service_workflow_route (correspondence_type_id)
  WHERE is_default_route = TRUE AND deleted_at IS NULL;

CREATE INDEX ix_service_workflow_route_type
  ON service_workflow_route (correspondence_type_id)
  WHERE deleted_at IS NULL;

COMMENT ON TABLE service_workflow_route IS 'Camunda process keys offered per correspondence type; one default per type for AUTO mode.';

-- Seed defaults (aligns with CorrespondenceProcessDefinitionKeys.forCorrespondenceTypeCode)
INSERT INTO service_workflow_route (correspondence_type_id, process_definition_key, name_ar, name_en, is_default_route, sort_order)
SELECT ct.id, 'inbound-correspondence', '�&سار ��ارد � Camunda', 'Inbound Camunda workflow', TRUE, 10
FROM correspondence_type ct WHERE ct.code = 'INBOUND' AND ct.deleted_at IS NULL;

INSERT INTO service_workflow_route (correspondence_type_id, process_definition_key, name_ar, name_en, is_default_route, sort_order)
SELECT ct.id, 'outbound-correspondence', '�&سار صادر � Camunda', 'Outbound Camunda workflow', TRUE, 10
FROM correspondence_type ct WHERE ct.code = 'OUTBOUND' AND ct.deleted_at IS NULL;

INSERT INTO service_workflow_route (correspondence_type_id, process_definition_key, name_ar, name_en, is_default_route, sort_order)
SELECT ct.id, 'internal-correspondence', '�&سار داخ��` � Camunda', 'Internal Camunda workflow', TRUE, 10
FROM correspondence_type ct WHERE ct.code = 'INTERNAL' AND ct.deleted_at IS NULL;

-- Non-matching types use internal process (same as Java default branch)
INSERT INTO service_workflow_route (correspondence_type_id, process_definition_key, name_ar, name_en, is_default_route, sort_order)
SELECT ct.id, 'internal-correspondence', '�&سار داخ��` � Camunda', 'Internal Camunda workflow', TRUE, 10
FROM correspondence_type ct
WHERE ct.code IN ('EXTERNAL', 'CIRCULAR', 'DECISION') AND ct.deleted_at IS NULL;

-- Optional second inbound route (manual choice demo): duplicate key not allowed as second row with different label only if same key â€” skip extra row or use same process with different name only once.
-- Add alternate label-only route would need different BPMN; omitted.

-- -----------------------------------------------------------------------------
-- correspondence: workflow selection, supply/beneficiary
-- -----------------------------------------------------------------------------
ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS workflow_route_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO';

ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS service_workflow_route_id BIGINT REFERENCES service_workflow_route (id);

ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS supply_transaction BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS beneficiary_name VARCHAR(500);

ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS beneficiary_organization VARCHAR(500);

ALTER TABLE correspondence
  ADD COLUMN IF NOT EXISTS beneficiary_identifier VARCHAR(128);

COMMENT ON COLUMN correspondence.workflow_route_mode IS 'AUTO: use default route for type; MANUAL: use service_workflow_route_id.';
COMMENT ON COLUMN correspondence.supply_transaction IS 'True when created from «ت��ر�`د �&عا�&�ة» (supply) screen.';

-- -----------------------------------------------------------------------------
-- leave_request
-- -----------------------------------------------------------------------------
CREATE TABLE leave_request (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  reason        TEXT,
  status_code   VARCHAR(64) NOT NULL DEFAULT 'PENDING',
  decided_by    UUID REFERENCES app_user (id) ON DELETE SET NULL,
  decided_at    TIMESTAMPTZ,
  decision_note TEXT,
  deleted_at    TIMESTAMPTZ,
  deleted_by    UUID,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    UUID,
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by    UUID,
  CONSTRAINT ck_leave_request_dates CHECK (end_date >= start_date)
);

CREATE INDEX ix_leave_request_user ON leave_request (user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_leave_request_status ON leave_request (status_code) WHERE deleted_at IS NULL;

COMMENT ON TABLE leave_request IS 'Employee leave / vacation requests (status workflow can be extended later).';

-- -----------------------------------------------------------------------------
-- letter template optional file path (under ac.storage.root)
-- -----------------------------------------------------------------------------
ALTER TABLE correspondence_letter_template
  ADD COLUMN IF NOT EXISTS template_file_path VARCHAR(500);

COMMENT ON COLUMN correspondence_letter_template.template_file_path IS 'Optional relative path under storage root; when set, API may load HTML from disk instead of body_html.';

-- -----------------------------------------------------------------------------
-- Permissions + role grants
-- -----------------------------------------------------------------------------
INSERT INTO permission (code, name_ar, name_en, sort_order, is_active)
SELECT 'leave.self', 'ت�د�`�& ط�ب إجازة', 'Submit leave request', 210, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'leave.self' AND p.deleted_at IS NULL);

INSERT INTO permission (code, name_ar, name_en, sort_order, is_active)
SELECT 'leave.admin', 'إدارة ط�بات ا�إجازات', 'Manage leave requests', 211, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'leave.admin' AND p.deleted_at IS NULL);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'leave.self'
WHERE r.code IN ('STAFF', 'CORRESP_CLERK', 'DEPT_MANAGER', 'APPROVER', 'CORRESP_MGR', 'SYS_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'leave.admin'
WHERE r.code IN ('SYS_ADMIN', 'CORRESP_MGR', 'DEPT_MANAGER')
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Shell nav entries (profile/me/navigation)
INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
VALUES
  ('org_structure', '/org-structure', 'ا��!�`ْ� ا�ت� ظ�`�&�`', 'Organization structure', 'Department hierarchy from database.', 22, TRUE, 'account_tree', TRUE,
   (SELECT id FROM permission WHERE code = 'correspondence.view' AND deleted_at IS NULL ORDER BY id LIMIT 1)),
  ('leave_requests', '/leave-requests', 'ا�إجازات', 'Leave requests', 'Leave and vacation requests.', 23, TRUE, 'event_available', TRUE,
   (SELECT id FROM permission WHERE code = 'leave.self' AND deleted_at IS NULL ORDER BY id LIMIT 1)),
  ('supply_transaction', '/supply-transaction', 'ت��ر�`د �&عا�&�ة', 'Supply transaction', 'Supply / register with beneficiary.', 28, TRUE, 'inventory_2', TRUE,
   (SELECT id FROM permission WHERE code = 'correspondence.create' AND deleted_at IS NULL ORDER BY id LIMIT 1))
ON CONFLICT (code) DO UPDATE SET
  route_path = EXCLUDED.route_path,
  name_ar = EXCLUDED.name_ar,
  name_en = EXCLUDED.name_en,
  description = EXCLUDED.description,
  sort_order = EXCLUDED.sort_order,
  icon_key = EXCLUDED.icon_key,
  show_in_shell_nav = EXCLUDED.show_in_shell_nav,
  required_permission_id = EXCLUDED.required_permission_id,
  updated_at = now();

-- ---------- source: V22__guide_delegation_links_indexing.sql ----------

-- Extensions aligned with user guide §9 (delegation, links, non-archived media, attachment indexing, search/nav).

-- -----------------------------------------------------------------------------
-- 9. Authority delegation (administrative — not Camunda task delegate)
-- -----------------------------------------------------------------------------
CREATE TABLE authority_delegation (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  delegator_user_id         UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  delegate_user_id          UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
  valid_from                DATE NOT NULL,
  valid_to                  DATE NOT NULL,
  allowed_correspondence_type_codes TEXT,
  allowed_confidentiality_codes     TEXT,
  can_sign_on_behalf        BOOLEAN NOT NULL DEFAULT FALSE,
  notes                     TEXT,
  deleted_at                TIMESTAMPTZ,
  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                UUID,
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                UUID,
  CONSTRAINT ck_authority_delegation_dates CHECK (valid_to >= valid_from),
  CONSTRAINT ck_authority_delegation_users CHECK (delegator_user_id <> delegate_user_id)
);

CREATE INDEX ix_authority_delegation_delegator ON authority_delegation (delegator_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_authority_delegation_delegate ON authority_delegation (delegate_user_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE authority_delegation IS 'Administrative delegation of correspondence actions (guide §9); comma-separated type/confidentiality codes, empty = all allowed.';

-- -----------------------------------------------------------------------------
-- Correspondence links (guide: linking correspondence)
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_link (
  id                        BIGSERIAL PRIMARY KEY,
  correspondence_id         UUID NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  linked_correspondence_id  UUID NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  link_kind                 VARCHAR(32) NOT NULL DEFAULT 'RELATED',
  notes                     TEXT,
  deleted_at                TIMESTAMPTZ,
  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                UUID,
  CONSTRAINT ck_correspondence_link_neq CHECK (correspondence_id <> linked_correspondence_id)
);

CREATE UNIQUE INDEX ux_correspondence_link_pair
  ON correspondence_link (correspondence_id, linked_correspondence_id)
  WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Non-archived items (disk, book, …) — guide non-archived correspondence
-- -----------------------------------------------------------------------------
CREATE TABLE correspondence_nonarchived_item (
  id                 BIGSERIAL PRIMARY KEY,
  correspondence_id  UUID NOT NULL REFERENCES correspondence (id) ON DELETE CASCADE,
  item_type          VARCHAR(128) NOT NULL,
  description_text   TEXT,
  quantity           INTEGER NOT NULL DEFAULT 1 CONSTRAINT ck_nonarchived_qty CHECK (quantity >= 1),
  sort_order         INTEGER NOT NULL DEFAULT 0,
  deleted_at         TIMESTAMPTZ,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by         UUID
);

CREATE INDEX ix_nonarchived_corr ON correspondence_nonarchived_item (correspondence_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Attachment index (pages / subject) — guide attachment indexing
-- -----------------------------------------------------------------------------
CREATE TABLE attachment_index_entry (
  id            BIGSERIAL PRIMARY KEY,
  attachment_id BIGINT NOT NULL REFERENCES attachment (id) ON DELETE CASCADE,
  page_from     INTEGER,
  page_to       INTEGER,
  subject_text  TEXT,
  sort_order    INTEGER NOT NULL DEFAULT 0,
  deleted_at    TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by    UUID
);

CREATE INDEX ix_attachment_index_attachment ON attachment_index_entry (attachment_id) WHERE deleted_at IS NULL;

-- Permission: optional fine-grained delegation admin
INSERT INTO permission (code, name_ar, name_en, sort_order, is_active)
SELECT 'delegation.manage', 'إدارة ا�تف���`ض ا�إدار�`', 'Manage authority delegations', 220, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'delegation.manage' AND p.deleted_at IS NULL);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p ON p.code = 'delegation.manage'
WHERE r.code IN ('SYS_ADMIN', 'CORRESP_MGR')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Shell navigation
INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
VALUES
  ('correspondence_search', '/correspondence-search', 'بحث ا��&عا�&�ات', 'Correspondence search', 'Advanced search (guide §10).', 26, TRUE, 'search', TRUE,
   (SELECT id FROM permission WHERE code = 'correspondence.view' AND deleted_at IS NULL ORDER BY id LIMIT 1)),
  ('delegations', '/delegations', 'ا�تف���`ضات ا�إدار�`ة', 'Authority delegations', 'Delegate inbox actions (guide §9).', 27, TRUE, 'switch_account', TRUE,
   (SELECT id FROM permission WHERE code = 'correspondence.view' AND deleted_at IS NULL ORDER BY id LIMIT 1))
ON CONFLICT (code) DO UPDATE SET
  route_path = EXCLUDED.route_path,
  name_ar = EXCLUDED.name_ar,
  name_en = EXCLUDED.name_en,
  description = EXCLUDED.description,
  sort_order = EXCLUDED.sort_order,
  icon_key = EXCLUDED.icon_key,
  show_in_shell_nav = EXCLUDED.show_in_shell_nav,
  required_permission_id = EXCLUDED.required_permission_id,
  updated_at = now();

-- ---------- source: V23__lookup_catalog.sql ----------

-- Registry of manageable lookup groups (codes align with API path segments and Angular bundle keys).
-- parent_lookup_code: which *other* lookup group is the FK parent for rows (e.g. correspondence_status â†’ correspondence_type).

CREATE TABLE lookup_catalog (
  lookup_code          VARCHAR(64) PRIMARY KEY,
  name_ar              VARCHAR(200) NOT NULL,
  name_en              VARCHAR(200) NOT NULL,
  parent_lookup_code   VARCHAR(64),
  sort_order           INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT fk_lookup_catalog_parent FOREIGN KEY (parent_lookup_code)
    REFERENCES lookup_catalog (lookup_code) ON DELETE SET NULL
);

COMMENT ON TABLE lookup_catalog IS 'Metadata for lookup admin UI: stable lookup_code per physical table.';

INSERT INTO lookup_catalog (lookup_code, name_ar, name_en, parent_lookup_code, sort_order) VALUES
  ('correspondence_type', '� ��ع ا��&عا�&�ة', 'Correspondence type', NULL, 10),
  ('correspondence_status', 'حا�ة ا��&عا�&�ة', 'Correspondence status', 'correspondence_type', 20),
  ('priority', 'ا�أ������`ة', 'Priority', NULL, 30),
  ('confidentiality', 'درجة ا�سر�`ة', 'Confidentiality', NULL, 40),
  ('classification', 'ا�تص� �`ف', 'Classification', NULL, 50),
  ('workflow_action_type', '� ��ع إجراء ا��&سار', 'Workflow action type', NULL, 60),
  ('workflow_history_event_type', '� ��ع حدث ا�سج�', 'Workflow history event type', NULL, 70);

COMMENT ON COLUMN lookup_catalog.parent_lookup_code IS 'If set, row.parent_id references the *id* space of that lookup group (e.g. correspondence_status.parent â†’ correspondence_type.id). NULL for flat lists; classification uses parent_id within the same table (tree).';

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
VALUES (
  'lookup_admin',
  '/lookup-admin',
  'إدارة ���ائ�& ا�� ظا�&',
  'Lookup management',
  'Manage lookup tables (types, statuses, priorities, â€¦).',
  15,
  TRUE,
  'list_alt',
  TRUE,
  (SELECT id FROM permission WHERE code = 'lookup.manage' AND deleted_at IS NULL ORDER BY id LIMIT 1)
)
ON CONFLICT (code) DO UPDATE SET
  route_path = EXCLUDED.route_path,
  name_ar = EXCLUDED.name_ar,
  name_en = EXCLUDED.name_en,
  description = EXCLUDED.description,
  sort_order = EXCLUDED.sort_order,
  icon_key = EXCLUDED.icon_key,
  show_in_shell_nav = EXCLUDED.show_in_shell_nav,
  required_permission_id = EXCLUDED.required_permission_id,
  updated_at = now();

-- ---------- source: V24__audit_fks_authority_delegation_deleted_by.sql ----------

-- Align V22 tables with app_user audit FKs and SoftDeletableEntity.deleted_by on authority_delegation.

ALTER TABLE authority_delegation
  ADD COLUMN IF NOT EXISTS deleted_by UUID;

ALTER TABLE authority_delegation
  ADD CONSTRAINT fk_authority_delegation_created_by
    FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_authority_delegation_updated_by
    FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_authority_delegation_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_link
  ADD CONSTRAINT fk_correspondence_link_created_by
    FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_nonarchived_item
  ADD CONSTRAINT fk_correspondence_nonarchived_created_by
    FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment_index_entry
  ADD CONSTRAINT fk_attachment_index_created_by
    FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL;

DROP TRIGGER IF EXISTS tr_authority_delegation_updated_at ON authority_delegation;
CREATE TRIGGER tr_authority_delegation_updated_at
  BEFORE UPDATE ON authority_delegation FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- ---------- source: V25__org_visual_node_status.sql ----------

-- Visual workflow org-chart node states (codes drive CSS class names on the frontend).
CREATE TABLE org_visual_node_status (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(64)  NOT NULL,
  name_ar     VARCHAR(200) NOT NULL,
  name_en     VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order  INTEGER      NOT NULL DEFAULT 0,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at  TIMESTAMPTZ,
  deleted_by  UUID,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID,
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by  UUID,
  CONSTRAINT ck_org_visual_node_status_sort_non_negative CHECK (sort_order >= 0)
);

COMMENT ON TABLE org_visual_node_status IS 'Org-chart visual node status codes (timeline/root styling); not correspondence lifecycle.';

CREATE UNIQUE INDEX ux_org_visual_node_status_code_active
  ON org_visual_node_status (code) WHERE deleted_at IS NULL;

INSERT INTO lookup_catalog (lookup_code, name_ar, name_en, parent_lookup_code, sort_order) VALUES
  ('org_visual_node_status', 'حا�ة ع�دة ا��&سار ا�بصر�`', 'Visual workflow node status', NULL, 75);

INSERT INTO org_visual_node_status (code, name_ar, name_en, sort_order) VALUES
  ('done',    '�&ْت�&�',     'Completed', 10),
  ('active',  'جار�`',      'In progress', 20),
  ('pending', '�&ع��',      'Pending', 30),
  ('info',    '�&ع����&ات',   'Info', 40);

ALTER TABLE org_visual_node_status
  ADD CONSTRAINT fk_org_visual_node_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_org_visual_node_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_org_visual_node_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

CREATE TRIGGER tr_org_visual_node_status_updated_at
  BEFORE UPDATE ON org_visual_node_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

-- ---------- source: V26__dashboard_kpi_segment.sql ----------

-- Home dashboard KPI tiles: which correspondence_status rows roll into which headline metric (editable via data; no frontend status-code lists).
-- Segments: SLA_DONE (completed work), PIPELINE (active handling), INBOX (new intake).

ALTER TABLE correspondence_status
  ADD COLUMN kpi_segment VARCHAR(32) NULL;

UPDATE correspondence_status SET kpi_segment = 'SLA_DONE'
  WHERE UPPER(code) IN ('COMPLETED', 'ARCHIVED') AND deleted_at IS NULL;

UPDATE correspondence_status SET kpi_segment = 'PIPELINE'
  WHERE UPPER(code) IN ('IN_PROGRESS', 'PENDING_APPROVAL', 'RETURNED') AND deleted_at IS NULL;

UPDATE correspondence_status SET kpi_segment = 'INBOX'
  WHERE UPPER(code) = 'NEW' AND deleted_at IS NULL;

ALTER TABLE correspondence_status
  ADD CONSTRAINT ck_correspondence_status_kpi_segment CHECK (
    kpi_segment IS NULL OR kpi_segment IN ('SLA_DONE', 'PIPELINE', 'INBOX')
  );

COMMENT ON COLUMN correspondence_status.kpi_segment IS 'Optional home-dashboard KPI bucket; NULL = not counted toward SLA_DONE/PIPELINE/INBOX tiles.';

ALTER TABLE correspondence_type
  ADD COLUMN dashboard_outbound_highlight BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE correspondence_type SET dashboard_outbound_highlight = TRUE
  WHERE UPPER(code) = 'OUTBOUND' AND deleted_at IS NULL;

COMMENT ON COLUMN correspondence_type.dashboard_outbound_highlight IS 'When TRUE, correspondence rows of this type count toward the outbound KPI (dashboard API).';

ALTER TABLE correspondence_type
  ADD COLUMN dashboard_inbound_highlight BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE correspondence_type SET dashboard_inbound_highlight = TRUE
  WHERE UPPER(code) = 'INBOUND' AND deleted_at IS NULL;

COMMENT ON COLUMN correspondence_type.dashboard_inbound_highlight IS 'When TRUE, list UIs may treat this type as inbound traffic (lookup bundle).';

-- ---------- source: V27__workflow_action_transitions.sql ----------

-- Drive Camunda task decisions and status changes from data (no hardcoded APPROVE/REJECT/... in Java).
DROP INDEX IF EXISTS srs_system.ux_workflow_action_type_code_active;

ALTER TABLE workflow_action_type
  ADD COLUMN IF NOT EXISTS allowed_from_correspondence_status_id BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS next_correspondence_status_id BIGINT REFERENCES correspondence_status (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS required_role_id BIGINT REFERENCES role (id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS requires_comment BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS show_in_task_decision_ui BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN workflow_action_type.allowed_from_correspondence_status_id IS 'If set, this row applies only when correspondence has this status; NULL = wildcard for any status.';
COMMENT ON COLUMN workflow_action_type.next_correspondence_status_id IS 'Lifecycle status after task completes with this action code; NULL = do not change status.';
COMMENT ON COLUMN workflow_action_type.required_role_id IS 'If set, only users with this role may use this action in the task UI.';
COMMENT ON COLUMN workflow_action_type.requires_comment IS 'When TRUE, API rejects the action without a non-blank comment.';
COMMENT ON COLUMN workflow_action_type.show_in_task_decision_ui IS 'When TRUE, listed in correspondence detail workflow-actions API for assignees.';

-- Wildcard row: at most one per code where allowed_from IS NULL
CREATE UNIQUE INDEX ux_workflow_action_type_code_wildcard
  ON workflow_action_type (upper((code)::text))
  WHERE deleted_at IS NULL AND allowed_from_correspondence_status_id IS NULL;

-- Specific from-status: unique (code, from_status)
CREATE UNIQUE INDEX ux_workflow_action_type_code_from
  ON workflow_action_type (upper((code)::text), allowed_from_correspondence_status_id)
  WHERE deleted_at IS NULL AND allowed_from_correspondence_status_id IS NOT NULL;

-- Seed transitions (Camunda wfDecision variable = workflow_action_type.code)
UPDATE workflow_action_type w
SET next_correspondence_status_id = (SELECT id FROM correspondence_status s WHERE UPPER(s.code) = 'COMPLETED' AND s.deleted_at IS NULL ORDER BY s.id LIMIT 1),
    requires_comment = FALSE,
    show_in_task_decision_ui = TRUE
WHERE UPPER(w.code) = 'APPROVE' AND w.deleted_at IS NULL AND w.allowed_from_correspondence_status_id IS NULL;

UPDATE workflow_action_type w
SET next_correspondence_status_id = (SELECT id FROM correspondence_status s WHERE UPPER(s.code) = 'REJECTED' AND s.deleted_at IS NULL ORDER BY s.id LIMIT 1),
    requires_comment = TRUE,
    show_in_task_decision_ui = TRUE
WHERE UPPER(w.code) = 'REJECT' AND w.deleted_at IS NULL AND w.allowed_from_correspondence_status_id IS NULL;

UPDATE workflow_action_type w
SET next_correspondence_status_id = (SELECT id FROM correspondence_status s WHERE UPPER(s.code) = 'RETURNED' AND s.deleted_at IS NULL ORDER BY s.id LIMIT 1),
    requires_comment = TRUE,
    show_in_task_decision_ui = TRUE
WHERE UPPER(w.code) = 'RETURN' AND w.deleted_at IS NULL AND w.allowed_from_correspondence_status_id IS NULL;

UPDATE workflow_action_type w
SET next_correspondence_status_id = (SELECT id FROM correspondence_status s WHERE UPPER(s.code) = 'IN_PROGRESS' AND s.deleted_at IS NULL ORDER BY s.id LIMIT 1),
    requires_comment = TRUE,
    show_in_task_decision_ui = TRUE
WHERE UPPER(w.code) = 'REFER' AND w.deleted_at IS NULL AND w.allowed_from_correspondence_status_id IS NULL;

-- ---------- source: V28__correspondence_cancel_metadata_workflow_ui_variant.sql ----------

-- Cancel eligibility from correspondence_status (no hardcoded CANCELLED target in Java).
-- Workflow task button semantics: ui_variant drives Angular CSS mapping (not action codes).

ALTER TABLE correspondence_status
  ADD COLUMN IF NOT EXISTS allows_cancel BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS cancel_outcome BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN correspondence_status.allows_cancel IS 'When FALSE, user cancel is blocked while in this status (non-terminal use case).';
COMMENT ON COLUMN correspondence_status.cancel_outcome IS 'Exactly one active row: lifecycle status applied when user cancels.';

UPDATE correspondence_status SET allows_cancel = FALSE WHERE is_terminal = TRUE AND deleted_at IS NULL;

UPDATE correspondence_status SET cancel_outcome = TRUE
  WHERE UPPER(code) = 'CANCELLED' AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_correspondence_status_one_cancel_outcome
  ON correspondence_status ((1))
  WHERE deleted_at IS NULL AND cancel_outcome IS TRUE;

ALTER TABLE workflow_action_type
  ADD COLUMN IF NOT EXISTS ui_variant VARCHAR(32) NOT NULL DEFAULT 'secondary';

ALTER TABLE workflow_action_type
  DROP CONSTRAINT IF EXISTS ck_workflow_action_type_ui_variant;

ALTER TABLE workflow_action_type
  ADD CONSTRAINT ck_workflow_action_type_ui_variant CHECK (
    ui_variant IN ('primary', 'secondary', 'danger', 'warning', 'success')
  );

COMMENT ON COLUMN workflow_action_type.ui_variant IS 'Semantic button style for task decision UI (maps to CSS on the client).';

UPDATE workflow_action_type SET ui_variant = 'primary' WHERE UPPER(code) = 'APPROVE' AND deleted_at IS NULL;
UPDATE workflow_action_type SET ui_variant = 'danger' WHERE UPPER(code) = 'REJECT' AND deleted_at IS NULL;
UPDATE workflow_action_type SET ui_variant = 'warning' WHERE UPPER(code) = 'RETURN' AND deleted_at IS NULL;
UPDATE workflow_action_type SET ui_variant = 'secondary' WHERE UPPER(code) IN ('REFER', 'ROUTE') AND deleted_at IS NULL;

-- ---------- source: V29__correspondence_status_ui_variant.sql ----------

-- Status badge semantics for correspondence lifecycle (fully data-driven UI; no status-code substring styling).
ALTER TABLE correspondence_status
  ADD COLUMN IF NOT EXISTS ui_variant VARCHAR(32) NOT NULL DEFAULT 'neutral';

ALTER TABLE correspondence_status
  DROP CONSTRAINT IF EXISTS ck_correspondence_status_ui_variant;

ALTER TABLE correspondence_status
  ADD CONSTRAINT ck_correspondence_status_ui_variant CHECK (
    ui_variant IN ('success', 'danger', 'warning', 'info', 'secondary', 'neutral')
  );

COMMENT ON COLUMN correspondence_status.ui_variant IS 'Badge style key for lists/detail (success, danger, warning, info, secondary, neutral).';

UPDATE correspondence_status SET ui_variant = 'info'
  WHERE UPPER(code) IN ('NEW', 'PENDING_APPROVAL') AND deleted_at IS NULL;

UPDATE correspondence_status SET ui_variant = 'secondary'
  WHERE UPPER(code) = 'IN_PROGRESS' AND deleted_at IS NULL;

UPDATE correspondence_status SET ui_variant = 'warning'
  WHERE UPPER(code) IN ('RETURNED', 'DEFERRED') AND deleted_at IS NULL;

UPDATE correspondence_status SET ui_variant = 'success'
  WHERE UPPER(code) = 'COMPLETED' AND deleted_at IS NULL;

UPDATE correspondence_status SET ui_variant = 'danger'
  WHERE UPPER(code) IN ('REJECTED', 'CANCELLED') AND deleted_at IS NULL;

UPDATE correspondence_status SET ui_variant = 'neutral'
  WHERE UPPER(code) = 'ARCHIVED' AND deleted_at IS NULL;

-- ---------- source: V30__permission_ui_screen_capabilities_roles.sql ----------

-- Link permissions to screens (metadata); seed capability permission codes; demo ADMIN/USER roles.

ALTER TABLE permission
  ADD COLUMN IF NOT EXISTS ui_screen_id BIGINT REFERENCES ui_screen (id) ON DELETE SET NULL;

COMMENT ON COLUMN permission.ui_screen_id IS 'Optional primary screen for this permission (admin inventory / capabilities).';

CREATE INDEX IF NOT EXISTS ix_permission_ui_screen_id ON permission (ui_screen_id) WHERE deleted_at IS NULL;

-- Canonical capability codes (frontend guards use these strings).
INSERT INTO permission (code, name_ar, name_en, sort_order, is_active, ui_screen_id)
SELECT 'VIEW_DASHBOARD', 'عرض ���حة ا�تحْ�&', 'View dashboard', 5, TRUE, s.id
FROM ui_screen s
WHERE LOWER(s.code) = 'dashboard' AND s.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'VIEW_DASHBOARD' AND p.deleted_at IS NULL);

INSERT INTO permission (code, name_ar, name_en, sort_order, is_active, ui_screen_id)
SELECT 'VIEW_TRANSACTIONS', 'عرض ا��&عا�&�ات', 'View transactions', 6, TRUE, s.id
FROM ui_screen s
WHERE LOWER(s.code) = 'transactions' AND s.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'VIEW_TRANSACTIONS' AND p.deleted_at IS NULL);

INSERT INTO permission (code, name_ar, name_en, sort_order, is_active, ui_screen_id)
SELECT 'CREATE_TRANSACTION', 'إ� شاء �&عا�&�ة', 'Create transaction', 7, TRUE, s.id
FROM ui_screen s
WHERE LOWER(s.code) = 'create_transaction' AND s.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'CREATE_TRANSACTION' AND p.deleted_at IS NULL);

INSERT INTO permission (code, name_ar, name_en, sort_order, is_active, ui_screen_id)
SELECT 'CANCEL_TRANSACTION', 'إ�غاء �&عا�&�ة', 'Cancel transaction', 8, TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.code = 'CANCEL_TRANSACTION' AND p.deleted_at IS NULL);

UPDATE permission p
SET ui_screen_id = (SELECT id FROM ui_screen WHERE LOWER(code) = 'dashboard' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE p.code = 'VIEW_DASHBOARD' AND p.deleted_at IS NULL AND p.ui_screen_id IS NULL;

UPDATE permission p
SET ui_screen_id = (SELECT id FROM ui_screen WHERE LOWER(code) = 'transactions' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE p.code = 'VIEW_TRANSACTIONS' AND p.deleted_at IS NULL AND p.ui_screen_id IS NULL;

UPDATE permission p
SET ui_screen_id = (SELECT id FROM ui_screen WHERE LOWER(code) = 'create_transaction' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE p.code = 'CREATE_TRANSACTION' AND p.deleted_at IS NULL AND p.ui_screen_id IS NULL;

-- Demo roles (do not remove existing SRS roles)
INSERT INTO role (code, name_ar, name_en, sort_order, is_active)
SELECT 'ADMIN', '�&سؤ���', 'Administrator', 5, TRUE
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.code = 'ADMIN' AND r.deleted_at IS NULL);

INSERT INTO role (code, name_ar, name_en, sort_order, is_active)
SELECT 'USER', '�&ستخد�&', 'User', 6, TRUE
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.code = 'USER' AND r.deleted_at IS NULL);

-- Grant new permissions to SYS_ADMIN
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('VIEW_DASHBOARD', 'VIEW_TRANSACTIONS', 'CREATE_TRANSACTION', 'CANCEL_TRANSACTION')
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN: all four
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('VIEW_DASHBOARD', 'VIEW_TRANSACTIONS', 'CREATE_TRANSACTION', 'CANCEL_TRANSACTION')
WHERE r.code = 'ADMIN' AND r.deleted_at IS NULL AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- USER: read-only dashboard + list
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('VIEW_DASHBOARD', 'VIEW_TRANSACTIONS')
WHERE r.code = 'USER' AND r.deleted_at IS NULL AND p.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Align legacy roles: correspondence.view â†’ VIEW_DASHBOARD + VIEW_TRANSACTIONS
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, p.id
FROM role_permission rp
JOIN permission src ON src.id = rp.permission_id AND src.code = 'correspondence.view' AND src.deleted_at IS NULL
JOIN permission p ON p.code IN ('VIEW_DASHBOARD', 'VIEW_TRANSACTIONS') AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission x WHERE x.role_id = rp.role_id AND x.permission_id = p.id
  );

-- correspondence.create â†’ CREATE_TRANSACTION
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, p.id
FROM role_permission rp
JOIN permission src ON src.id = rp.permission_id AND src.code = 'correspondence.create' AND src.deleted_at IS NULL
JOIN permission p ON p.code = 'CREATE_TRANSACTION' AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission x WHERE x.role_id = rp.role_id AND x.permission_id = p.id
  );

-- correspondence.delete â†’ CANCEL_TRANSACTION
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, p.id
FROM role_permission rp
JOIN permission src ON src.id = rp.permission_id AND src.code = 'correspondence.delete' AND src.deleted_at IS NULL
JOIN permission p ON p.code = 'CANCEL_TRANSACTION' AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission x WHERE x.role_id = rp.role_id AND x.permission_id = p.id
  );

