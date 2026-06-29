-- Slice 7 — UI screens for workflow routes admin, org routing preview, email dispatch.
SET search_path TO srs_system, public;

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'workflow_routes_admin', '/admin/workflow-routes', 'مسارات سير العمل', 'Workflow routes',
       'Admin CRUD for service_workflow_route catalog',
       345, TRUE, 'route', TRUE,
       (SELECT id FROM permission WHERE code = 'ADMIN_USER_MANAGE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'workflow_routes_admin');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'org_routing_preview', '/org-structure/routing-preview', 'معاينة التوجيه', 'Routing preview',
       'Preview Q/L/K/S routing chain between departments',
       155, TRUE, 'share', TRUE,
       (SELECT id FROM permission WHERE code = 'CORRESPONDENCE_CREATE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'org_routing_preview');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'email_dispatch', '/email-dispatch', 'إرسال بريد', 'Email dispatch',
       'Manual outbound email dispatch',
       185, TRUE, 'mail', TRUE,
       (SELECT id FROM permission WHERE code = 'NOTIFICATION_DISPATCH' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'email_dispatch');
