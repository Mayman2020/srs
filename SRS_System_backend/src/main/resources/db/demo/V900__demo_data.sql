-- =============================================================================
-- V900__demo_data.sql  (LOCAL PROFILE ONLY)
--
-- Demo seed that lets a fresh local environment showcase every Slice 6 feature:
--   * A small department tree
--   * Several human logins covering every role (SYS_ADMIN, CORRESP_MGR,
--     CORRESP_CLERK, DEPT_MANAGER, STAFF, APPROVER, AUDITOR) plus a TOP_SECRET
--     cleared operator
--   * A handful of correspondences in different statuses / confidentialities
--   * Notification channel targets (EMAIL/WEBHOOK/TEAMS) and outbox rows in
--     every status (PENDING / SENT / FAILED / DEAD)
--   * Two notification preference rows for the staff user
--   * One active legal hold on one correspondence
--   * Archive transition log rows (DRY_RUN preview) to populate the audit grid
--
-- Every INSERT is wrapped with WHERE NOT EXISTS / ON CONFLICT-style guards so
-- this script can be re-run safely against an existing local database.
--
-- This file lives under classpath:db/demo and is ONLY picked up by the `local`
-- Spring profile (application-local.yml overrides spring.flyway.locations to
-- include the demo folder). Production / staging / test profiles inherit only
-- `classpath:db/migration` from application.yml, so demo data never leaks into
-- real environments.
-- =============================================================================

SET search_path TO srs_system, public;

-- -----------------------------------------------------------------------------
-- 1. Departments (children of ROOT)
-- -----------------------------------------------------------------------------
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT v.code, v.name_ar, v.name_en, (SELECT id FROM department WHERE code = 'ROOT'), v.sort_order
FROM (VALUES
  ('CORRESP_OFFICE', 'مكتب الاتصالات الإدارية', 'Correspondence office',   10),
  ('LEGAL_AFFAIRS',  'الشؤون القانونية',         'Legal affairs',           20),
  ('IT_DEPT',        'تقنية المعلومات',           'Information technology',  30),
  ('HR_DEPT',        'الموارد البشرية',           'Human resources',         40),
  ('FINANCE',        'الشؤون المالية',            'Finance',                 50)
) AS v(code, name_ar, name_en, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM department d WHERE d.code = v.code AND d.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 2. Human demo users (UUIDs hard-coded so re-runs hit ON CONFLICT cleanly)
--
-- DelegatingPasswordEncoder accepts the {noop}plain encoding for dev only —
-- every password below is identical to the username for easy login. Never use
-- {noop} in any non-local environment.
-- -----------------------------------------------------------------------------
WITH demo(id, username, full_ar, full_en, dept_code, clearance_code) AS (
  VALUES
    ('d0000000-0000-4000-8000-000000000001'::uuid, 'manager',  'مدير المراسلات',     'Correspondence manager', 'CORRESP_OFFICE', 'SECRET'),
    ('d0000000-0000-4000-8000-000000000002'::uuid, 'clerk',    'موظف اتصالات إدارية', 'Correspondence clerk',  'CORRESP_OFFICE', 'NORMAL'),
    ('d0000000-0000-4000-8000-000000000003'::uuid, 'staff',    'موظف عام',           'General staff',         'HR_DEPT',        'NORMAL'),
    ('d0000000-0000-4000-8000-000000000004'::uuid, 'approver', 'معتمد الإدارة',       'Approver',              'LEGAL_AFFAIRS',  'SECRET'),
    ('d0000000-0000-4000-8000-000000000005'::uuid, 'auditor',  'مدقق النظام',         'Auditor',               'IT_DEPT',        'SECRET'),
    ('d0000000-0000-4000-8000-000000000006'::uuid, 'topsecret','ضابط أمن المعلومات', 'Security officer',      'IT_DEPT',        'TOP_SECRET'),
    ('d0000000-0000-4000-8000-000000000007'::uuid, 'deptmgr',  'مدير إدارة',         'Department manager',    'FINANCE',        'SECRET')
)
INSERT INTO app_user (
  id, username, password_hash, full_name_ar, full_name_en, email,
  department_id, security_clearance_id, is_active
)
SELECT
  demo.id,
  demo.username,
  '{noop}' || demo.username,
  demo.full_ar,
  demo.full_en,
  demo.username || '@local.invalid',
  COALESCE(
    (SELECT id FROM department WHERE code = demo.dept_code AND deleted_at IS NULL LIMIT 1),
    (SELECT id FROM department WHERE code = 'ROOT' LIMIT 1)
  ),
  (SELECT id FROM confidentiality WHERE code = demo.clearance_code AND deleted_at IS NULL LIMIT 1),
  TRUE
FROM demo
WHERE NOT EXISTS (
  SELECT 1 FROM app_user u WHERE u.username = demo.username AND u.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 3. Role grants for the demo users (idempotent)
-- -----------------------------------------------------------------------------
INSERT INTO user_role (app_user_id, role_id)
SELECT u.id, r.id
FROM app_user u
JOIN role r ON r.deleted_at IS NULL
WHERE u.deleted_at IS NULL
  AND (
       (u.username = 'manager'   AND r.code IN ('CORRESP_MGR', 'APPROVER'))
    OR (u.username = 'clerk'     AND r.code = 'CORRESP_CLERK')
    OR (u.username = 'staff'     AND r.code = 'STAFF')
    OR (u.username = 'approver'  AND r.code = 'APPROVER')
    OR (u.username = 'auditor'   AND r.code = 'AUDITOR')
    OR (u.username = 'topsecret' AND r.code IN ('SYS_ADMIN', 'AUDITOR'))
    OR (u.username = 'deptmgr'   AND r.code IN ('DEPT_MANAGER', 'APPROVER'))
  )
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur
    WHERE ur.app_user_id = u.id AND ur.role_id = r.id
  );

-- -----------------------------------------------------------------------------
-- 4. Demo correspondences (different status / type / confidentiality)
--    UUIDs hard-coded so we can FK against them for legal hold / outbox below.
-- -----------------------------------------------------------------------------
WITH demo_corr(id, ref_no, type_code, status_code, priority_code, conf_code,
               subject, description, owner_dept_code, created_username) AS (
  VALUES
    ('c0000000-0000-4000-8000-000000000001'::uuid, 'INC-2026-0001',
       'INBOUND',  'NEW',         'NORMAL', 'NORMAL',
       'استلام طلب موردين',                'استلام طلب من شركة موردين بشأن تجديد العقد الإطاري.',
       'CORRESP_OFFICE', 'clerk'),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 'OUT-2026-0002',
       'OUTBOUND', 'IN_PROGRESS', 'HIGH',   'LIMITED',
       'الرد على استفسار رسمي',            'مسودة رد رسمي على استفسار جهة خارجية حول لائحة الإجراءات.',
       'LEGAL_AFFAIRS',  'manager'),
    ('c0000000-0000-4000-8000-000000000003'::uuid, 'INT-2026-0003',
       'INTERNAL', 'IN_PROGRESS', 'NORMAL', 'NORMAL',
       'تعميم إجراءات السلامة',             'تعميم داخلي لمتابعة تطبيق إجراءات السلامة في كافة الإدارات.',
       'HR_DEPT',        'staff'),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 'OUT-2026-0004',
       'OUTBOUND', 'COMPLETED',   'URGENT', 'NORMAL',
       'إفادة بالموافقة',                   'إفادة رسمية بالموافقة على طلب الترقية الإدارية.',
       'CORRESP_OFFICE', 'approver'),
    ('c0000000-0000-4000-8000-000000000005'::uuid, 'INC-2026-0005',
       'INBOUND',  'IN_PROGRESS', 'HIGH',   'SECRET',
       'مذكرة قانونية سرية',                'مذكرة قانونية سرية تتعلق بقضية تحت الدراسة.',
       'LEGAL_AFFAIRS',  'topsecret'),
    ('c0000000-0000-4000-8000-000000000006'::uuid, 'INT-2026-0006',
       'CIRCULAR', 'NEW',         'NORMAL', 'NORMAL',
       'تعميم إجازات الأعياد',             'جدول إجازات الأعياد الرسمية للسنة الحالية.',
       'HR_DEPT',        'manager')
)
INSERT INTO correspondence (
  id, reference_number,
  correspondence_type_id, correspondence_status_id,
  priority_id, confidentiality_id, classification_id,
  subject, description, owner_department_id,
  created_by, updated_by
)
SELECT
  d.id, d.ref_no,
  (SELECT id FROM correspondence_type   WHERE code = d.type_code     AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM correspondence_status WHERE code = d.status_code   AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM priority              WHERE code = d.priority_code AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM confidentiality       WHERE code = d.conf_code     AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM classification        WHERE code = 'GEN'           AND deleted_at IS NULL LIMIT 1),
  d.subject, d.description,
  (SELECT id FROM department WHERE code = d.owner_dept_code AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM app_user WHERE username = d.created_username AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM app_user WHERE username = d.created_username AND deleted_at IS NULL LIMIT 1)
FROM demo_corr d
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence c WHERE c.id = d.id
);

-- Circular recipients for the OUT-2026-0004 + INT-2026-0006 letters so the
-- recipient-grid screen has rows to render.
INSERT INTO correspondence_recipient (correspondence_id, department_id)
SELECT
  'c0000000-0000-4000-8000-000000000004'::uuid,
  d.id
FROM department d
WHERE d.code IN ('HR_DEPT', 'FINANCE', 'IT_DEPT')
  AND d.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM correspondence_recipient cr
    WHERE cr.correspondence_id = 'c0000000-0000-4000-8000-000000000004'::uuid
      AND cr.department_id = d.id
  );

INSERT INTO correspondence_recipient (correspondence_id, department_id)
SELECT
  'c0000000-0000-4000-8000-000000000006'::uuid,
  d.id
FROM department d
WHERE d.code IN ('CORRESP_OFFICE', 'LEGAL_AFFAIRS', 'HR_DEPT', 'FINANCE', 'IT_DEPT')
  AND d.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM correspondence_recipient cr
    WHERE cr.correspondence_id = 'c0000000-0000-4000-8000-000000000006'::uuid
      AND cr.department_id = d.id
  );

-- -----------------------------------------------------------------------------
-- 5. In-app notifications for the staff user (so the inbox is not empty)
-- -----------------------------------------------------------------------------
INSERT INTO notification (
  id, recipient_user_id, notification_event_type_id, correspondence_id,
  title_ar, title_en, body_ar, body_en, data
)
SELECT
  'e0000000-0000-4000-8000-000000000001'::uuid,
  (SELECT id FROM app_user WHERE username = 'staff' AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM notification_event_type WHERE code = 'ASSIGNED' LIMIT 1),
  'c0000000-0000-4000-8000-000000000003'::uuid,
  'تمت إحالة معاملة إليك',
  'A correspondence has been assigned to you',
  'تمت إحالة المعاملة رقم INT-2026-0003 إلى عهدتك للمتابعة.',
  'Correspondence INT-2026-0003 has been routed to you for follow-up.',
  '{"correspondenceId":"c0000000-0000-4000-8000-000000000003","referenceNumber":"INT-2026-0003"}'::jsonb
WHERE NOT EXISTS (
  SELECT 1 FROM notification WHERE id = 'e0000000-0000-4000-8000-000000000001'::uuid
);

INSERT INTO notification (
  id, recipient_user_id, notification_event_type_id, correspondence_id,
  title_ar, title_en, body_ar, body_en, data
)
SELECT
  'e0000000-0000-4000-8000-000000000002'::uuid,
  (SELECT id FROM app_user WHERE username = 'manager' AND deleted_at IS NULL LIMIT 1),
  (SELECT id FROM notification_event_type WHERE code = 'DUE_SOON' LIMIT 1),
  'c0000000-0000-4000-8000-000000000002'::uuid,
  'اقتراب موعد الاستحقاق',
  'Due date approaching',
  'تبقى أقل من يومين على موعد استحقاق المعاملة OUT-2026-0002.',
  'Less than two days remain until OUT-2026-0002 is due.',
  '{"correspondenceId":"c0000000-0000-4000-8000-000000000002","referenceNumber":"OUT-2026-0002"}'::jsonb
WHERE NOT EXISTS (
  SELECT 1 FROM notification WHERE id = 'e0000000-0000-4000-8000-000000000002'::uuid
);

-- -----------------------------------------------------------------------------
-- 6. Notification channel targets (Slice 6 admin UI)
--    signing_secret_ref is the env-var NAME, never a literal secret value.
-- -----------------------------------------------------------------------------
INSERT INTO notification_channel_target (
  id, channel_code, target_code, target_url, signing_secret_ref, enabled, description
)
SELECT v.id::uuid, v.channel_code, v.target_code, v.target_url, v.secret_ref, v.enabled, v.description
FROM (VALUES
  ('f0000000-0000-4000-8000-000000000001', 'EMAIL',   'demo-email-relay',
     NULL,
     NULL,
     TRUE,
     'Local SMTP relay (MailHog at smtp://localhost:1025). Demo only.'),
  ('f0000000-0000-4000-8000-000000000002', 'WEBHOOK', 'demo-corp-webhook',
     'https://example.invalid/notifications/corp',
     'AC_NOTIFICATION_WEBHOOK_SECRET',
     TRUE,
     'Sample corporate webhook target (HMAC-SHA256 signed via X-AC-Signature).'),
  ('f0000000-0000-4000-8000-000000000003', 'TEAMS',   'demo-ops-teams',
     'https://outlook.office.com/webhook/00000000-0000-0000-0000-000000000000@teams.invalid',
     'AC_NOTIFICATION_TEAMS_SECRET',
     FALSE,
     'Microsoft Teams ops channel (disabled by default — enable after rotating secret).')
) AS v(id, channel_code, target_code, target_url, secret_ref, enabled, description)
WHERE NOT EXISTS (
  SELECT 1 FROM notification_channel_target nct
  WHERE nct.target_code = v.target_code AND nct.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 7. Notification outbox samples — one row per status so the admin grid shows
--    every state, including DLQ.
-- -----------------------------------------------------------------------------
INSERT INTO notification_outbox (
  id, idempotency_key, event_type_code, channel_code,
  recipient_user_id, recipient_address, subject, body_text, payload_json,
  message_key, message_params_json,
  correlation_resource_type, correlation_resource_id,
  status, attempt_count, next_attempt_at, last_attempted_at, last_error,
  created_by
)
SELECT v.id::uuid, v.idempotency_key, v.event_type_code, v.channel_code,
       (SELECT id FROM app_user WHERE username = v.recipient_username AND deleted_at IS NULL LIMIT 1),
       v.recipient_address, v.subject, v.body_text, v.payload_json,
       v.message_key, v.message_params_json,
       v.corr_type, v.corr_id,
       v.status, v.attempt_count, v.next_attempt_at, v.last_attempted_at, v.last_error,
       (SELECT id FROM app_user WHERE username = 'system' AND deleted_at IS NULL LIMIT 1)
FROM (VALUES
  ('a0000000-0000-4000-8000-000000000001', 'demo-pending-1',  'CORRESPONDENCE_INBOUND', 'EMAIL',
     'staff',   'staff@local.invalid',
     'وارد جديد: INC-2026-0001',
     'تم تسجيل معاملة واردة جديدة برقم INC-2026-0001.',
     '{"referenceNumber":"INC-2026-0001"}',
     'notification.correspondence.inbound.created',
     '{"referenceNumber":"INC-2026-0001"}',
     'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000001',
     'PENDING', 0, now() + INTERVAL '5 minutes', NULL, NULL),
  ('a0000000-0000-4000-8000-000000000002', 'demo-sent-1',     'ASSIGNED',               'IN_APP',
     'staff',   NULL,
     'تمت إحالة معاملة إليك',
     'تمت إحالة المعاملة INT-2026-0003 إلى عهدتك.',
     '{"referenceNumber":"INT-2026-0003"}',
     'notification.correspondence.assigned',
     '{"referenceNumber":"INT-2026-0003"}',
     'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000003',
     'SENT', 1, now() - INTERVAL '2 hours', now() - INTERVAL '2 hours', NULL),
  ('a0000000-0000-4000-8000-000000000003', 'demo-failed-1',   'OVERDUE',                'WEBHOOK',
     'manager', 'https://example.invalid/notifications/corp',
     'تأخر معاملة',
     'تجاوزت المعاملة OUT-2026-0002 موعد الاستحقاق.',
     '{"referenceNumber":"OUT-2026-0002"}',
     'notification.correspondence.overdue',
     '{"referenceNumber":"OUT-2026-0002"}',
     'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000002',
     'FAILED', 2, now() + INTERVAL '15 minutes', now() - INTERVAL '20 minutes',
     'HTTP 503 from https://example.invalid/notifications/corp — will retry.'),
  ('a0000000-0000-4000-8000-000000000004', 'demo-dead-1',     'DUE_SOON',               'TEAMS',
     'manager', 'https://outlook.office.com/webhook/00000000-0000-0000-0000-000000000000@teams.invalid',
     'اقتراب موعد الاستحقاق',
     'تبقى أقل من يومين على موعد استحقاق المعاملة OUT-2026-0002.',
     '{"referenceNumber":"OUT-2026-0002"}',
     'notification.correspondence.due_soon',
     '{"referenceNumber":"OUT-2026-0002"}',
     'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000002',
     'DEAD', 6, now() - INTERVAL '1 day', now() - INTERVAL '1 day',
     'Max attempts (6) exceeded. Last failure: HTTP 401 from Teams incoming webhook (signing_secret_ref AC_NOTIFICATION_TEAMS_SECRET unset).'),
  ('a0000000-0000-4000-8000-000000000005', 'demo-pending-2',  'APPROVED',               'EMAIL',
     'approver','approver@local.invalid',
     'تم اعتماد المعاملة',
     'تم اعتماد المعاملة OUT-2026-0004 رسميا.',
     '{"referenceNumber":"OUT-2026-0004"}',
     'notification.correspondence.approved',
     '{"referenceNumber":"OUT-2026-0004"}',
     'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000004',
     'PENDING', 0, now() + INTERVAL '30 seconds', NULL, NULL)
) AS v(
  id, idempotency_key, event_type_code, channel_code,
  recipient_username, recipient_address, subject, body_text, payload_json,
  message_key, message_params_json,
  corr_type, corr_id,
  status, attempt_count, next_attempt_at, last_attempted_at, last_error
)
WHERE NOT EXISTS (
  SELECT 1 FROM notification_outbox o WHERE o.idempotency_key = v.idempotency_key
);

-- -----------------------------------------------------------------------------
-- 8. Notification preferences for the `staff` user
-- -----------------------------------------------------------------------------
INSERT INTO notification_preference (user_id, event_type_code, channel_code, enabled)
SELECT
  (SELECT id FROM app_user WHERE username = 'staff' AND deleted_at IS NULL LIMIT 1),
  v.event_code,
  v.channel_code,
  v.enabled
FROM (VALUES
  ('OVERDUE',  'EMAIL', FALSE),
  ('DUE_SOON', 'EMAIL', TRUE),
  ('ASSIGNED', 'IN_APP',TRUE)
) AS v(event_code, channel_code, enabled)
WHERE NOT EXISTS (
  SELECT 1 FROM notification_preference np
  WHERE np.user_id = (SELECT id FROM app_user WHERE username = 'staff' AND deleted_at IS NULL LIMIT 1)
    AND np.event_type_code = v.event_code
    AND np.channel_code = v.channel_code
    AND np.deleted_at IS NULL
);

-- -----------------------------------------------------------------------------
-- 9. Legal hold on the SECRET correspondence (so retention preview is blocked)
-- -----------------------------------------------------------------------------
INSERT INTO legal_hold (id, correspondence_id, reason, placed_by)
SELECT
  '10000000-0000-4000-8000-000000000001'::uuid,
  'c0000000-0000-4000-8000-000000000005'::uuid,
  'حجز قانوني — قضية رقم 2026/Q1/045 قيد التحقيق. لا يجوز حذف أو إخفاء المعاملة حتى رفع الحجز رسميا.',
  (SELECT id FROM app_user WHERE username = 'topsecret' AND deleted_at IS NULL LIMIT 1)
WHERE NOT EXISTS (
  SELECT 1 FROM legal_hold WHERE id = '10000000-0000-4000-8000-000000000001'::uuid
);

-- -----------------------------------------------------------------------------
-- 10. Archive transition log (DRY_RUN preview) — populates the audit grid.
-- -----------------------------------------------------------------------------
INSERT INTO archive_transition_log (
  id, applied_to, resource_id, policy_id, legal_hold_id, action, executed_by, detail_json
)
SELECT v.id::uuid, v.applied_to, v.resource_id,
       (SELECT id FROM retention_policy WHERE code = v.policy_code AND deleted_at IS NULL LIMIT 1),
       v.hold_id,
       v.action,
       (SELECT id FROM app_user WHERE username = 'system' AND deleted_at IS NULL LIMIT 1),
       v.detail_json
FROM (VALUES
  ('20000000-0000-4000-8000-000000000001', 'CORRESPONDENCE', 'c0000000-0000-4000-8000-000000000005',
     'CORRESP_DEFAULT_7Y', '10000000-0000-4000-8000-000000000001'::uuid,
     'SKIPPED_LEGAL_HOLD',
     '{"reason":"active legal hold","holdReason":"قضية 2026/Q1/045"}'),
  ('20000000-0000-4000-8000-000000000002', 'NOTIFICATION',   'e0000000-0000-4000-8000-000000000001',
     'NOTIFICATION_1Y', NULL,
     'SKIPPED_DRY_RUN',
     '{"reason":"ac.retention.dry-run = true","wouldAge":1}'),
  ('20000000-0000-4000-8000-000000000003', 'AUDIT_EVENT',    'demo-audit-1',
     'AUDIT_EVENT_10Y', NULL,
     'SKIPPED_DRY_RUN',
     '{"reason":"ac.retention.dry-run = true","wouldAge":1}'),
  ('20000000-0000-4000-8000-000000000004', 'ATTACHMENT_ACCESS_LOG', 'demo-access-1',
     'ACCESS_LOG_3Y', NULL,
     'SKIPPED_DRY_RUN',
     '{"reason":"ac.retention.dry-run = true","wouldAge":1}')
) AS v(id, applied_to, resource_id, policy_code, hold_id, action, detail_json)
WHERE NOT EXISTS (
  SELECT 1 FROM archive_transition_log a WHERE a.id = v.id::uuid
);
