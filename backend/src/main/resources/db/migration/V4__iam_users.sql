-- IAM-1 / IAM-2 / IAM-9: user accounts. Email/password auth with Argon2id
-- hashing (password_hash/password_algo), optional TOTP 2FA (totp_secret is
-- only ever set once totp_enabled flips true — see TotpService), and the
-- account-lockout bookkeeping login attempts use.
--
-- tenant_id is required per ARCH-2, but note that the *login* lookup by
-- email necessarily runs before a session has proven which tenant the
-- caller belongs to: until Epic 0.9 gives tenants a resolvable identity
-- (subdomain, org code, ...), the login request itself supplies tenant_id
-- and the RLS policy below still enforces that a stolen/guessed tenant_id
-- can only ever authenticate against that tenant's own users — it cannot
-- read or affect any other tenant's rows. This mirrors how company_id and
-- branch_id are opaque, caller-supplied identifiers elsewhere in Phase 0.
CREATE TABLE users (
    id                    uuid PRIMARY KEY,
    tenant_id             uuid NOT NULL,
    email                 varchar(255) NOT NULL,
    password_hash         varchar(255) NOT NULL,
    password_algo         varchar(32) NOT NULL DEFAULT 'ARGON2ID',
    password_changed_at   timestamptz NOT NULL,
    totp_secret           varchar(64),
    totp_enabled          boolean NOT NULL DEFAULT false,
    status                varchar(32) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts int NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    created_by            varchar(255),
    created_at            timestamptz,
    modified_by           varchar(255),
    modified_at           timestamptz,
    version               bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
