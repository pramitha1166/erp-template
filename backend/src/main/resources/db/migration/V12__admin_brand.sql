-- ADM-1 / BRD-12: Brand registry and its entitlement config. These three
-- tables deliberately carry NO tenant_id and NO RLS — a Brand sits above
-- every Tenant (it's the thing that PROVISIONS tenants), so scoping it by
-- "which tenant" would be a category error, not a missed ARCH-2
-- obligation. Access is enforced purely by the admin:platform:manage
-- permission check in the service layer (AdminAccessGuard), the same way
-- a permission check — not RLS — already gates every operation IAM's own
-- RBAC tables don't naturally scope (e.g. AccessControlService).
CREATE TABLE brands (
    id                uuid PRIMARY KEY,
    name              varchar(255) NOT NULL,
    legal_name        varchar(255),
    support_email     varchar(255),
    status            varchar(32) NOT NULL DEFAULT 'ACTIVE',
    suspended_at      timestamptz,
    suspended_reason  varchar(500),
    created_by        varchar(255),
    created_at        timestamptz,
    modified_by       varchar(255),
    modified_at       timestamptz,
    version           bigint NOT NULL DEFAULT 0
);

-- ADM-1: a platform-wide default a Brand inherits unless it has its own
-- brand_entitlements override (BRD-12).
CREATE TABLE platform_entitlement_defaults (
    id            uuid PRIMARY KEY,
    feature_code  varchar(100) NOT NULL,
    enabled       boolean NOT NULL DEFAULT false,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_platform_entitlement_defaults_feature UNIQUE (feature_code)
);

CREATE TABLE brand_entitlements (
    id            uuid PRIMARY KEY,
    brand_id      uuid NOT NULL REFERENCES brands (id),
    feature_code  varchar(100) NOT NULL,
    enabled       boolean NOT NULL DEFAULT false,
    version       bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_brand_entitlements_brand_feature UNIQUE (brand_id, feature_code)
);

CREATE INDEX idx_brand_entitlements_brand_id ON brand_entitlements (brand_id);
