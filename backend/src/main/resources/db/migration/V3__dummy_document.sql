-- Reference document table exercising the ARCH-2..ARCH-6 framework end to
-- end (Phase 0 gate criterion: "a dummy document completes full
-- lifecycle"). Not a real business document — Phase 1 introduces the first
-- ones (JournalEntry etc.). branch_id is NOT NULL from this first migration
-- per ORG-2, even though the Branch master itself doesn't land until Epic
-- 0.9; it is an opaque identifier until then.
CREATE TABLE dummy_documents (
    id              uuid PRIMARY KEY,
    tenant_id       uuid NOT NULL,
    company_id      uuid NOT NULL,
    branch_id       uuid NOT NULL,
    doc_number      varchar(64),
    doc_status      varchar(32) NOT NULL,
    posting_date    date NOT NULL,
    amended_from_id uuid REFERENCES dummy_documents (id),
    note            varchar(1000),
    created_by      varchar(255),
    created_at      timestamptz,
    modified_by     varchar(255),
    modified_at     timestamptz,
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_dummy_documents_tenant_id ON dummy_documents (tenant_id);

-- ARCH-2: RLS policy lands in the same migration that creates the table.
-- FORCE is required in addition to ENABLE: without it, the table's owning
-- role (the Flyway/migration role) bypasses RLS entirely, which would
-- silently defeat this on every environment where the app connects as the
-- role that ran the migrations. A genuine Postgres superuser bypasses RLS
-- regardless of FORCE — true only of local/dev bootstrap roles, never of
-- an RDS master user in staging/production.
ALTER TABLE dummy_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE dummy_documents FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON dummy_documents
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
