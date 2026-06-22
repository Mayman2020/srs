-- V24: align ui_screen permission FKs with canonical codes, fix nav gaps, RETURNED terminal
SET search_path TO srs_system, public;

-- Repoint shell nav rows that still reference legacy permission rows via permission_alias.
UPDATE ui_screen s
SET required_permission_id = pa.permission_id,
    updated_at = now()
FROM permission legacy
JOIN permission_alias pa ON UPPER(pa.alias_code) = UPPER(legacy.code)
WHERE s.required_permission_id = legacy.id
  AND s.deleted_at IS NULL
  AND legacy.deleted_at IS NULL
  AND s.required_permission_id IS DISTINCT FROM pa.permission_id;

-- Authority delegations route guard uses DELEGATION_MANAGE (not correspondence view).
UPDATE ui_screen
SET required_permission_id = (
      SELECT id FROM permission
      WHERE code = 'DELEGATION_MANAGE' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    updated_at = now()
WHERE code = 'delegations' AND deleted_at IS NULL;

-- In-app notifications inbox (route: /notifications).
UPDATE ui_screen
SET route_path = '/notifications',
    show_in_shell_nav = TRUE,
    icon_key = COALESCE(icon_key, 'notifications'),
    required_permission_id = (
      SELECT id FROM permission
      WHERE code = 'NOTIFICATION_VIEW' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    updated_at = now()
WHERE code = 'notifications' AND deleted_at IS NULL;

-- Letter templates admin (V22 used invalid column permission_code).
INSERT INTO ui_screen (
  code, route_path, name_ar, name_en, description, sort_order, is_active,
  icon_key, show_in_shell_nav, required_permission_id
)
SELECT
  'letter_templates_admin',
  '/admin/letter-templates',
  'قوالب الخطابات',
  'Letter templates',
  'Manage correspondence letter templates.',
  860,
  TRUE,
  'description',
  TRUE,
  (SELECT id FROM permission WHERE code = 'LETTER_TEMPLATE_MANAGE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'letter_templates_admin');

UPDATE ui_screen
SET route_path = '/admin/letter-templates',
    name_ar = 'قوالب الخطابات',
    name_en = 'Letter templates',
    sort_order = 860,
    is_active = TRUE,
    icon_key = COALESCE(icon_key, 'description'),
    show_in_shell_nav = TRUE,
    required_permission_id = (
      SELECT id FROM permission
      WHERE code = 'LETTER_TEMPLATE_MANAGE' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    updated_at = now()
WHERE code = 'letter_templates_admin' AND deleted_at IS NULL;

-- Camunda task delegation inbox (route: /task-delegations).
INSERT INTO ui_screen (
  code, route_path, name_ar, name_en, description, sort_order, is_active,
  icon_key, show_in_shell_nav, required_permission_id
)
SELECT
  'task_delegations',
  '/task-delegations',
  'تفويض المهام',
  'Task delegations',
  'Delegate workflow tasks to another user.',
  176,
  TRUE,
  'group_add',
  TRUE,
  (SELECT id FROM permission WHERE code = 'TASK_DELEGATION_MANAGE_OWN' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'task_delegations');

-- Admin communications hub should match AdminConsoleController (ADMIN_USER_MANAGE).
UPDATE ui_screen
SET required_permission_id = (
      SELECT id FROM permission
      WHERE code = 'ADMIN_USER_MANAGE' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    updated_at = now()
WHERE code = 'admin_main' AND deleted_at IS NULL;

-- RETURN ends the Camunda process; treat as terminal in correspondence lifecycle.
UPDATE correspondence_status
SET is_terminal = TRUE,
    allows_cancel = FALSE,
    updated_at = now()
WHERE UPPER(code) = 'RETURNED' AND deleted_at IS NULL;
