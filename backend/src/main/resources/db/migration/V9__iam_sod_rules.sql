-- IAM-7: Segregation-of-Duties — configurable pairs of permission codes
-- that must never both end up granted to the same user in the same
-- company (e.g. `procurement:supplier:create` + `finance:payment:approve`).
-- Pairs are normalized to (permission_code_a < permission_code_b) at write
-- time by SegregationOfDutiesService so the same conflict can't be stored
-- twice in reversed order.
CREATE TABLE sod_rules (
    id                uuid PRIMARY KEY,
    tenant_id         uuid NOT NULL,
    permission_code_a varchar(255) NOT NULL,
    permission_code_b varchar(255) NOT NULL,
    description       varchar(1000),
    active            boolean NOT NULL DEFAULT true,
    created_by        varchar(255),
    created_at        timestamptz,
    modified_by       varchar(255),
    modified_at       timestamptz,
    version           bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_sod_rules UNIQUE (tenant_id, permission_code_a, permission_code_b),
    CONSTRAINT chk_sod_rules_ordered CHECK (permission_code_a < permission_code_b)
);

CREATE INDEX idx_sod_rules_tenant_id ON sod_rules (tenant_id);

ALTER TABLE sod_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE sod_rules FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON sod_rules
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
