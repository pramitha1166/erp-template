# Eudext ERP — Project Instructions

Multi-tenant, modular ERP for Sri Lankan SMEs. Java 21 / Spring Boot 3.x /
Spring Modulith · PostgreSQL 16 (RLS) · Next.js 15 / TypeScript.

## Source of truth

- `docs/SRS.md` — the full Software Requirements Specification. Every
  requirement has an ID (e.g. `ARCH-2`, `IAM-3`, `ORG-2`, `LK-4`). Reference
  these IDs in code comments only when a rule is genuinely non-obvious
  (e.g. why a field is `NOT NULL`), and in commit/PR descriptions always.
- `docs/TASK-BREAKDOWN.md` — the backlog: Phase → Epic → Task, each task
  tagged with the SRS IDs it satisfies. Work one Epic per branch/PR. Before
  starting anything not obviously covered there, check whether it already
  exists as a task before inventing a new approach.

## Non-negotiable invariants

These come directly from the SRS and are expensive to fix after the fact —
never work around them "for now":

- **Money**: `BigDecimal` scale 4 internally, currency-precision on display.
  Never `double`/`float` for a monetary field (ARCH-5) — the build's static
  analysis rule must catch this, don't disable it to get code compiling.
- **Migrations**: Flyway only. No Hibernate `ddl-auto` in any environment,
  including local dev (ARCH-7).
- **Optimistic locking**: every aggregate root has `@Version` (ARCH-6).
- **Module boundaries**: Spring Modulith modules (`finance`, `inventory`,
  `procurement`, `sales`, `payroll`, `statutory`, `banking`, `masterdata`)
  talk only via published events or explicit public APIs — never reach
  across a module boundary into another module's repository or internal
  package. ArchUnit enforces this; a failing ArchUnit test means the design
  is wrong, not that the rule needs an exception (ARCH-1).
- **Tenancy**: every table carries `tenant_id`, enforced by PostgreSQL RLS,
  not just an application-layer `WHERE` clause. A new table needs an RLS
  policy in the same migration that creates it (ARCH-2).
- **Branch dimension**: every transactional `Document`, every `GLEntry` row,
  and every `StockLedgerEntry` row has `branch_id NOT NULL` from the
  migration that first creates the table. This cannot be retrofitted without
  corrupting historic reporting — get it right the first time (ORG-2).
- **Ledger immutability**: `GLEntry` and `StockLedgerEntry` are insert-only.
  No application code path updates or deletes a row in either table.
  Corrections are new, linked entries (§7 of the SRS, FIN-4).
- **Document lifecycle**: `DRAFT → SUBMITTED → CANCELLED`, with `AMENDED`
  as a new linked document. A submitted document is immutable except for
  fields explicitly designated amendable (ARCH-4).
- **Statutory calculations** (`MOD-LK`, package under `statutory`): pure
  functions with no persistence dependency (LK-14). Every calculation needs
  unit tests covering zero, below-threshold, each slab boundary, and
  above-top-slab cases, and the package must sit at **100% branch coverage**
  in CI — this is a hard gate, not a target (SRS §4.6 test requirement).
- **White-labelling**: no hardcoded product name, logo path, colour, or URL
  in backend or frontend source outside the seed data file (BRD-2). If you
  need a brand-specific value, it comes from `Brand` config, not a literal.

## Working conventions

- One branch, one PR, per Epic (or a small cluster of directly related
  Tasks) from `docs/TASK-BREAKDOWN.md`. Reference the Task IDs in the PR
  description (e.g. "Implements 0.2.3, 0.2.4").
- Tests land in the same PR as the code, not deferred to a later pass —
  the exception is the cross-cutting audits that Phase 6 explicitly owns.
- Before opening or updating a PR: run the backend test suite, ArchUnit,
  and the coverage check locally. A red CI run on a PR you opened is yours
  to fix before asking for review.
- Don't build ahead of the phase gate: e.g. Finance-module code (Phase 1)
  should depend on Inventory (Phase 2) only through the stock-hook
  interface point implied by FIN-8, never by assuming Phase 2 tables exist.
- Master data is soft-delete only once referenced (`disabled` flag, never a
  hard delete) — MDM-10.

## Commands

To be filled in once Epic 0.0 (repository & CI/CD bootstrap) lands — that
PR should update this section with the actual build/test/lint commands for
`backend/` and `frontend/`.
