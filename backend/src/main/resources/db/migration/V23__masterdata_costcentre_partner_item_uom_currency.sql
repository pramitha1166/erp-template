-- Epic 0.6 (PLAT-MDM): the master-data tables not already covered by V14
-- (companies / a flat CoA / fiscal years, seeded for onboarding under
-- Epic 0.11). MDM-10: every table here is disable-only from the app layer
-- -- there is no delete endpoint, only a `disabled` flag flip once a
-- record may be referenced.

-- MDM-4: hierarchical cost centres.
CREATE TABLE cost_centres (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    company_id    uuid NOT NULL,
    code          varchar(20) NOT NULL,
    name          varchar(255) NOT NULL,
    parent_id     uuid REFERENCES cost_centres (id),
    disabled      boolean NOT NULL DEFAULT false,
    created_by    varchar(255),
    created_at    timestamptz,
    modified_by   varchar(255),
    modified_at   timestamptz,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_cost_centres_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_cost_centres_tenant_id ON cost_centres (tenant_id);
CREATE INDEX idx_cost_centres_company_id ON cost_centres (company_id);
CREATE INDEX idx_cost_centres_parent_id ON cost_centres (parent_id);

ALTER TABLE cost_centres ENABLE ROW LEVEL SECURITY;
ALTER TABLE cost_centres FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON cost_centres
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-5: Customer / Supplier master.
CREATE TABLE business_partners (
    id                       uuid PRIMARY KEY,
    tenant_id                uuid NOT NULL,
    company_id               uuid NOT NULL,
    partner_type             varchar(16) NOT NULL,
    code                     varchar(20) NOT NULL,
    name                     varchar(255) NOT NULL,
    tax_registration_no      varchar(100),
    credit_limit             numeric(20, 4) NOT NULL DEFAULT 0,
    credit_terms_days        int NOT NULL DEFAULT 0,
    default_account_id       uuid REFERENCES accounts (id),
    bank_name                varchar(255),
    bank_branch              varchar(255),
    bank_account_no          varchar(100),
    bank_swift_code          varchar(20),
    disabled                 boolean NOT NULL DEFAULT false,
    created_by               varchar(255),
    created_at               timestamptz,
    modified_by              varchar(255),
    modified_at              timestamptz,
    version                  bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_business_partners_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_business_partners_tenant_id ON business_partners (tenant_id);
CREATE INDEX idx_business_partners_company_id ON business_partners (company_id);
CREATE INDEX idx_business_partners_partner_type ON business_partners (partner_type);

ALTER TABLE business_partners ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_partners FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON business_partners
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE business_partner_contacts (
    id                uuid PRIMARY KEY,
    tenant_id         uuid NOT NULL,
    partner_id        uuid NOT NULL REFERENCES business_partners (id),
    name              varchar(255) NOT NULL,
    designation       varchar(100),
    phone             varchar(50),
    email             varchar(255),
    primary_contact   boolean NOT NULL DEFAULT false,
    created_at        timestamptz,
    version           bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_business_partner_contacts_tenant_id ON business_partner_contacts (tenant_id);
CREATE INDEX idx_business_partner_contacts_partner_id ON business_partner_contacts (partner_id);

ALTER TABLE business_partner_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_partner_contacts FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON business_partner_contacts
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-6: hierarchical item groups.
CREATE TABLE item_groups (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    company_id    uuid NOT NULL,
    code          varchar(20) NOT NULL,
    name          varchar(255) NOT NULL,
    parent_id     uuid REFERENCES item_groups (id),
    disabled      boolean NOT NULL DEFAULT false,
    created_by    varchar(255),
    created_at    timestamptz,
    modified_by   varchar(255),
    modified_at   timestamptz,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_item_groups_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_item_groups_tenant_id ON item_groups (tenant_id);
CREATE INDEX idx_item_groups_company_id ON item_groups (company_id);
CREATE INDEX idx_item_groups_parent_id ON item_groups (parent_id);

ALTER TABLE item_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE item_groups FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON item_groups
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-7: units of measure, shared across a tenant's companies, plus
-- pairwise conversion factors (e.g. purchase UOM "BOX" -> stock UOM "NOS").
CREATE TABLE units_of_measure (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    code          varchar(20) NOT NULL,
    name          varchar(255) NOT NULL,
    disabled      boolean NOT NULL DEFAULT false,
    created_by    varchar(255),
    created_at    timestamptz,
    modified_by   varchar(255),
    modified_at   timestamptz,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_units_of_measure_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_units_of_measure_tenant_id ON units_of_measure (tenant_id);

ALTER TABLE units_of_measure ENABLE ROW LEVEL SECURITY;
ALTER TABLE units_of_measure FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON units_of_measure
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE uom_conversions (
    id                  uuid PRIMARY KEY,
    tenant_id           uuid NOT NULL,
    from_uom_id         uuid NOT NULL REFERENCES units_of_measure (id),
    to_uom_id           uuid NOT NULL REFERENCES units_of_measure (id),
    conversion_factor   numeric(20, 6) NOT NULL,
    created_at          timestamptz,
    version             bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_uom_conversions_pair UNIQUE (from_uom_id, to_uom_id),
    CONSTRAINT ck_uom_conversions_distinct CHECK (from_uom_id <> to_uom_id)
);

CREATE INDEX idx_uom_conversions_tenant_id ON uom_conversions (tenant_id);

ALTER TABLE uom_conversions ENABLE ROW LEVEL SECURITY;
ALTER TABLE uom_conversions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON uom_conversions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-6: item master.
CREATE TABLE items (
    id                  uuid PRIMARY KEY,
    tenant_id           uuid NOT NULL,
    company_id          uuid NOT NULL,
    code                varchar(50) NOT NULL,
    name                varchar(255) NOT NULL,
    item_group_id       uuid NOT NULL REFERENCES item_groups (id),
    stock_uom_id        uuid NOT NULL REFERENCES units_of_measure (id),
    purchase_uom_id     uuid REFERENCES units_of_measure (id),
    valuation_method    varchar(20) NOT NULL,
    reorder_level       numeric(20, 4) NOT NULL DEFAULT 0,
    batch_tracked       boolean NOT NULL DEFAULT false,
    serial_tracked      boolean NOT NULL DEFAULT false,
    tax_category_code   varchar(50),
    hs_code             varchar(20),
    disabled            boolean NOT NULL DEFAULT false,
    created_by          varchar(255),
    created_at          timestamptz,
    modified_by         varchar(255),
    modified_at         timestamptz,
    version             bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_items_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_items_tenant_id ON items (tenant_id);
CREATE INDEX idx_items_company_id ON items (company_id);
CREATE INDEX idx_items_item_group_id ON items (item_group_id);

ALTER TABLE items ENABLE ROW LEVEL SECURITY;
ALTER TABLE items FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON items
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- MDM-8: currency master + date-effective exchange rates.
CREATE TABLE currencies (
    id               uuid PRIMARY KEY,
    tenant_id        uuid NOT NULL,
    code             varchar(3) NOT NULL,
    name             varchar(100) NOT NULL,
    symbol           varchar(10),
    decimal_places   int NOT NULL DEFAULT 2,
    disabled         boolean NOT NULL DEFAULT false,
    created_by       varchar(255),
    created_at       timestamptz,
    modified_by      varchar(255),
    modified_at      timestamptz,
    version          bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_currencies_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_currencies_tenant_id ON currencies (tenant_id);

ALTER TABLE currencies ENABLE ROW LEVEL SECURITY;
ALTER TABLE currencies FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON currencies
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE exchange_rates (
    id               uuid PRIMARY KEY,
    tenant_id        uuid NOT NULL,
    currency_code    varchar(3) NOT NULL,
    rate_date        date NOT NULL,
    rate_to_base     numeric(20, 6) NOT NULL,
    source           varchar(16) NOT NULL DEFAULT 'MANUAL',
    created_at       timestamptz,
    version          bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_exchange_rates_tenant_currency_date UNIQUE (tenant_id, currency_code, rate_date)
);

CREATE INDEX idx_exchange_rates_tenant_id ON exchange_rates (tenant_id);
CREATE INDEX idx_exchange_rates_currency_code ON exchange_rates (currency_code);

ALTER TABLE exchange_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE exchange_rates FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON exchange_rates
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
