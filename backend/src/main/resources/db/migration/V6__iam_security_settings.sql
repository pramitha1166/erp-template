-- IAM-8 / IAM-9: per-tenant configurable security policy — session idle
-- timeout, and password length/complexity/history/expiry. One row per
-- tenant; PasswordPolicyService/SessionService fall back to safe defaults
-- (hardcoded in TenantSecuritySettingsService) when a tenant has not yet
-- provisioned a row, so a brand-new tenant is never left unprotected.
CREATE TABLE tenant_security_settings (
    id                     uuid PRIMARY KEY,
    tenant_id              uuid NOT NULL,
    idle_timeout_minutes   int NOT NULL DEFAULT 30,
    password_min_length    int NOT NULL DEFAULT 10,
    password_require_upper boolean NOT NULL DEFAULT true,
    password_require_lower boolean NOT NULL DEFAULT true,
    password_require_digit boolean NOT NULL DEFAULT true,
    password_require_symbol boolean NOT NULL DEFAULT false,
    password_history_count int NOT NULL DEFAULT 3,
    password_expiry_days   int,
    created_by             varchar(255),
    created_at             timestamptz,
    modified_by            varchar(255),
    modified_at            timestamptz,
    version                bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_tenant_security_settings_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_tenant_security_settings_tenant_id ON tenant_security_settings (tenant_id);

ALTER TABLE tenant_security_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_security_settings FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenant_security_settings
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- IAM-9: last N password hashes per user, checked so a user cannot reuse a
-- recent password. Insert-only — history is never edited or pruned except
-- by the policy engine reading only the most recent history_count rows.
CREATE TABLE password_history (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    user_id       uuid NOT NULL REFERENCES users (id),
    password_hash varchar(255) NOT NULL,
    created_at    timestamptz NOT NULL
);

CREATE INDEX idx_password_history_tenant_id ON password_history (tenant_id);
CREATE INDEX idx_password_history_user_id ON password_history (user_id);

ALTER TABLE password_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_history FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON password_history
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
