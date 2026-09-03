-- Epic 0.7 (PLAT-DOC): generic file attachments (DOC-1) and print-format
-- templates (DOC-2), attachable/configurable against any document type
-- across modules.
--
-- `document_type` is a `module:entity` pair — the same shape as the first
-- two segments of an IAM PermissionCode (e.g. `sales:invoice`) — naming
-- which owning module/entity `document_id` belongs to. The documents
-- module never reaches into another module's tables to resolve this
-- (ARCH-1 forbids that); instead DOC-5's access check derives
-- `document_type + ":view"` / `":manage"` and asks IAM's PermissionApi
-- directly, exactly mirroring how every other module's own REST layer
-- already checks permissions (see e.g. MasterDataAccessControl).

CREATE TABLE attachments (
    id                uuid PRIMARY KEY,
    tenant_id         uuid NOT NULL,
    company_id        uuid NOT NULL,
    document_type     varchar(100) NOT NULL,
    document_id       uuid NOT NULL,
    file_name         varchar(255) NOT NULL,
    content_type      varchar(255) NOT NULL,
    size_bytes        bigint NOT NULL,
    storage_key       varchar(512) NOT NULL,
    checksum_sha256   varchar(64) NOT NULL,
    -- DOC-4: PENDING (scanning not configured in this environment), CLEAN,
    -- INFECTED, FAILED. An infected upload is rejected before it is ever
    -- written to object storage or this table (see AttachmentService), so
    -- INFECTED/FAILED rows are not expected in steady state but the column
    -- allows for a future async scan pipeline without a schema change.
    scan_status       varchar(20) NOT NULL DEFAULT 'PENDING',
    scan_message      varchar(500),
    uploaded_by       varchar(255),
    uploaded_at       timestamptz,
    version           bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_attachments_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_attachments_tenant_id ON attachments (tenant_id);
CREATE INDEX idx_attachments_document ON attachments (document_type, document_id);

ALTER TABLE attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON attachments
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- DOC-2: configurable print-format templates per document type. The
-- template itself (Thymeleaf XHTML, merged with document data and
-- rendered to PDF server-side — DOC-3) lives in `template_content`.
CREATE TABLE print_formats (
    id                uuid PRIMARY KEY,
    tenant_id         uuid NOT NULL,
    company_id        uuid NOT NULL,
    document_type     varchar(100) NOT NULL,
    name              varchar(255) NOT NULL,
    is_default        boolean NOT NULL DEFAULT false,
    template_content  text NOT NULL,
    disabled          boolean NOT NULL DEFAULT false,
    created_by        varchar(255),
    created_at        timestamptz,
    modified_by       varchar(255),
    modified_at       timestamptz,
    version           bigint NOT NULL DEFAULT 0,

    CONSTRAINT uq_print_formats_company_doctype_name UNIQUE (company_id, document_type, name)
);

CREATE INDEX idx_print_formats_tenant_id ON print_formats (tenant_id);
CREATE INDEX idx_print_formats_company_doctype ON print_formats (company_id, document_type);

ALTER TABLE print_formats ENABLE ROW LEVEL SECURITY;
ALTER TABLE print_formats FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON print_formats
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- At most one enabled default print format per company + document type.
CREATE UNIQUE INDEX uq_print_formats_one_default
    ON print_formats (company_id, document_type)
    WHERE is_default = true AND disabled = false;
