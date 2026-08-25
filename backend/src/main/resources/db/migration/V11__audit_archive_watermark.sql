-- AUD-5: bookkeeping for the retention/archival job (AuditArchiveService).
-- Deliberately a *separate*, ordinarily-mutable table — audit_log itself
-- stays strictly insert-only (AUD-3, V10). Rows in audit_log are never
-- deleted by archival: the 7-year minimum retention is trivially satisfied
-- by simply never deleting, and "archival to cold storage after 2 years"
-- is satisfied by ensuring a durable copy exists in the object store
-- (AuditArchiveProperties) — the hot table keeps serving reads (e.g. the
-- version-history API) for as long as the row lives there. This table
-- only tracks, per tenant, how far that cold-storage export has progressed.
CREATE TABLE audit_archive_watermark (
    tenant_id         uuid PRIMARY KEY,
    archived_through  timestamptz NOT NULL,
    last_object_key   varchar(512),
    updated_at        timestamptz NOT NULL,
    version           bigint NOT NULL DEFAULT 0
);

ALTER TABLE audit_archive_watermark ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_archive_watermark FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON audit_archive_watermark
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
