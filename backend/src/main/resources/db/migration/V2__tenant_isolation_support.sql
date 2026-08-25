-- ARCH-2: shared helper every tenant-scoped table's RLS policy uses. Reads
-- the `app.tenant_id` session variable that TenantAwareDataSource stamps on
-- every connection checkout. NULLIF(..., '') maps "not set" to SQL NULL
-- rather than a cast error on an empty string, and comparing tenant_id to
-- NULL is never true — so a connection with no tenant context set sees
-- (and can write) zero rows, which is the safe, fail-closed default.
CREATE FUNCTION current_tenant_id() RETURNS uuid
LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::uuid;
$$;
