-- IAM-3 / IAM-4: roles are named, tenant-scoped bundles of permissions.
-- Permissions themselves are not a catalog table — module/entity/action
-- triples (e.g. `finance:journal-entry:submit`) are validated for shape by
-- PermissionCode in application code, not by a foreign key, because the
-- entities most triples name (JournalEntry, StockLedgerEntry, ...) do not
-- exist yet in Phase 0; a hard FK to a not-yet-built catalog would block
-- this epic on Phase 1+ modules. RoleService.grantPermission is the single
-- write path and is where format validation actually happens.
CREATE TABLE roles (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL,
    name        varchar(255) NOT NULL,
    description varchar(1000),
    created_by  varchar(255),
    created_at  timestamptz,
    modified_by varchar(255),
    modified_at timestamptz,
    version     bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles (tenant_id);

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON roles
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE role_permissions (
    id              uuid PRIMARY KEY,
    tenant_id       uuid NOT NULL,
    role_id         uuid NOT NULL REFERENCES roles (id),
    permission_code varchar(255) NOT NULL,
    created_at      timestamptz NOT NULL,

    CONSTRAINT uq_role_permissions_role_code UNIQUE (role_id, permission_code)
);

CREATE INDEX idx_role_permissions_tenant_id ON role_permissions (tenant_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);

ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permissions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON role_permissions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- IAM-4: a user may hold different roles in different companies of the
-- same tenant. company_id is an opaque caller-supplied identifier until
-- Epic 0.9 introduces the Company master, same convention as
-- Document.companyId.
CREATE TABLE user_company_roles (
    id         uuid PRIMARY KEY,
    tenant_id  uuid NOT NULL,
    user_id    uuid NOT NULL REFERENCES users (id),
    company_id uuid NOT NULL,
    role_id    uuid NOT NULL REFERENCES roles (id),
    created_at timestamptz NOT NULL,

    CONSTRAINT uq_user_company_roles UNIQUE (user_id, company_id, role_id)
);

CREATE INDEX idx_user_company_roles_tenant_id ON user_company_roles (tenant_id);
CREATE INDEX idx_user_company_roles_user_company ON user_company_roles (user_id, company_id);

ALTER TABLE user_company_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_company_roles FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON user_company_roles
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
