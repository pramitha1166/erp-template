-- ADM-5 / ADM-7: an outbound notification record (invite emails,
-- suspension/impersonation notices, ...). tenant_id is nullable — a
-- platform-level notice (e.g. to Eudext operators) has no owning tenant —
-- so the RLS policy additionally allows any session to see a null-tenant
-- row: those rows hold no one tenant's business data, so cross-tenant
-- visibility of them isn't a leak the way it would be for any other table
-- here.
CREATE TABLE notification_outbox (
    id               uuid PRIMARY KEY,
    tenant_id        uuid,
    recipient_email  varchar(255) NOT NULL,
    template_code    varchar(100) NOT NULL,
    payload          jsonb,
    status           varchar(16) NOT NULL DEFAULT 'PENDING',
    created_at       timestamptz NOT NULL,
    sent_at          timestamptz,
    failure_reason   varchar(1000),
    version          bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_outbox_tenant_id ON notification_outbox (tenant_id);

ALTER TABLE notification_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_outbox FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON notification_outbox
    USING (tenant_id = current_tenant_id() OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_tenant_id() OR tenant_id IS NULL);
