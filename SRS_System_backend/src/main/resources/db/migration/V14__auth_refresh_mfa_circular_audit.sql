-- Auth: MFA flag, refresh tokens, OTP challenges
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    jti UUID NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_token (expires_at);

CREATE TABLE IF NOT EXISTS mfa_otp_challenge (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id),
    channel VARCHAR(32) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mfa_otp_user ON mfa_otp_challenge (user_id);

-- Communication: circulars (multi-recipient + broadcast)
CREATE TABLE IF NOT EXISTS circular (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    is_broadcast BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS circular_recipient (
    circular_id UUID NOT NULL REFERENCES circular (id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    read_at TIMESTAMPTZ,
    PRIMARY KEY (circular_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_circular_created_at ON circular (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_circular_recipient_user ON circular_recipient (user_id);

-- Audit: generic user-action trail (in addition to role_switch_audit)
CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id VARCHAR(255) NOT NULL,
    action_code VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128),
    resource_id VARCHAR(255),
    detail_json TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_audit_event_actor_time ON audit_event (actor_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_action ON audit_event (action_code, occurred_at DESC);
