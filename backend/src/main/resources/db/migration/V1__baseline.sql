-- Baseline migration (Epic 0.0 / ARCH-7). Establishes the extensions every
-- later migration relies on. Business schema (tenant/company/document
-- tables, RLS policies) lands starting with Epic 0.1 — every table added
-- from that point carries tenant_id and an RLS policy in the SAME
-- migration that creates it (ARCH-2).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
