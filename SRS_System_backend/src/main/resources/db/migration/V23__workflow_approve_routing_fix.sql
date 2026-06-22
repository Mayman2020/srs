-- V23: Fix workflow APPROVE transitions (multi-stop routing) + correspondence type process routes
SET search_path TO srs_system, public;

-- APPROVE must keep correspondence open until the process reaches a terminal end event.
UPDATE workflow_action_type w
SET next_correspondence_status_id = (
      SELECT id FROM correspondence_status s
      WHERE UPPER(s.code) = 'IN_PROGRESS' AND s.deleted_at IS NULL
      ORDER BY s.id LIMIT 1
    )
WHERE UPPER(w.code) = 'APPROVE'
  AND w.deleted_at IS NULL
  AND w.allowed_from_correspondence_status_id IS NULL;

-- APPROVE from NEW / PENDING_APPROVAL -> IN_PROGRESS (status-specific, preferred over wildcard)
INSERT INTO workflow_action_type (
  code, name_ar, name_en, sort_order, is_active,
  allowed_from_correspondence_status_id, next_correspondence_status_id,
  requires_comment, show_in_task_decision_ui, ui_variant)
SELECT 'APPROVE', 'اعتماد', 'Approve', 50, TRUE,
       s_from.id,
       s_to.id,
       FALSE, TRUE, 'primary'
FROM correspondence_status s_from
CROSS JOIN correspondence_status s_to
WHERE UPPER(s_from.code) = 'NEW'
  AND UPPER(s_to.code) = 'IN_PROGRESS'
  AND s_from.deleted_at IS NULL
  AND s_to.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM workflow_action_type w
    WHERE UPPER(w.code) = 'APPROVE'
      AND w.allowed_from_correspondence_status_id = s_from.id
      AND w.deleted_at IS NULL
  );

INSERT INTO workflow_action_type (
  code, name_ar, name_en, sort_order, is_active,
  allowed_from_correspondence_status_id, next_correspondence_status_id,
  requires_comment, show_in_task_decision_ui, ui_variant)
SELECT 'APPROVE', 'اعتماد', 'Approve', 50, TRUE,
       s_from.id,
       s_to.id,
       FALSE, TRUE, 'primary'
FROM correspondence_status s_from
CROSS JOIN correspondence_status s_to
WHERE UPPER(s_from.code) = 'PENDING_APPROVAL'
  AND UPPER(s_to.code) = 'IN_PROGRESS'
  AND s_from.deleted_at IS NULL
  AND s_to.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM workflow_action_type w
    WHERE UPPER(w.code) = 'APPROVE'
      AND w.allowed_from_correspondence_status_id = s_from.id
      AND w.deleted_at IS NULL
  );

-- IN_PROGRESS + APPROVE stays IN_PROGRESS during routing chain hops
INSERT INTO workflow_action_type (
  code, name_ar, name_en, sort_order, is_active,
  allowed_from_correspondence_status_id, next_correspondence_status_id,
  requires_comment, show_in_task_decision_ui, ui_variant)
SELECT 'APPROVE', 'اعتماد', 'Approve', 50, TRUE,
       s_from.id,
       s_to.id,
       FALSE, TRUE, 'primary'
FROM correspondence_status s_from
CROSS JOIN correspondence_status s_to
WHERE UPPER(s_from.code) = 'IN_PROGRESS'
  AND UPPER(s_to.code) = 'IN_PROGRESS'
  AND s_from.deleted_at IS NULL
  AND s_to.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM workflow_action_type w
    WHERE UPPER(w.code) = 'APPROVE'
      AND w.allowed_from_correspondence_status_id = s_from.id
      AND w.deleted_at IS NULL
  );

-- EXTERNAL correspondence uses inbound process (external org -> internal register/route)
UPDATE service_workflow_route swr
SET process_definition_key = 'inbound-correspondence',
    name_en = 'External — inbound route',
    name_ar = 'مسار خارجي — وارد'
FROM correspondence_type ct
WHERE swr.correspondence_type_id = ct.id
  AND UPPER(ct.code) = 'EXTERNAL'
  AND swr.deleted_at IS NULL
  AND ct.deleted_at IS NULL;

-- DECISION correspondence uses internal approval chain
UPDATE service_workflow_route swr
SET process_definition_key = 'internal-correspondence',
    name_en = 'Decision — internal route',
    name_ar = 'مسار قرار — داخلي'
FROM correspondence_type ct
WHERE swr.correspondence_type_id = ct.id
  AND UPPER(ct.code) = 'DECISION'
  AND swr.deleted_at IS NULL
  AND ct.deleted_at IS NULL;
