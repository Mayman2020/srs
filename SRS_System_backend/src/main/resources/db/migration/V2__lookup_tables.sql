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
