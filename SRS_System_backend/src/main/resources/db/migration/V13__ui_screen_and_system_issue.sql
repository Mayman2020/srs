-- UI screen registry for RBAC documentation / future route guards (admin CRUD).
CREATE TABLE IF NOT EXISTS ui_screen (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(128)  NOT NULL,
    route_path      VARCHAR(512)  NOT NULL,
    name_ar         VARCHAR(200)  NOT NULL,
    name_en         VARCHAR(200)  NOT NULL,
    description     TEXT,
    sort_order      INTEGER       NOT NULL DEFAULT 0,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      UUID,
    CONSTRAINT uq_ui_screen_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS ix_ui_screen_route ON ui_screen (route_path) WHERE deleted_at IS NULL;

COMMENT ON TABLE ui_screen IS 'Register of application screens/routes for administration (SRS RBAC / UI inventory).';

-- Client and server captured problems for admin triage.
CREATE TABLE IF NOT EXISTS system_issue (
    id              BIGSERIAL PRIMARY KEY,
    source          VARCHAR(16)   NOT NULL,
    severity        VARCHAR(16)   NOT NULL DEFAULT 'ERROR',
    message         TEXT          NOT NULL,
    detail          TEXT,
    page_url        VARCHAR(2000),
    user_id         UUID          REFERENCES app_user (id) ON DELETE SET NULL,
    http_status     INTEGER,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    resolved_by     UUID          REFERENCES app_user (id) ON DELETE SET NULL,
    resolution_note TEXT,
    CONSTRAINT ck_system_issue_source CHECK (source IN ('CLIENT', 'SERVER'))
);

CREATE INDEX IF NOT EXISTS ix_system_issue_created ON system_issue (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_system_issue_open ON system_issue (created_at DESC) WHERE resolved_at IS NULL;

INSERT INTO ui_screen (code, route_path, name_ar, name_en, sort_order)
VALUES
    ('dashboard', '/dashboard', 'لوحة التحكم', 'Dashboard', 10),
    ('transactions', '/transactions', 'إدارة المعاملات', 'Transactions', 20),
    ('create_transaction', '/create-transaction', 'إنشاء معاملة', 'Create transaction', 30),
    ('reports', '/reports', 'التقارير', 'Reports', 40),
    ('notifications', '/notifications', 'الإشعارات', 'Notifications', 50),
    ('admin_main', '/admin-communications-main', 'إدارة النظام', 'System administration', 60)
ON CONFLICT (code) DO NOTHING;
