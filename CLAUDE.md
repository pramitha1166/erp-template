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

## Project status (last updated 2026-09-02)

GitHub issues are the live source of truth for what's done — this is a
snapshot to orient a new session quickly, not authoritative. Re-check issue
state before assuming anything below is still accurate.

**Backend Epics — done (merged):** 0.0 Bootstrap (#1), 0.1 Document Model
(#2), 0.2 IAM (#3), 0.3 Audit Trail (#4), 0.4 Workflow Engine / `PLAT-WF`
(#5, PR #39), 0.5 Numbering / `PLAT-NUM` (#6, PR #42), 0.6 Master Data /
`PLAT-MDM` (#7, PR #45), 0.11 Tenant Onboarding & Administration /
`PLAT-ADMIN` (#31, plus sub-task 0.11.10 platform/brand-admin auth +
bootstrap rotation, #35).

**0.4 caveat (still open as of this update):** the session that merged
Epic 0.4 could not run `mvn verify`'s Testcontainers IT (`WorkflowEngineIT`)
— sandbox egress blocked the Docker Hub pull — so it compiled clean and
unit-tested clean but the integration test has not been confirmed green by
anyone yet, including the 0.5/0.6/F0.4/F0.5/F0.6 work that has since landed
on top of it. Run `mvn verify` before trusting `WorkflowApi` further.

**Backend Epics — open:** 0.7 Documents & Attachments (#8), 0.8 Branding &
White-Label (#9), 0.9 Branch & Org Dimensions (#10), 0.10 Reporting
Framework (#11).

**Frontend Epics — done (merged):** F0.0 App Shell & Bootstrap (#21),
F0.1 Document Lifecycle UI Primitives (#22), F0.2 Auth & Access UI (#23),
F0.3 Audit Trail UI (#24 — code had already merged via a prior commit;
closed 2026-09-01 with no further work needed), F0.4 Workflow & Approvals
UI (#25, PR #40), F0.5 Numbering UI (#41, PR #43), F0.6 Master Data UI
(#44, PR #46), F0.11 Admin Portal UI (#32), F0.11.7 separate admin login
UI (#36, PR #47, a sub-task split out during F0.11 implementation).

**Frontend Epics — open:** none currently. Every Phase 0 backend epic
merged so far (0.0–0.6, 0.11) has its frontend companion merged too.

**Addendum:** `PLAT-ADMIN` (SRS §3.10, `ADM-1`..`ADM-9`) was added after a
gap review found no requirement for who provisions a `Brand` or how a
`Tenant` gets onboarded — `BRD-14` only ever specified the brand-level
partner console. Covers the Platform Admin Console, the Brand Admin
Console, tenant onboarding with default-data seeding, suspension/
reactivation, audited impersonation, and PDPA export/erasure handling.
See Epic 0.11 / F0.11 in `docs/TASK-BREAKDOWN.md`.

**Not yet broken out:** frontend companions for Epics 0.7–0.10 (i.e.
F0.7–F0.10) don't exist yet in `docs/TASK-BREAKDOWN.md` or as issues — cut
them once each backend epic lands, following the F0.0–F0.6/F0.11 pattern.
(F0.5 Numbering UI was cut when Epic 0.5's backend landed; F0.6 Master
Data UI (#44) was cut alongside Epic 0.6's backend implementation, ahead
of that PR merging, since the task breakdown was already fully known —
see below.)

When an Epic's PR merges, update its line above and close the matching
GitHub issue if it isn't already closed — don't let this section go stale.

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
  and the coverage check locally. **There is no CI running them for you** —
  the only automated pipeline is the AWS one that builds and deploys, and
  it does not run tests. An unrun suite is an unverified change.
- Don't build ahead of the phase gate: e.g. Finance-module code (Phase 1)
  should depend on Inventory (Phase 2) only through the stock-hook
  interface point implied by FIN-8, never by assuming Phase 2 tables exist.
- Master data is soft-delete only once referenced (`disabled` flag, never a
  hard delete) — MDM-10.

## Commands

### Backend (`backend/`, Maven)

- `mvn compile` — compile only.
- `mvn test` — unit tests, ArchUnit module-boundary rules (ARCH-1), the
  ARCH-5 double/float ban, and Spring Modulith verification. No Docker
  required.
- `mvn verify` — everything in `test` plus Testcontainers integration tests
  (`*IT.java`, NFR-M2) and the statutory-package JaCoCo coverage gate
  (100% branch coverage, SRS §4.6 — a hard CI gate). Requires Docker.
- `mvn -Popenapi verify -DskipTests` — boots the app against a real
  Postgres and writes `target/openapi.yaml` (NFR-M4). Needs
  `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` pointing at a reachable Postgres
  (see `docker-compose.yml` or the `backend` CI job).
- `mvn org.owasp:dependency-check-maven:13.0.0:check -DfailBuildOnCVSS=7
  -DnvdApiKey=$NVD_API_KEY` — dependency CVE scan (NFR-S3). Add an
  `NVD_API_KEY` from https://nvd.nist.gov/developers/request-an-api-key to
  run this at a usable speed.

### Frontend (`frontend/`, npm)

- `npm run dev` — local dev server.
- `npm run lint` — ESLint.
- `npm run typecheck` — `tsc --noEmit`.
- `npm run test` — unit tests (Vitest + Testing Library).
- `npm run build` — production build.
- `npm run audit:ci` — dependency CVE scan (NFR-S3), gated by
  `audit-ci.jsonc`'s documented allowlist.
- `npm run generate:api` — regenerates `src/lib/api/schema.d.ts` from the
  backend's published OpenAPI spec (consumes 0.0.8). Run
  `mvn -Popenapi verify -DskipTests` in `backend/` first, or point
  `OPENAPI_SPEC` at a running backend's `/v3/api-docs.yaml`.

### Local stack

- `docker compose up` — Postgres 16, Redis, MinIO, backend, frontend
  (NFR-D1).

### Infrastructure & deployment (AWS)

`infra/` — Terraform for the `staging` AWS environment (ECS Fargate, RDS,
ElastiCache, ALB, S3, ECR). Two separate pipelines, deliberately not one:
There are **no GitHub Actions workflows** — the repo deliberately has none.
Infrastructure changes are planned and applied by hand from a workstation
(`cd infra/environments/staging && terraform plan`), while an **AWS
CodePipeline** (`infra/modules/codepipeline`, Terraform-managed) builds and
deploys the *application* straight from GitHub via a CodeStar connection.
Each CodeBuild project reports its outcome back to the repo's Deployments
API, so a successful deploy shows up on the commit and under the repo's
Environments tab. See `infra/README.md` for the one-time bootstrap, the
manual steps (authorizing the GitHub connection, storing the deployment
token), and day-2 operations (custom domain, rollback, scaling toward
production).
