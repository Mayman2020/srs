-- =============================================================================
-- V7__permissions_unified.sql
--
-- Introduce the canonical SCREAMING_SNAKE permission catalog used by the
-- target plan (CORRESPONDENCE_*, WORKFLOW_TASK_*, ADMIN_*, NOTIFICATION_*,
-- REPORT_*, DASHBOARD_VIEW, DELEGATION_MANAGE, LEAVE_*).
--
-- We do NOT delete the legacy dotted-lowercase codes (correspondence.view,
-- lookup.manage, user.manage, etc.) or the V30 codes (VIEW_TRANSACTIONS,
-- CREATE_TRANSACTION, ...). Instead a permission_alias table maps legacy ->
-- canonical so EffectiveUserPermissionService can resolve either spelling.
--
-- V13 (Phase 9) will drop the alias table and legacy codes once Spring
-- @PreAuthorize strings and FE route data are fully migrated.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- permission_alias: alias_code -> canonical permission_id.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permission_alias (
  id            BIGSERIAL PRIMARY KEY,
  alias_code    VARCHAR(128) NOT NULL,
  permission_id BIGINT       NOT NULL REFERENCES permission (id) ON DELETE CASCADE,
  notes         TEXT,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    UUID,
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by    UUID
);

COMMENT ON TABLE permission_alias IS
  'Legacy permission code (alias_code) -> canonical permission.id. Removed in V13.';

CREATE UNIQUE INDEX IF NOT EXISTS ux_permission_alias_code
  ON permission_alias (UPPER(alias_code));

CREATE INDEX IF NOT EXISTS ix_permission_alias_permission_id
  ON permission_alias (permission_id);

-- -----------------------------------------------------------------------------
-- Canonical permissions (idempotent inserts).
-- -----------------------------------------------------------------------------
-- Helper: insert one permission if its code does not yet exist.
-- (Done as plain INSERT ... WHERE NOT EXISTS so the migration stays declarative.)

INSERT INTO permission (code, name_ar, name_en, description, sort_order, is_active)
SELECT v.code, v.name_ar, v.name_en, v.description, v.sort_order, TRUE
FROM (VALUES
  ('CORRESPONDENCE_CREATE',     'إنشاء مراسلة',         'Create correspondence',     100, 'Originate a new correspondence (inbound/outbound/internal).'),
  ('CORRESPONDENCE_VIEW',       'عرض المراسلات',        'View correspondence',       110, 'Read correspondences within visibility rules.'),
  ('CORRESPONDENCE_UPDATE',     'تعديل مراسلات',        'Update correspondence',     120, 'Edit subject/body/metadata/attachments of a correspondence.'),
  ('CORRESPONDENCE_DELETE',     'حذف مراسلة',          'Delete correspondence',     130, 'Soft-delete a correspondence (creator or admin).'),
  ('CORRESPONDENCE_APPROVE',    'اعتماد مراسلة',        'Approve correspondence',    140, 'Execute APPROVE workflow action on assigned tasks.'),
  ('CORRESPONDENCE_REJECT',     'رفض مراسلة',          'Reject correspondence',     150, 'Execute REJECT workflow action on assigned tasks.'),
  ('CORRESPONDENCE_FORWARD',    'إحالة مراسلة',         'Forward correspondence',    160, 'Reroute a correspondence to a different department/user.'),
  ('CORRESPONDENCE_ARCHIVE',    'أرشفة مراسلة',         'Archive correspondence',    170, 'Move a terminal correspondence into the archive.'),

  ('WORKFLOW_TASK_VIEW',        'عرض مهام سير العمل',    'View workflow tasks',       200, 'See active Camunda tasks in the inbox.'),
  ('WORKFLOW_TASK_ACTION',      'تنفيذ مهمة',           'Act on workflow tasks',     210, 'Complete, claim, delegate Camunda tasks.'),

  ('ADMIN_LOOKUP_MANAGE',       'إدارة القوائم',        'Manage lookups',            300, 'Manage lookup catalogs and rows.'),
  ('ADMIN_USER_MANAGE',         'إدارة المستخدمين',     'Manage users',              310, 'CRUD on app_user and role assignments.'),
  ('ADMIN_ROLE_MANAGE',         'إدارة الأدوار',        'Manage roles',              320, 'CRUD on role and role_permission.'),
  ('ADMIN_ORG_MANAGE',          'إدارة الهيكل التنظيمي', 'Manage organizational structure', 330, 'Manage departments, organizations, and Q/L/K/S levels.'),
  ('ADMIN_UI_SCREEN_MANAGE',    'إدارة شاشات الواجهة',   'Manage UI screens',         340, 'Manage ui_screen entries that drive shell navigation.'),
  ('ADMIN_AUDIT_VIEW',          'عرض سجل التدقيق',      'View audit trail',          350, 'View generic audit_event and audit_log records.'),
  ('ADMIN_SYSTEM_ISSUE_VIEW',   'عرض مشاكل النظام',     'View system issues',        360, 'View / resolve client-reported system issues.'),

  ('NOTIFICATION_DISPATCH',     'إرسال إشعارات',        'Dispatch notifications',    400, 'Send outbound email / SMS notifications.'),
  ('NOTIFICATION_VIEW',         'عرض الإشعارات',        'View notifications',        410, 'Read own notification inbox.'),

  ('REPORT_VIEW',               'عرض التقارير',         'View reports',              500, 'View analytical reports and dashboards.'),
  ('REPORT_EXPORT',             'تصدير التقارير',       'Export reports',            510, 'Export reports to Excel/CSV/PDF.'),

  ('DASHBOARD_VIEW',            'عرض لوحة التحكم',      'View dashboard',            600, 'See dashboard KPIs.'),
  ('DELEGATION_MANAGE',         'إدارة التفويضات',      'Manage delegations',        700, 'Create and revoke authority delegations.'),
  ('LEAVE_SELF',                'طلبات الإجازة الذاتية', 'Self leave requests',       800, 'Submit and view own leave requests.'),
  ('LEAVE_ADMIN',               'إدارة طلبات الإجازة',  'Administer leave requests', 810, 'Approve / reject leave requests for others.')
-- Column alias order must match the VALUES tuple order above:
-- (code, name_ar, name_en, sort_order, description).
) AS v(code, name_ar, name_en, sort_order, description)
WHERE NOT EXISTS (
  SELECT 1 FROM permission p WHERE p.code = v.code AND p.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- Alias mapping (legacy code -> canonical permission).
-- Bridge V30 SCREAMING_SNAKE seeds + V1 dotted-lowercase seeds to the new set.
-- -----------------------------------------------------------------------------
INSERT INTO permission_alias (alias_code, permission_id, notes)
SELECT v.alias_code, p.id, v.notes
FROM (VALUES
  -- legacy dotted -> canonical (V1)
  ('correspondence.view',    'CORRESPONDENCE_VIEW',    'V1 legacy dotted code'),
  ('correspondence.create',  'CORRESPONDENCE_CREATE',  'V1 legacy dotted code'),
  ('correspondence.edit',    'CORRESPONDENCE_UPDATE',  'V1 legacy dotted code'),
  ('correspondence.delete',  'CORRESPONDENCE_DELETE',  'V1 legacy dotted code'),
  ('correspondence.approve', 'CORRESPONDENCE_APPROVE', 'V1 legacy dotted code'),
  ('workflow.execute',       'WORKFLOW_TASK_ACTION',   'V1 legacy dotted code'),
  ('user.manage',            'ADMIN_USER_MANAGE',      'V1 legacy dotted code'),
  ('role.manage',            'ADMIN_ROLE_MANAGE',      'V1 legacy dotted code'),
  ('report.view',            'REPORT_VIEW',            'V1 legacy dotted code'),
  ('audit.view',             'ADMIN_AUDIT_VIEW',       'V1 legacy dotted code'),
  ('admin.settings',         'ADMIN_UI_SCREEN_MANAGE', 'V1 legacy dotted code'),
  ('lookup.manage',          'ADMIN_LOOKUP_MANAGE',    'V1 legacy dotted code'),
  ('leave.self',             'LEAVE_SELF',             'V1 leave permission'),
  ('leave.admin',            'LEAVE_ADMIN',            'V1 leave permission'),
  ('delegation.manage',      'DELEGATION_MANAGE',      'V1 delegation permission'),

  -- V30 SCREAMING_SNAKE -> canonical
  ('VIEW_DASHBOARD',      'DASHBOARD_VIEW',         'V30 capability code'),
  ('VIEW_TRANSACTIONS',   'CORRESPONDENCE_VIEW',    'V30 capability code (transactions = correspondence)'),
  ('CREATE_TRANSACTION',  'CORRESPONDENCE_CREATE',  'V30 capability code (transactions = correspondence)'),
  ('CANCEL_TRANSACTION',  'CORRESPONDENCE_DELETE',  'V30 capability code maps to delete/cancel')
) AS v(alias_code, canonical_code, notes)
JOIN permission p ON p.code = v.canonical_code AND p.deleted_at IS NULL
WHERE NOT EXISTS (
  SELECT 1 FROM permission_alias a WHERE UPPER(a.alias_code) = UPPER(v.alias_code)
);

-- -----------------------------------------------------------------------------
-- Grant canonical permissions to seeded roles, preserving current capability.
-- For each role, union the canonical equivalents of permissions already granted.
-- -----------------------------------------------------------------------------
INSERT INTO role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, canonical.id
FROM role_permission rp
JOIN permission legacy
  ON legacy.id = rp.permission_id AND legacy.deleted_at IS NULL
JOIN permission_alias alias
  ON UPPER(alias.alias_code) = UPPER(legacy.code)
JOIN permission canonical
  ON canonical.id = alias.permission_id AND canonical.deleted_at IS NULL
WHERE NOT EXISTS (
  SELECT 1 FROM role_permission existing
  WHERE existing.role_id = rp.role_id AND existing.permission_id = canonical.id
);

-- SYS_ADMIN: all canonical permissions.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SYS_ADMIN' AND r.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND p.code IN (
    'CORRESPONDENCE_CREATE','CORRESPONDENCE_VIEW','CORRESPONDENCE_UPDATE','CORRESPONDENCE_DELETE',
    'CORRESPONDENCE_APPROVE','CORRESPONDENCE_REJECT','CORRESPONDENCE_FORWARD','CORRESPONDENCE_ARCHIVE',
    'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION',
    'ADMIN_LOOKUP_MANAGE','ADMIN_USER_MANAGE','ADMIN_ROLE_MANAGE','ADMIN_ORG_MANAGE',
    'ADMIN_UI_SCREEN_MANAGE','ADMIN_AUDIT_VIEW','ADMIN_SYSTEM_ISSUE_VIEW',
    'NOTIFICATION_DISPATCH','NOTIFICATION_VIEW',
    'REPORT_VIEW','REPORT_EXPORT',
    'DASHBOARD_VIEW','DELEGATION_MANAGE','LEAVE_SELF','LEAVE_ADMIN'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- AUDITOR: read everything that an auditor needs.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_VIEW','WORKFLOW_TASK_VIEW','REPORT_VIEW','REPORT_EXPORT',
  'DASHBOARD_VIEW','ADMIN_AUDIT_VIEW','NOTIFICATION_VIEW'
) AND p.deleted_at IS NULL
WHERE r.code = 'AUDITOR' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- APPROVER: review/approve/reject.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_VIEW','CORRESPONDENCE_APPROVE','CORRESPONDENCE_REJECT','CORRESPONDENCE_FORWARD',
  'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION','REPORT_VIEW','DASHBOARD_VIEW','NOTIFICATION_VIEW'
) AND p.deleted_at IS NULL
WHERE r.code = 'APPROVER' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- CORRESP_CLERK: clerk operations.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_CREATE','CORRESPONDENCE_VIEW','CORRESPONDENCE_UPDATE',
  'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION','DASHBOARD_VIEW','NOTIFICATION_VIEW'
) AND p.deleted_at IS NULL
WHERE r.code = 'CORRESP_CLERK' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- STAFF: basic correspondence + own leave.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_CREATE','CORRESPONDENCE_VIEW','CORRESPONDENCE_UPDATE',
  'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION','DASHBOARD_VIEW','NOTIFICATION_VIEW','LEAVE_SELF'
) AND p.deleted_at IS NULL
WHERE r.code = 'STAFF' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- DEPT_MANAGER: department manager.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_CREATE','CORRESPONDENCE_VIEW','CORRESPONDENCE_UPDATE',
  'CORRESPONDENCE_APPROVE','CORRESPONDENCE_REJECT','CORRESPONDENCE_FORWARD',
  'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION',
  'REPORT_VIEW','DASHBOARD_VIEW','NOTIFICATION_VIEW','DELEGATION_MANAGE','LEAVE_ADMIN','LEAVE_SELF'
) AND p.deleted_at IS NULL
WHERE r.code = 'DEPT_MANAGER' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- CORRESP_MGR: correspondence manager.
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
  'CORRESPONDENCE_CREATE','CORRESPONDENCE_VIEW','CORRESPONDENCE_UPDATE','CORRESPONDENCE_DELETE',
  'CORRESPONDENCE_APPROVE','CORRESPONDENCE_REJECT','CORRESPONDENCE_FORWARD','CORRESPONDENCE_ARCHIVE',
  'WORKFLOW_TASK_VIEW','WORKFLOW_TASK_ACTION',
  'REPORT_VIEW','REPORT_EXPORT','DASHBOARD_VIEW','NOTIFICATION_VIEW','NOTIFICATION_DISPATCH',
  'ADMIN_LOOKUP_MANAGE','DELEGATION_MANAGE'
) AND p.deleted_at IS NULL
WHERE r.code = 'CORRESP_MGR' AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
