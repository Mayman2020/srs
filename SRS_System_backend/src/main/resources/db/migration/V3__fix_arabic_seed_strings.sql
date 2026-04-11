-- Fix Arabic display: earlier seed data stored mis-encoded UTF-8 as wrong characters. This migration sets correct Arabic for known codes.
SET search_path TO srs_system, public;

-- ---------------------------------------------------------------------------
-- workflow_history_event_type
-- ---------------------------------------------------------------------------
UPDATE workflow_history_event_type SET name_ar = 'إجراء مستخدم' WHERE code = 'USER_ACTION' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'حدث نظام' WHERE code = 'SYSTEM_EVENT' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تغيير حالة' WHERE code = 'STATUS_CHANGE' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'معلم SLA' WHERE code = 'SLA_MILESTONE' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تجاوز SLA' WHERE code = 'SLA_BREACH' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تعيين مهمة' WHERE code = 'TASK_ASSIGNED' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'إكمال مهمة' WHERE code = 'TASK_COMPLETED' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تعليق' WHERE code = 'COMMENT' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تفويض' WHERE code = 'DELEGATION' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'تصعيد' WHERE code = 'ESCALATION' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'انتقال سير عمل' WHERE code = 'CAMUNDA_TRANSITION' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'مرفق' WHERE code = 'ATTACHMENT' AND deleted_at IS NULL;
UPDATE workflow_history_event_type SET name_ar = 'ارتباط معاملة' WHERE code = 'CORRESPONDENCE_LINK' AND deleted_at IS NULL;

UPDATE workflow_history_event_type SET name_ar = 'إنشاء معاملة' WHERE code = 'CREATE' AND sort_order = 5 AND deleted_at IS NULL;

UPDATE workflow_action_type SET name_ar = 'إنشاء' WHERE code = 'CREATE' AND sort_order = 5 AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- ui_screen (shell + routes)
-- ---------------------------------------------------------------------------
UPDATE ui_screen SET name_ar = 'لوحة التحكم' WHERE code = 'dashboard' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'المعاملات' WHERE code = 'transactions' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'إنشاء معاملة' WHERE code = 'create_transaction' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'التقارير' WHERE code = 'reports' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'الإشعارات' WHERE code = 'notifications' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'إدارة النظام' WHERE code = 'admin_main' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'تسجيل الدخول' WHERE code = 'login' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'قائمة المعاملات' WHERE code = 'transactions_list' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'تفاصيل المعاملة' WHERE code = 'transaction_details' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'التعاميم' WHERE code = 'circulars' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'المستخدمون' WHERE code = 'users' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'الأدوار والصلاحيات' WHERE code = 'roles' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'الملف الشخصي' WHERE code = 'profile' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'الهيكل التنظيمي' WHERE code = 'org_structure' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'الإجازات' WHERE code = 'leave_requests' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'توريد معاملة' WHERE code = 'supply_transaction' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'بحث المعاملات' WHERE code = 'correspondence_search' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'التفويضات الإدارية' WHERE code = 'delegations' AND deleted_at IS NULL;
UPDATE ui_screen SET name_ar = 'إدارة قوائم النظام' WHERE code = 'lookup_admin' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- lookup_catalog
-- ---------------------------------------------------------------------------
UPDATE lookup_catalog SET name_ar = 'نوع المعاملة' WHERE lookup_code = 'correspondence_type';
UPDATE lookup_catalog SET name_ar = 'حالة المعاملة' WHERE lookup_code = 'correspondence_status';
UPDATE lookup_catalog SET name_ar = 'الأولوية' WHERE lookup_code = 'priority';
UPDATE lookup_catalog SET name_ar = 'درجة السرية' WHERE lookup_code = 'confidentiality';
UPDATE lookup_catalog SET name_ar = 'التصنيف' WHERE lookup_code = 'classification';
UPDATE lookup_catalog SET name_ar = 'نوع إجراء المسار' WHERE lookup_code = 'workflow_action_type';
UPDATE lookup_catalog SET name_ar = 'نوع حدث السجل' WHERE lookup_code = 'workflow_history_event_type';

UPDATE lookup_catalog SET name_ar = 'حالة عقدة المسار البصري' WHERE lookup_code = 'org_visual_node_status';

-- ---------------------------------------------------------------------------
-- permission (leave / delegation / capability codes)
-- ---------------------------------------------------------------------------
UPDATE permission SET name_ar = 'تقديم طلب إجازة' WHERE code = 'leave.self' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'إدارة طلبات الإجازات' WHERE code = 'leave.admin' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'إدارة التفويض الإداري' WHERE code = 'delegation.manage' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'عرض لوحة التحكم' WHERE code = 'VIEW_DASHBOARD' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'عرض المعاملات' WHERE code = 'VIEW_TRANSACTIONS' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'إنشاء معاملة' WHERE code = 'CREATE_TRANSACTION' AND deleted_at IS NULL;
UPDATE permission SET name_ar = 'إلغاء معاملة' WHERE code = 'CANCEL_TRANSACTION' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Demo roles (if seeded)
-- ---------------------------------------------------------------------------
UPDATE role SET name_ar = 'مسؤول' WHERE code = 'ADMIN' AND deleted_at IS NULL;
UPDATE role SET name_ar = 'مستخدم' WHERE code = 'USER' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- org_visual_node_status
-- ---------------------------------------------------------------------------
UPDATE org_visual_node_status SET name_ar = 'مكتمل' WHERE code = 'done' AND deleted_at IS NULL;
UPDATE org_visual_node_status SET name_ar = 'جاري' WHERE code = 'active' AND deleted_at IS NULL;
UPDATE org_visual_node_status SET name_ar = 'معلق' WHERE code = 'pending' AND deleted_at IS NULL;
UPDATE org_visual_node_status SET name_ar = 'معلومات' WHERE code = 'info' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Global CANCELLED correspondence status (seeded without type)
-- ---------------------------------------------------------------------------
UPDATE correspondence_status SET name_ar = 'ملغاة' WHERE UPPER(code) = 'CANCELLED' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- correspondence_letter_template (titles + HTML bodies)
-- ---------------------------------------------------------------------------
UPDATE correspondence_letter_template SET
  name_ar = 'خطاب رسمي عام',
  body_html = '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> …</p><p>السلام عليكم ورحمة الله وبركاته، وبعد:</p><p>نص الخطاب…</p>'
WHERE code = 'default' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'خطاب تذكير',
  body_html = '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> تذكير</p><p>نود تذكيركم بالإفادة…</p>'
WHERE code = 'reminder' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'خطاب موافقة',
  body_html = '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> إفادة بالموافقة</p><p>نفيدكم بالموافقة…</p>'
WHERE code = 'approval' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'خطاب اعتذار / رفض',
  body_html = '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> اعتذار</p><p>نعتذر عن عدم إمكانية الموافقة…</p>'
WHERE code = 'rejection' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'تعميم إداري',
  body_html = '<p style="text-align:center"><strong>تعميم إداري</strong></p><p>نص التعميم…</p>'
WHERE code = 'admin-circular' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'تعميم وزاري',
  body_html = '<p style="text-align:center"><strong>تعميم وزاري</strong></p><p>نص التعميم…</p>'
WHERE code = 'ministerial-circular' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET
  name_ar = 'بدون خطاب',
  body_html = ''
WHERE code = 'no-letter' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Department names (org chart seed — codes from baseline)
-- ---------------------------------------------------------------------------
UPDATE department SET name_ar = 'الإدارة العامة للموارد البشرية' WHERE code = 'GD_HR' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'الإدارة العامة لتقنية المعلومات' WHERE code = 'GD_IT' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'الإدارة العامة للشؤون المالية' WHERE code = 'GD_FIN' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم التوظيف' WHERE code = 'HR_RECRUIT' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم التدريب والتطوير' WHERE code = 'HR_TRAIN' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم البنية التحتية' WHERE code = 'IT_INFRA' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم الأمن السيبراني' WHERE code = 'IT_CYBER' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم الميزانية' WHERE code = 'FIN_BUDGET' AND deleted_at IS NULL;
UPDATE department SET name_ar = 'قسم الحسابات' WHERE code = 'FIN_ACCT' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- service_workflow_route (Camunda path labels per correspondence type)
-- ---------------------------------------------------------------------------
UPDATE service_workflow_route SET name_ar = 'مسار وارد — Camunda' WHERE process_definition_key = 'inbound-correspondence' AND deleted_at IS NULL;
UPDATE service_workflow_route SET name_ar = 'مسار صادر — Camunda' WHERE process_definition_key = 'outbound-correspondence' AND deleted_at IS NULL;
UPDATE service_workflow_route SET name_ar = 'مسار داخلي — Camunda' WHERE process_definition_key = 'internal-correspondence' AND deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Bootstrap admin display name
-- ---------------------------------------------------------------------------
UPDATE app_user SET full_name_ar = 'مدير النظام' WHERE username = 'admin' AND deleted_at IS NULL;
