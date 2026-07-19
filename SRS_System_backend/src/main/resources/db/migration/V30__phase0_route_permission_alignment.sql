-- Phase 0: keep route guards, capability catalogue, and shell navigation aligned.
SET search_path TO srs_system, public;

INSERT INTO permission (code, name_ar, name_en, sort_order)
SELECT 'LETTER_TEMPLATE_MANAGE', 'إدارة قوالب الخطابات', 'Manage letter templates', 860
WHERE NOT EXISTS (
  SELECT 1 FROM permission
  WHERE code = 'LETTER_TEMPLATE_MANAGE' AND deleted_at IS NULL
);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'LETTER_TEMPLATE_MANAGE' AND p.deleted_at IS NULL
WHERE r.code IN ('SYS_ADMIN', 'CORRESP_MGR')
  AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

UPDATE ui_screen
SET required_permission_id = (
      SELECT id FROM permission
      WHERE code = 'WORKFLOW_TASK_VIEW' AND deleted_at IS NULL
      ORDER BY id LIMIT 1
    ),
    updated_at = now()
WHERE code = 'workflow_tasks'
  AND deleted_at IS NULL;
