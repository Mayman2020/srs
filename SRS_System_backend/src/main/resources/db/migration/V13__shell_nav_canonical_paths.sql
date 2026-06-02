-- ===================================================================
-- V13 — Canonical UI screen paths and Arabic labels
-- ===================================================================
-- The original baseline V1 seed inserted ui_screen rows with mojibake
-- Arabic strings (UTF-8 → Latin-1 round-trip) and with the legacy
-- "/transactions/…" route paths. The Angular shell now serves
-- correspondence under "/correspondence/…" with legacy redirects in
-- place, but the persisted shell navigation still pointed users to the
-- legacy URLs, defeating their purpose. This migration:
--
--   1. fixes the Arabic labels for the core shell entries to readable
--      UTF-8 text;
--   2. updates {@code route_path} to the canonical "/correspondence/…"
--      paths so the API-driven sidebar links go straight to the new
--      routes (legacy URLs continue to resolve via the Angular
--      redirects defined in {@code app.routes.ts});
--   3. backfills the Q/L/K/S routing admin screen and the workflow
--      task inbox so they appear in the shell sidebar for users that
--      have the matching permissions.
-- ===================================================================

UPDATE srs_system.ui_screen
SET
    route_path = '/correspondence',
    name_ar    = 'المعاملات',
    name_en    = 'Correspondence',
    updated_at = now()
WHERE code = 'transactions';

UPDATE srs_system.ui_screen
SET
    route_path = '/correspondence/list/:type',
    name_ar    = 'قائمة المعاملات',
    name_en    = 'Correspondence list',
    updated_at = now()
WHERE code = 'transactions_list';

UPDATE srs_system.ui_screen
SET
    route_path = '/correspondence/create',
    name_ar    = 'إنشاء معاملة',
    name_en    = 'New correspondence',
    updated_at = now()
WHERE code = 'create_transaction';

UPDATE srs_system.ui_screen
SET
    route_path = '/correspondence/:id',
    name_ar    = 'تفاصيل المعاملة',
    name_en    = 'Correspondence details',
    updated_at = now()
WHERE code = 'transaction_details';

UPDATE srs_system.ui_screen
SET
    route_path = '/correspondence/supply',
    name_ar    = 'توريد معاملة',
    name_en    = 'Supply correspondence',
    updated_at = now()
WHERE code = 'supply_transaction';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'لوحة التحكم',
    updated_at = now()
WHERE code = 'dashboard';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'التقارير',
    updated_at = now()
WHERE code = 'reports';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'الإشعارات',
    updated_at = now()
WHERE code = 'notifications';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'إدارة النظام',
    updated_at = now()
WHERE code = 'admin_main';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'التعاميم',
    updated_at = now()
WHERE code = 'circulars';

UPDATE srs_system.ui_screen
SET
    name_ar    = 'الإجازات',
    updated_at = now()
WHERE code = 'leave_requests';

-- ----- new shell entries: Q/L/K/S levels and workflow task inbox -----

INSERT INTO srs_system.ui_screen
    (code, route_path, name_ar, name_en, description, sort_order, is_active,
     icon_key, show_in_shell_nav, required_permission_id)
VALUES
    ('org_levels',
     '/org-levels',
     'المستويات التنظيمية',
     'Organizational levels',
     'Q / L / K / S routing hierarchy (read-only catalog).',
     65,
     TRUE,
     'account_tree',
     TRUE,
     (SELECT id FROM srs_system.permission
       WHERE code = 'ADMIN_ORG_MANAGE' AND deleted_at IS NULL
       ORDER BY id LIMIT 1)),
    ('workflow_tasks',
     '/workflow-tasks',
     'صندوق المهام',
     'Workflow task inbox',
     'Open Camunda tasks for the current user.',
     22,
     TRUE,
     'assignment',
     TRUE,
     (SELECT id FROM srs_system.permission
       WHERE code = 'CORRESPONDENCE_VIEW' AND deleted_at IS NULL
       ORDER BY id LIMIT 1)),
    ('audit_events',
     '/audit-events',
     'سجل التدقيق',
     'Audit events',
     'Server-side audit trail.',
     90,
     TRUE,
     'fact_check',
     TRUE,
     (SELECT id FROM srs_system.permission
       WHERE code = 'ADMIN_AUDIT_VIEW' AND deleted_at IS NULL
       ORDER BY id LIMIT 1)),
    ('sms_dispatch',
     '/sms-dispatch',
     'إرسال SMS',
     'Send SMS',
     'Outbound SMS dispatch.',
     55,
     TRUE,
     'sms',
     TRUE,
     (SELECT id FROM srs_system.permission
       WHERE code = 'NOTIFICATION_DISPATCH' AND deleted_at IS NULL
       ORDER BY id LIMIT 1)),
    ('circular_create',
     '/circulars/create',
     'إنشاء تعميم',
     'Create circular',
     'Compose a new circular / broadcast.',
     48,
     TRUE,
     'campaign',
     FALSE,
     (SELECT id FROM srs_system.permission
       WHERE code = 'CORRESPONDENCE_CREATE' AND deleted_at IS NULL
       ORDER BY id LIMIT 1))
ON CONFLICT (code) DO UPDATE SET
    route_path             = EXCLUDED.route_path,
    name_ar                = EXCLUDED.name_ar,
    name_en                = EXCLUDED.name_en,
    description            = EXCLUDED.description,
    sort_order             = EXCLUDED.sort_order,
    is_active              = EXCLUDED.is_active,
    show_in_shell_nav      = EXCLUDED.show_in_shell_nav,
    icon_key               = EXCLUDED.icon_key,
    required_permission_id = EXCLUDED.required_permission_id,
    updated_at             = now();
