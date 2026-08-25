-- IAM-5: field-level permissions. entity_code follows the `module:entity`
-- half of a permission triple (e.g. `payroll:employee`); field_name is the
-- entity's field (e.g. `salary`). No row for a given (role, entity_code,
-- field_name) means the field-level engine defers to the role's ordinary
-- module:entity:action permissions rather than restricting it — an explicit
-- row is what narrows access below that (e.g. to READ, or NONE).
CREATE TABLE field_permissions (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL,
    role_id     uuid NOT NULL REFERENCES roles (id),
    entity_code varchar(255) NOT NULL,
    field_name  varchar(255) NOT NULL,
    access      varchar(16) NOT NULL,

    CONSTRAINT uq_field_permissions UNIQUE (role_id, entity_code, field_name)
);

CREATE INDEX idx_field_permissions_tenant_id ON field_permissions (tenant_id);
CREATE INDEX idx_field_permissions_role_entity ON field_permissions (role_id, entity_code);

ALTER TABLE field_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE field_permissions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON field_permissions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- IAM-6: record-level scoping by warehouse/cost-centre/branch. A role with
-- no rows for a given scope_type is unrestricted on that dimension (sees
-- every record); one or more rows narrows visibility to exactly those
-- scope_value ids. scope_value is opaque until Epic 0.9 (Branch) and
-- whichever epics introduce Warehouse/Cost Centre masters — coordinated
-- per this task's note in the SRS.
CREATE TABLE record_scope_restrictions (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL,
    role_id     uuid NOT NULL REFERENCES roles (id),
    scope_type  varchar(32) NOT NULL,
    scope_value uuid NOT NULL,

    CONSTRAINT uq_record_scope_restrictions UNIQUE (role_id, scope_type, scope_value)
);

CREATE INDEX idx_record_scope_restrictions_tenant_id ON record_scope_restrictions (tenant_id);
CREATE INDEX idx_record_scope_restrictions_role_type ON record_scope_restrictions (role_id, scope_type);

ALTER TABLE record_scope_restrictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE record_scope_restrictions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON record_scope_restrictions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
