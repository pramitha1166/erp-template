-- IAM-1 / IAM-8: one row per issued refresh token. Only a SHA-256 hash of
-- the refresh token is stored (never the token itself), mirroring password
-- hashing practice — a leaked database row cannot be replayed as a bearer
-- token. Rotation chains via replaced_by_id so refresh-token reuse (a token
-- presented after it has already been rotated away) can be detected and
-- the whole chain revoked.
CREATE TABLE user_sessions (
    id                 uuid PRIMARY KEY,
    tenant_id          uuid NOT NULL,
    user_id            uuid NOT NULL REFERENCES users (id),
    refresh_token_hash varchar(64) NOT NULL,
    issued_at          timestamptz NOT NULL,
    expires_at         timestamptz NOT NULL,
    last_seen_at       timestamptz NOT NULL,
    ip_address         varchar(64),
    user_agent         varchar(512),
    revoked_at         timestamptz,
    replaced_by_id     uuid REFERENCES user_sessions (id),

    CONSTRAINT uq_user_sessions_token_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_user_sessions_tenant_id ON user_sessions (tenant_id);
CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);

ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON user_sessions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
