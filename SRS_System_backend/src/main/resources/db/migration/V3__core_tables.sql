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
  username           CITEXT       NOT NULL,
  password_hash      TEXT,
  full_name_ar       VARCHAR(200) NOT NULL,
  full_name_en       VARCHAR(200) NOT NULL,
  email              CITEXT       NOT NULL,
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
