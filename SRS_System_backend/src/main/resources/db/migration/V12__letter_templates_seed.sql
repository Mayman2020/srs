-- =============================================================================
-- V12__letter_templates_seed.sql
--
-- Fix mis-encoded Arabic in V14 letter template seeds and add per-correspondence-
-- type templates so the create / reply UI can pick a sensible default
-- (INBOUND, OUTBOUND, INTERNAL, CIRCULAR, DECISION).
-- =============================================================================

SET search_path TO srs_system, public;

-- Repair mis-encoded Arabic on existing seeds (V14 wrote replacement characters).
UPDATE correspondence_letter_template SET name_ar = 'خطاب رسمي عام',
       body_html = '<p><strong>إلى:</strong> ...</p>'
                || '<p><strong>الموضوع:</strong> ...</p>'
                || '<p>السلام عليكم ورحمة الله وبركاته، وبعد:</p>'
                || '<p>نص الخطاب ...</p>'
  WHERE code = 'default' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'خطاب تذكير',
       body_html = '<p><strong>إلى:</strong> ...</p>'
                || '<p><strong>الموضوع:</strong> تذكير</p>'
                || '<p>نود تذكيركم بالإفادة ...</p>'
  WHERE code = 'reminder' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'خطاب موافقة',
       body_html = '<p><strong>إلى:</strong> ...</p>'
                || '<p><strong>الموضوع:</strong> إفادة بالموافقة</p>'
                || '<p>نفيدكم بالموافقة على ...</p>'
  WHERE code = 'approval' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'خطاب اعتذار / رفض',
       body_html = '<p><strong>إلى:</strong> ...</p>'
                || '<p><strong>الموضوع:</strong> اعتذار</p>'
                || '<p>نعتذر عن عدم إمكانية الموافقة على ...</p>'
  WHERE code = 'rejection' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'تعميم إداري',
       body_html = '<p style="text-align:center"><strong>تعميم إداري</strong></p>'
                || '<p>نص التعميم ...</p>'
  WHERE code = 'admin-circular' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'تعميم وزاري',
       body_html = '<p style="text-align:center"><strong>تعميم وزاري</strong></p>'
                || '<p>نص التعميم ...</p>'
  WHERE code = 'ministerial-circular' AND deleted_at IS NULL;

UPDATE correspondence_letter_template SET name_ar = 'بدون خطاب'
  WHERE code = 'no-letter' AND deleted_at IS NULL;

-- ----------------------------------------------------------------------------
-- New type-specific templates.
-- ----------------------------------------------------------------------------
INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order)
SELECT 'inbound-acknowledge',
       'إقرار استلام مراسلة واردة',
       'Inbound acknowledgment',
       '<p><strong>إلى:</strong> ...</p>'
    || '<p><strong>الموضوع:</strong> إقرار استلام</p>'
    || '<p>نفيدكم باستلام مراسلتكم رقم (...) المؤرخة في (...).</p>'
    || '<p>وسيتم الرد عليكم بعد إنهاء الإجراءات اللازمة.</p>',
       80
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template
  WHERE code = 'inbound-acknowledge' AND deleted_at IS NULL
);

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order)
SELECT 'outbound-cover',
       'خطاب صادر — صفحة الغلاف',
       'Outbound cover letter',
       '<p style="text-align:right"><strong>المملكة العربية السعودية</strong></p>'
    || '<p style="text-align:right">وزارة الدفاع</p>'
    || '<p><strong>إلى:</strong> ...</p>'
    || '<p><strong>الموضوع:</strong> ...</p>'
    || '<p>السلام عليكم ورحمة الله وبركاته،</p>'
    || '<p>نتشرف بإفادة سعادتكم بـ ...</p>'
    || '<p>وتقبلوا فائق التحية والتقدير،،</p>',
       90
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template
  WHERE code = 'outbound-cover' AND deleted_at IS NULL
);

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order)
SELECT 'internal-memo',
       'مذكرة داخلية',
       'Internal memo',
       '<p><strong>من:</strong> ...</p>'
    || '<p><strong>إلى:</strong> ...</p>'
    || '<p><strong>التاريخ:</strong> ...</p>'
    || '<p><strong>الموضوع:</strong> ...</p>'
    || '<hr/>'
    || '<p>نص المذكرة الداخلية ...</p>',
       100
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template
  WHERE code = 'internal-memo' AND deleted_at IS NULL
);

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order)
SELECT 'broadcast-circular',
       'تعميم عام',
       'Broadcast circular',
       '<p style="text-align:center"><strong>تعميم</strong></p>'
    || '<p style="text-align:center">إلى كافة الإدارات والوحدات</p>'
    || '<p>نص التعميم ...</p>',
       110
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template
  WHERE code = 'broadcast-circular' AND deleted_at IS NULL
);

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order)
SELECT 'admin-decision',
       'قرار إداري',
       'Administrative decision',
       '<p style="text-align:center"><strong>قرار إداري رقم (...) لعام (...)</strong></p>'
    || '<p>بناءً على الصلاحيات الممنوحة، وبعد الاطلاع على ...</p>'
    || '<p style="text-align:center"><strong>تقرر ما يلي:</strong></p>'
    || '<p>أولاً: ...</p>'
    || '<p>ثانياً: ...</p>',
       120
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template
  WHERE code = 'admin-decision' AND deleted_at IS NULL
);

-- ----------------------------------------------------------------------------
-- Map letter templates to a default correspondence_type via a mapping table
-- (no DB ENUM; admin can override per environment).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS correspondence_letter_template_type_default (
  id                            BIGSERIAL PRIMARY KEY,
  correspondence_type_id        BIGINT NOT NULL REFERENCES correspondence_type (id) ON DELETE CASCADE,
  correspondence_letter_template_id BIGINT NOT NULL REFERENCES correspondence_letter_template (id) ON DELETE CASCADE,
  is_default                    BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order                    INTEGER NOT NULL DEFAULT 0,
  created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                    UUID,
  updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by                    UUID,
  CONSTRAINT uq_letter_template_type UNIQUE (correspondence_type_id, correspondence_letter_template_id)
);

COMMENT ON TABLE correspondence_letter_template_type_default IS
  'Per-correspondence-type default letter templates; multiple selectable, exactly one default per type.';

-- One default template per correspondence type.
CREATE UNIQUE INDEX IF NOT EXISTS ux_letter_template_one_default_per_type
  ON correspondence_letter_template_type_default (correspondence_type_id)
  WHERE is_default = TRUE;

-- Seed pairings.
INSERT INTO correspondence_letter_template_type_default
       (correspondence_type_id, correspondence_letter_template_id, is_default, sort_order)
SELECT ct.id, lt.id, v.is_default, v.sort_order
FROM (VALUES
  ('INBOUND',  'inbound-acknowledge', TRUE,  10),
  ('INBOUND',  'default',             FALSE, 20),
  ('OUTBOUND', 'outbound-cover',      TRUE,  10),
  ('OUTBOUND', 'default',             FALSE, 20),
  ('OUTBOUND', 'reminder',            FALSE, 30),
  ('INTERNAL', 'internal-memo',       TRUE,  10),
  ('INTERNAL', 'default',             FALSE, 20),
  ('CIRCULAR', 'broadcast-circular',  TRUE,  10),
  ('CIRCULAR', 'admin-circular',      FALSE, 20),
  ('CIRCULAR', 'ministerial-circular',FALSE, 30),
  ('DECISION', 'admin-decision',      TRUE,  10),
  ('EXTERNAL', 'outbound-cover',      TRUE,  10),
  ('EXTERNAL', 'default',             FALSE, 20)
) AS v(type_code, template_code, is_default, sort_order)
JOIN correspondence_type ct ON UPPER(ct.code) = UPPER(v.type_code) AND ct.deleted_at IS NULL
JOIN correspondence_letter_template lt ON lt.code = v.template_code AND lt.deleted_at IS NULL
WHERE NOT EXISTS (
  SELECT 1 FROM correspondence_letter_template_type_default existing
  WHERE existing.correspondence_type_id = ct.id
    AND existing.correspondence_letter_template_id = lt.id
);
