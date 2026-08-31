-- ADM-5: a Tenant's entitlement override, bounded by its Brand's own
-- entitlement (enforced in EntitlementService, not here). Tenant-owned
-- data — ordinary RLS applies, unlike brands/tenants/platform_entitlement_
-- defaults/brand_entitlements (V12/V13).
CREATE TABLE tenant_entitlements (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    feature_code  varchar(100) NOT NULL,
    enabled       boolean NOT NULL DEFAULT false,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_tenant_entitlements_tenant_feature UNIQUE (tenant_id, feature_code)
);

CREATE INDEX idx_tenant_entitlements_tenant_id ON tenant_entitlements (tenant_id);

ALTER TABLE tenant_entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_entitlements FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenant_entitlements
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ADM-4: the fixed six-item post-onboarding setup checklist.
CREATE TABLE onboarding_checklist_items (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    item_key      varchar(50) NOT NULL,
    completed     boolean NOT NULL DEFAULT false,
    completed_at  timestamptz,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_onboarding_checklist_items_tenant_key UNIQUE (tenant_id, item_key)
);

CREATE INDEX idx_onboarding_checklist_items_tenant_id ON onboarding_checklist_items (tenant_id);

ALTER TABLE onboarding_checklist_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE onboarding_checklist_items FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON onboarding_checklist_items
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ADM-7: time-boxed platform/brand-admin-as-tenant-admin sessions.
CREATE TABLE impersonation_sessions (
    id              uuid PRIMARY KEY,
    tenant_id       uuid NOT NULL,
    actor_user_id   uuid NOT NULL,
    target_user_id  uuid NOT NULL,
    reason          varchar(500) NOT NULL,
    started_at      timestamptz NOT NULL,
    expires_at      timestamptz NOT NULL,
    ended_at        timestamptz,
    status          varchar(16) NOT NULL DEFAULT 'ACTIVE',
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_impersonation_sessions_tenant_id ON impersonation_sessions (tenant_id);

ALTER TABLE impersonation_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE impersonation_sessions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON impersonation_sessions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ADM-8 / NFR-S7 / NFR-D5: PDPA data export/erasure requests.
CREATE TABLE data_subject_requests (
    id              uuid PRIMARY KEY,
    tenant_id       uuid NOT NULL,
    type            varchar(16) NOT NULL,
    status          varchar(16) NOT NULL DEFAULT 'PENDING',
    requested_by    varchar(255) NOT NULL,
    notes           varchar(1000),
    requested_at    timestamptz NOT NULL,
    completed_at    timestamptz,
    result_payload  jsonb,
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_data_subject_requests_tenant_id ON data_subject_requests (tenant_id);

ALTER TABLE data_subject_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_subject_requests FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON data_subject_requests
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- ADM-5 / BRD-14: brand-admin invite flow for additional/replacement tenant-admin users.
CREATE TABLE tenant_admin_invites (
    id           uuid PRIMARY KEY,
    tenant_id    uuid NOT NULL,
    email        varchar(255) NOT NULL,
    token_hash   varchar(64) NOT NULL,
    status       varchar(16) NOT NULL DEFAULT 'PENDING',
    invited_by   varchar(255) NOT NULL,
    expires_at   timestamptz NOT NULL,
    accepted_at  timestamptz,
    version      bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_tenant_admin_invites_token UNIQUE (token_hash)
);

CREATE INDEX idx_tenant_admin_invites_tenant_id ON tenant_admin_invites (tenant_id);

ALTER TABLE tenant_admin_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_admin_invites FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tenant_admin_invites
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
