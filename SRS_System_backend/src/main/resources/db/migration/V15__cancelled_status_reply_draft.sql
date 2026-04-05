-- Cancelled lifecycle + editor draft storage for replies
INSERT INTO correspondence_status (correspondence_type_id, code, name_ar, name_en, sort_order, is_terminal)
SELECT NULL, 'CANCELLED', 'ملغاة', 'Cancelled', 95, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM correspondence_status cs
    WHERE UPPER(cs.code) = 'CANCELLED' AND cs.deleted_at IS NULL
);

ALTER TABLE correspondence
    ADD COLUMN IF NOT EXISTS reply_draft_html TEXT;

COMMENT ON COLUMN correspondence.reply_draft_html IS 'Unpublished reply/editor HTML; cleared when reply is sent.';
