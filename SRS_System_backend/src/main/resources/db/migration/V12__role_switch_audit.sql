-- Audit trail for JWT active-role switches (enterprise accountability).
CREATE TABLE IF NOT EXISTS role_switch_audit (
    id BIGSERIAL PRIMARY KEY,
    app_user_id UUID NOT NULL REFERENCES app_user (id),
    old_role_code VARCHAR(100),
    new_role_code VARCHAR(100) NOT NULL,
    switched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_role_switch_audit_user ON role_switch_audit (app_user_id);
CREATE INDEX IF NOT EXISTS idx_role_switch_audit_time ON role_switch_audit (switched_at DESC);
