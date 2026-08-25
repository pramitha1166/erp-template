-- NUM-1 / ADM-3: default naming series seeded at onboarding. Allocation
-- (NUM-2 gapless sequencing, NUM-4 concurrency safety) is Epic 0.5's own
-- scope and isn't exercised yet — next_counter is tracked for that future
-- work, not read by anything in this epic.
CREATE TABLE numbering_series (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    company_id     uuid NOT NULL,
    doc_type       varchar(100) NOT NULL,
    prefix         varchar(20) NOT NULL,
    counter_width  int NOT NULL,
    next_counter   bigint NOT NULL DEFAULT 1,
    active         boolean NOT NULL DEFAULT true,
    version        bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_numbering_series_company_doctype UNIQUE (company_id, doc_type)
);

CREATE INDEX idx_numbering_series_tenant_id ON numbering_series (tenant_id);
CREATE INDEX idx_numbering_series_company_id ON numbering_series (company_id);

ALTER TABLE numbering_series ENABLE ROW LEVEL SECURITY;
ALTER TABLE numbering_series FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON numbering_series
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
