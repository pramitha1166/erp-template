-- PLAT-WF (Epic 0.4): configurable multi-level approval chains (WF-1),
-- condition-gated steps (WF-2), role/named-user/reporting-hierarchy
-- approver resolution (WF-3), sequential + parallel step execution
-- (WF-4), delegation and timeout escalation (WF-5), and an append-only
-- decision history (WF-7). Every table follows the ARCH-2 RLS pattern
-- used throughout Phase 0; cross-module ids (company_id, branch_id,
-- assigned_user_id, ...) stay opaque uuids, same convention as
-- Document.companyId (ARCH-1: no FK out of this module's tables).

-- WF-1: one chain per document type per company; only one may be active
-- at a time (the partial unique index below), but earlier chains are kept
-- (not deleted) so in-flight instances still resolve the chain they
-- started under.
CREATE TABLE approval_chains (
    id           uuid PRIMARY KEY,
    tenant_id    uuid NOT NULL,
    company_id   uuid NOT NULL,
    document_type varchar(100) NOT NULL,
    name         varchar(255) NOT NULL,
    active       boolean NOT NULL DEFAULT true,
    created_by   varchar(255),
    created_at   timestamptz,
    modified_by  varchar(255),
    modified_at  timestamptz,
    version      bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_approval_chains_tenant_id ON approval_chains (tenant_id);
CREATE UNIQUE INDEX uq_approval_chains_active ON approval_chains (company_id, document_type) WHERE active;

ALTER TABLE approval_chains ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_chains FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_chains
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- WF-3 / WF-4: an ordered step within a chain. Steps sharing the same
-- sequence_order form a parallel group -- every step in the group must be
-- satisfied before the instance advances to the next (higher)
-- sequence_order. approver_type selects which of approver_role_id /
-- approver_user_id / hierarchy_level is read (WF-3); escalation_* mirrors
-- the same three-way resolution for WF-5's timeout escalation target, and
-- is optional (a step with escalation_hours null never times out).
CREATE TABLE approval_steps (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    chain_id       uuid NOT NULL REFERENCES approval_chains (id),
    sequence_order int NOT NULL,
    name           varchar(255) NOT NULL,
    approver_type  varchar(16) NOT NULL CHECK (approver_type IN ('ROLE', 'USER', 'HIERARCHY')),
    approver_role_id uuid,
    approver_user_id uuid,
    hierarchy_level  int,
    escalation_hours int,
    escalation_type  varchar(16) CHECK (escalation_type IN ('ROLE', 'USER', 'HIERARCHY')),
    escalation_role_id uuid,
    escalation_user_id uuid,
    escalation_hierarchy_level int,
    created_by     varchar(255),
    created_at     timestamptz,
    modified_by    varchar(255),
    modified_at    timestamptz,
    version        bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_approval_steps_tenant_id ON approval_steps (tenant_id);
CREATE INDEX idx_approval_steps_chain_id ON approval_steps (chain_id, sequence_order);

ALTER TABLE approval_steps ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_steps FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_steps
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- WF-2: a step applies to an instance only if every one of its conditions
-- matches the submitted document's field values (AND semantics; a step
-- with no conditions always applies). value_number is NUMERIC, never
-- float/double (ARCH-5), for threshold comparisons like "amount > 500000".
CREATE TABLE approval_step_conditions (
    id           uuid PRIMARY KEY,
    tenant_id    uuid NOT NULL,
    step_id      uuid NOT NULL REFERENCES approval_steps (id),
    field_name   varchar(100) NOT NULL,
    operator     varchar(8) NOT NULL CHECK (operator IN ('EQ', 'NE', 'GT', 'GTE', 'LT', 'LTE')),
    value_string varchar(255),
    value_number numeric(20, 4),
    created_at   timestamptz NOT NULL
);

CREATE INDEX idx_approval_step_conditions_tenant_id ON approval_step_conditions (tenant_id);
CREATE INDEX idx_approval_step_conditions_step_id ON approval_step_conditions (step_id);

ALTER TABLE approval_step_conditions ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_step_conditions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_step_conditions
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- WF-5: a user may delegate their approval authority to another user for a
-- date range (e.g. while on leave). A delegation only ever widens who may
-- act on the delegator's already-assigned tasks -- it never reassigns them.
CREATE TABLE approval_delegations (
    id                uuid PRIMARY KEY,
    tenant_id         uuid NOT NULL,
    delegator_user_id uuid NOT NULL,
    delegate_user_id  uuid NOT NULL,
    start_date        date NOT NULL,
    end_date          date NOT NULL,
    reason            varchar(500),
    revoked           boolean NOT NULL DEFAULT false,
    created_by        varchar(255),
    created_at        timestamptz,
    modified_by       varchar(255),
    modified_at       timestamptz,
    version           bigint NOT NULL DEFAULT 0,

    CONSTRAINT ck_approval_delegations_date_range CHECK (end_date >= start_date),
    CONSTRAINT ck_approval_delegations_distinct_users CHECK (delegator_user_id <> delegate_user_id)
);

CREATE INDEX idx_approval_delegations_tenant_id ON approval_delegations (tenant_id);
CREATE INDEX idx_approval_delegations_delegator ON approval_delegations (delegator_user_id, start_date, end_date);

ALTER TABLE approval_delegations ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_delegations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_delegations
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- A running approval process for one document (document_type/document_id
-- are the opaque pair every Document subtype already carries). At most one
-- PENDING instance may exist per document at a time (the partial unique
-- index below); earlier resolved instances are kept for WF-7 history.
CREATE TABLE workflow_instances (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    company_id     uuid NOT NULL,
    branch_id      uuid,
    document_type  varchar(100) NOT NULL,
    document_id    uuid NOT NULL,
    chain_id       uuid NOT NULL REFERENCES approval_chains (id),
    status         varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    current_sequence_order int,
    submitted_by   uuid NOT NULL,
    created_at     timestamptz NOT NULL,
    modified_at    timestamptz,
    version        bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_workflow_instances_tenant_id ON workflow_instances (tenant_id);
CREATE INDEX idx_workflow_instances_document ON workflow_instances (document_type, document_id);
CREATE UNIQUE INDEX uq_workflow_instances_pending ON workflow_instances (document_type, document_id) WHERE status = 'PENDING';

ALTER TABLE workflow_instances ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_instances FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON workflow_instances
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- A snapshot, taken once when the instance starts, of exactly which chain
-- steps applied (WF-2 conditions evaluated against the field values
-- supplied at that moment). Group-advancement logic reads this instead of
-- re-evaluating conditions later, when the original field values are no
-- longer available.
CREATE TABLE workflow_instance_steps (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    instance_id    uuid NOT NULL REFERENCES workflow_instances (id),
    step_id        uuid NOT NULL REFERENCES approval_steps (id),
    sequence_order int NOT NULL,
    created_at     timestamptz NOT NULL
);

CREATE INDEX idx_workflow_instance_steps_tenant_id ON workflow_instance_steps (tenant_id);
CREATE INDEX idx_workflow_instance_steps_instance_id ON workflow_instance_steps (instance_id, sequence_order);

ALTER TABLE workflow_instance_steps ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflow_instance_steps FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON workflow_instance_steps
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- WF-4: one actionable task per resolved approver for a step occurrence.
-- A role step with three holders creates three PENDING tasks; the step is
-- satisfied the moment any one of them is APPROVED (the others are then
-- CANCELLED, not left dangling). due_at (set from the owning step's
-- escalation_hours) is what the WF-5 escalation sweep polls.
CREATE TABLE approval_tasks (
    id               uuid PRIMARY KEY,
    tenant_id        uuid NOT NULL,
    instance_id      uuid NOT NULL REFERENCES workflow_instances (id),
    step_id          uuid NOT NULL REFERENCES approval_steps (id),
    sequence_order   int NOT NULL,
    assigned_user_id uuid NOT NULL,
    status           varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ESCALATED')),
    decision_comment varchar(2000),
    decided_at       timestamptz,
    decided_by       uuid,
    due_at           timestamptz,
    created_at       timestamptz NOT NULL,
    version          bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_approval_tasks_tenant_id ON approval_tasks (tenant_id);
CREATE INDEX idx_approval_tasks_instance_id ON approval_tasks (instance_id);
CREATE INDEX idx_approval_tasks_assigned ON approval_tasks (assigned_user_id, status);
CREATE INDEX idx_approval_tasks_due ON approval_tasks (status, due_at);

ALTER TABLE approval_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_tasks FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_tasks
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- WF-7: append-only decision log (timestamps + comments) for a document's
-- approval history. Immutability enforced the same way as audit_log
-- (V10): reject-mutation triggers, plus no update/delete method ever
-- declared on the Spring Data repository.
CREATE TABLE approval_history (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL,
    instance_id    uuid NOT NULL REFERENCES workflow_instances (id),
    task_id        uuid,
    action         varchar(32) NOT NULL,
    actor_user_id  uuid,
    comment        varchar(2000),
    occurred_at    timestamptz NOT NULL
);

CREATE INDEX idx_approval_history_tenant_id ON approval_history (tenant_id);
CREATE INDEX idx_approval_history_instance_id ON approval_history (instance_id, occurred_at);

ALTER TABLE approval_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_history FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON approval_history
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE FUNCTION approval_history_reject_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'approval_history is append-only: % is not permitted (WF-7)', TG_OP;
END;
$$;

CREATE TRIGGER approval_history_no_update
    BEFORE UPDATE ON approval_history
    FOR EACH ROW EXECUTE FUNCTION approval_history_reject_mutation();

CREATE TRIGGER approval_history_no_delete
    BEFORE DELETE ON approval_history
    FOR EACH ROW EXECUTE FUNCTION approval_history_reject_mutation();
