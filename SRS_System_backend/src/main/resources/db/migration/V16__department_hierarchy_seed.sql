-- Sample organizational hierarchy under ROOT for department tree UI (demo / dev alignment with legacy app).

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_HR',
       'الإدارة العامة للموارد البشرية',
       'General Directorate of Human Resources',
       d.id,
       10
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_HR' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_IT',
       'الإدارة العامة لتقنية المعلومات',
       'General Directorate of Information Technology',
       d.id,
       20
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_IT' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'GD_FIN',
       'الإدارة العامة للشؤون المالية',
       'General Directorate of Financial Affairs',
       d.id,
       30
FROM department d
WHERE d.code = 'ROOT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'GD_FIN' AND x.deleted_at IS NULL)
LIMIT 1;

-- HR children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'HR_RECRUIT',
       'قسم التوظيف',
       'Recruitment',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_HR'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'HR_RECRUIT' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'HR_TRAIN',
       'قسم التدريب والتطوير',
       'Training and Development',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_HR'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'HR_TRAIN' AND x.deleted_at IS NULL)
LIMIT 1;

-- IT children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'IT_INFRA',
       'قسم البنية التحتية',
       'Infrastructure',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_IT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'IT_INFRA' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'IT_CYBER',
       'قسم الأمن السيبراني',
       'Cybersecurity',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_IT'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'IT_CYBER' AND x.deleted_at IS NULL)
LIMIT 1;

-- Finance children
INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'FIN_BUDGET',
       'قسم الميزانية',
       'Budget',
       d.id,
       10
FROM department d
WHERE d.code = 'GD_FIN'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'FIN_BUDGET' AND x.deleted_at IS NULL)
LIMIT 1;

INSERT INTO department (code, name_ar, name_en, parent_id, sort_order)
SELECT 'FIN_ACCT',
       'قسم الحسابات',
       'Accounts',
       d.id,
       20
FROM department d
WHERE d.code = 'GD_FIN'
  AND d.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM department x WHERE x.code = 'FIN_ACCT' AND x.deleted_at IS NULL)
LIMIT 1;
