-- MDM-1 / ADM-3: a tenant's company. First created by Epic 0.11's
-- onboarding flow; full master-data management (multi-company nuances,
-- etc.) is Epic 0.6's own scope.
CREATE TABLE companies (
    id                       uuid PRIMARY KEY,
    tenant_id                uuid NOT NULL,
    legal_name               varchar(255) NOT NULL,
    registration_no          varchar(100),
    vat_no                   varchar(100),
    address                  varchar(500),
    base_currency            varchar(3) NOT NULL,
    fiscal_year_start_month  int NOT NULL,
    logo_url                 varchar(500),
    disabled                 boolean NOT NULL DEFAULT false,
    created_by               varchar(255),
    created_at               timestamptz,
    modified_by              varchar(255),
    modified_at              timestamptz,
    version                  bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_companies_tenant_id ON companies (tenant_id);

ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON companies
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-3 / ADM-3: a minimal, flat-tree Chart of Accounts — just what the
-- onboarding seed needs. Full hierarchical CoA management is Epic 0.6's
-- own scope.
CREATE TABLE accounts (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    company_id    uuid NOT NULL,
    code          varchar(20) NOT NULL,
    name          varchar(255) NOT NULL,
    account_type  varchar(32) NOT NULL,
    parent_id     uuid,
    is_group      boolean NOT NULL DEFAULT false,
    active        boolean NOT NULL DEFAULT true,
    created_at    timestamptz,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_accounts_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_accounts_tenant_id ON accounts (tenant_id);
CREATE INDEX idx_accounts_company_id ON accounts (company_id);

ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON accounts
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-9 / ADM-3: default fiscal year + monthly accounting periods.
CREATE TABLE fiscal_years (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL,
    company_id  uuid NOT NULL,
    name        varchar(100) NOT NULL,
    start_date  date NOT NULL,
    end_date    date NOT NULL,
    status      varchar(16) NOT NULL DEFAULT 'OPEN',
    version     bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_fiscal_years_tenant_id ON fiscal_years (tenant_id);
CREATE INDEX idx_fiscal_years_company_id ON fiscal_years (company_id);

ALTER TABLE fiscal_years ENABLE ROW LEVEL SECURITY;
ALTER TABLE fiscal_years FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON fiscal_years
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE accounting_periods (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    company_id     uuid NOT NULL,
    fiscal_year_id uuid NOT NULL REFERENCES fiscal_years (id),
    name           varchar(100) NOT NULL,
    start_date     date NOT NULL,
    end_date       date NOT NULL,
    status         varchar(16) NOT NULL DEFAULT 'OPEN',
    version        bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_accounting_periods_tenant_id ON accounting_periods (tenant_id);
CREATE INDEX idx_accounting_periods_fiscal_year_id ON accounting_periods (fiscal_year_id);

ALTER TABLE accounting_periods ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounting_periods FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON accounting_periods
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
