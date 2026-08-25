-- AUD-1 / AUD-2: append-only audit trail. Every insert/update/delete on a
-- transactional or master-data entity lands a row here — either via the
-- generic AuditingInterceptor (see AuditInterceptorConfig, hooks every
-- Hibernate flush across every module without per-module boilerplate) or
-- via AuthAuditEventListener translating IAM-10's auth/permission/role
-- events into rows here. entity_id is varchar rather than uuid so the
-- audit table isn't coupled to every entity using uuid identifiers.
--
-- AUD-3: immutable once written, enforced two independent ways:
--   1. The triggers below unconditionally reject UPDATE/DELETE. RLS's
--      FORCE ROW LEVEL SECURITY (below) only restricts row *visibility*
--      per tenant — it does not stop the owning role from mutating rows it
--      can see, so a hard trigger is what actually closes that gap, for
--      every role including the table owner.
--   2. AuditLogRepository (Spring Data) never declares an update, delete,
--      or deleteById method; the only write path in application code is
--      the plain JDBC INSERT in AuditLogWriter.
CREATE TABLE audit_log (
    id            uuid PRIMARY KEY,
    tenant_id     uuid NOT NULL,
    entity_type   varchar(255) NOT NULL,
    entity_id     varchar(255) NOT NULL,
    action        varchar(16) NOT NULL,
    changes       jsonb NOT NULL,
    actor         varchar(255) NOT NULL,
    occurred_at   timestamptz NOT NULL,
    ip_address    varchar(64),
    request_id    varchar(64)
);

CREATE INDEX idx_audit_log_tenant_entity ON audit_log (tenant_id, entity_type, entity_id, occurred_at);
CREATE INDEX idx_audit_log_tenant_occurred_at ON audit_log (tenant_id, occurred_at);

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON audit_log
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE FUNCTION audit_log_reject_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % is not permitted (AUD-3)', TG_OP;
END;
$$;

CREATE TRIGGER audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_reject_mutation();

CREATE TRIGGER audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_reject_mutation();
