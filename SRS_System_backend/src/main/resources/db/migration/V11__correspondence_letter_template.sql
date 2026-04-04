-- Letter templates for correspondence editor (loaded by API; replaces hardcoded frontend HTML).

CREATE TABLE correspondence_letter_template (
  id              BIGSERIAL PRIMARY KEY,
  code            VARCHAR(64)  NOT NULL,
  name_ar         VARCHAR(250) NOT NULL,
  name_en         VARCHAR(250) NOT NULL,
  body_html       TEXT         NOT NULL DEFAULT '',
  sort_order      INTEGER      NOT NULL DEFAULT 0,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  deleted_at      TIMESTAMPTZ,
  deleted_by      UUID,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      UUID,
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_by      UUID,
  CONSTRAINT ck_correspondence_letter_template_sort CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX ux_correspondence_letter_template_code_active
  ON correspondence_letter_template (code) WHERE deleted_at IS NULL;

COMMENT ON TABLE correspondence_letter_template IS 'Rich-text letter bodies for create/reply flows; keyed by stable code.';

INSERT INTO correspondence_letter_template (code, name_ar, name_en, body_html, sort_order) VALUES
  ('default', 'خطاب رسمي عام', 'Standard official letter',
   '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> …</p><p>السلام عليكم ورحمة الله وبركاته، وبعد:</p><p>نص الخطاب…</p>', 10),
  ('reminder', 'خطاب تذكير', 'Reminder letter',
   '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> تذكير</p><p>نود تذكيركم بالإفادة…</p>', 20),
  ('approval', 'خطاب موافقة', 'Approval letter',
   '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> إفادة بالموافقة</p><p>نفيدكم بالموافقة…</p>', 30),
  ('rejection', 'خطاب اعتذار / رفض', 'Rejection letter',
   '<p><strong>إلى:</strong> …</p><p><strong>الموضوع:</strong> اعتذار</p><p>نعتذر عن عدم إمكانية الموافقة…</p>', 40),
  ('admin-circular', 'تعميم إداري', 'Administrative circular',
   '<p style="text-align:center"><strong>تعميم إداري</strong></p><p>نص التعميم…</p>', 50),
  ('ministerial-circular', 'تعميم وزاري', 'Ministerial circular',
   '<p style="text-align:center"><strong>تعميم وزاري</strong></p><p>نص التعميم…</p>', 60),
  ('no-letter', 'بدون خطاب', 'No letter', '', 70);
