-- ADM-2: the tenant registry itself — closing the gap this epic exists
-- for (see docs/SRS.md §3.10). This row's own id IS the tenant_id every
-- other tenant-scoped table carries; like brands (V12), it carries no
-- tenant_id column and no RLS of its own.
CREATE TABLE tenants (
    id                     uuid PRIMARY KEY,
    brand_id               uuid NOT NULL REFERENCES brands (id),
    name                   varchar(255) NOT NULL,
    status                 varchar(32) NOT NULL DEFAULT 'ACTIVE',
    suspended_at           timestamptz,
    suspended_reason       varchar(500),
    primary_admin_user_id  uuid,
    primary_company_id     uuid,
    created_by             varchar(255),
    created_at             timestamptz,
    modified_by            varchar(255),
    modified_at            timestamptz,
    version                bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_tenants_brand_id ON tenants (brand_id);

-- ADM-6: presence of a row means the tenant is suspended. Lives in
-- config.tenancy (not the admin module's own Tenant table) because iam
-- must check this synchronously at login, and iam must never depend on
-- admin (that would make admin -> iam -> admin a module cycle, which
-- Spring Modulith's verify() rejects — see SuspendedTenantMarker's
-- javadoc). Keyed by tenant_id directly, RLS'd exactly like `users`
-- (V4): the caller-supplied tenant id at login time is exactly the scope
-- this check needs.
CREATE TABLE suspended_tenants (
    tenant_id     uuid PRIMARY KEY,
    suspended_at  timestamptz NOT NULL,
    reason        varchar(500)
);

ALTER TABLE suspended_tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE suspended_tenants FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON suspended_tenants
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
