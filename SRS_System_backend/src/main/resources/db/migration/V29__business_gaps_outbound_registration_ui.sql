-- Business-gap closure: outbound delivery tracking + UI screen seeds.
SET search_path TO srs_system, public;

CREATE TABLE IF NOT EXISTS correspondence_outbound_delivery (
    id                  BIGSERIAL PRIMARY KEY,
    correspondence_id   UUID NOT NULL REFERENCES correspondence (id),
    channel_code        VARCHAR(32) NOT NULL,
    status_code         VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    recipient_label     VARCHAR(500),
    proof_reference     VARCHAR(256),
    notes               TEXT,
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID
);

CREATE INDEX IF NOT EXISTS ix_outbound_delivery_correspondence
    ON correspondence_outbound_delivery (correspondence_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_outbound_delivery_status
    ON correspondence_outbound_delivery (status_code)
    WHERE deleted_at IS NULL;

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'registration_desk', '/registration-desk', 'مكتب التسجيل', 'Registration desk',
       'Inbound/outbound registration desk with barcode labels',
       22, TRUE, 'fact_check', TRUE,
       (SELECT id FROM permission WHERE code = 'CORRESPONDENCE_CREATE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'registration_desk');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'outbound_delivery', '/outbound-delivery', 'تتبع الصادر', 'Outbound delivery',
       'Track outbound correspondence delivery status and proof',
       26, TRUE, 'local_shipping', TRUE,
       (SELECT id FROM permission WHERE code = 'CORRESPONDENCE_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'outbound_delivery');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'circular_read_report', '/circulars/read-report', 'تقرير قراءة التعميمات', 'Circular read report',
       'Acknowledgment and read-rate analytics per circular',
       46, TRUE, 'pie_chart', TRUE,
       (SELECT id FROM permission WHERE code = 'CORRESPONDENCE_CREATE' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'circular_read_report');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'attachment_access_log_report', '/admin/attachment-access-log', 'سجل تنزيل المرفقات', 'Attachment access log',
       'Cross-correspondence attachment download audit for auditors',
       355, TRUE, 'download', TRUE,
       (SELECT id FROM permission WHERE code = 'ATTACHMENT_ACCESS_LOG_VIEW' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'attachment_access_log_report');

INSERT INTO ui_screen (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
SELECT 'notification_catalog_admin', '/admin/notifications/catalog', 'كتالوج الإشعارات', 'Notification catalog',
       'Read-only catalog of notification event types and channels',
       365, TRUE, 'notifications_active', TRUE,
       (SELECT id FROM permission WHERE code = 'NOTIFICATION_CHANNEL_ADMIN' AND deleted_at IS NULL ORDER BY id LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM ui_screen WHERE code = 'notification_catalog_admin');
