# Eudext ERP — Task Breakdown

Source: `Eudext ERP — Software Requirements Specification`, v1.0 (Draft).

This document decomposes the SRS into an execution backlog: **Phase → Epic → Task**,
each task tagged with the SRS requirement IDs it satisfies so traceability survives
into sprint planning and test coverage reports. Sizes are rough (S = ≤2 days,
M = ~1 week, L = 2–3 weeks, XL = needs its own breakdown before sprinting) and
assume the 3–4 engineer team the SRS phasing is built around.

Repo is currently empty, so **Phase 0 starts with repository bootstrap** — this
is implied by the tech stack (§6) and maintainability NFRs (§5.4) but has no
requirement ID of its own, so it's called out explicitly as Epic 0.0 below.

---

## Phase 0 — Platform Foundation (~4 months)

**Gate:** tenant isolation test passes · a dummy document completes full lifecycle
· a second brand added by config only · branch-filtered visibility verified.

### Epic 0.0 — Repository & CI/CD Bootstrap (prerequisite, no SRS ID)

| Task | Description | Size |
|---|---|---|
| 0.0.1 | Monorepo layout: `backend/` (Spring Boot, Spring Modulith module skeletons per §2.1) and `frontend/` (Next.js 15 App Router) | M |
| 0.0.2 | Base Docker Compose: Postgres 16, Redis, backend, frontend, MinIO (S3-compatible) — NFR-D1 | M |
| 0.0.3 | Flyway baseline migration set up; confirm Hibernate `ddl-auto=none` everywhere — ARCH-7 | S |
| 0.0.4 | GitHub Actions CI: build, unit tests, ArchUnit, lint, dependency/CVE scan — NFR-S3, NFR-M3 | M |
| 0.0.5 | ArchUnit rule set encoding ARCH-1 module boundaries (Spring Modulith event-only cross-module access) | M |
| 0.0.6 | Static analysis rule: fail build on `double`/`float` for monetary fields — ARCH-5 | S |
| 0.0.7 | Structured JSON logging + correlation id propagation scaffold — NFR-M5 | M |
| 0.0.8 | OpenAPI generation wired into build, published as CI artifact — NFR-M4 | S |
| 0.0.9 | Testcontainers harness (Postgres, Redis) for integration tests — NFR-M2 | S |

### Epic 0.1 — Core Document Model (ARCH-2 .. ARCH-6)

| Task | SRS Ref | Size |
|---|---|---|
| 0.1.1 | `Document` supertype (id, tenantId, companyId, docNumber, docStatus, postingDate, audit fields, `@Version`) | ARCH-3, ARCH-6 | M |
| 0.1.2 | Document lifecycle state machine: DRAFT → SUBMITTED → CANCELLED, AMENDED-as-new-linked-doc; immutability enforcement on submitted docs | ARCH-4 | L |
| 0.1.3 | Row-Level Security: `tenant_id` on every table, RLS policies, session variable set per request from JWT | ARCH-2 | L |
| 0.1.4 | Automated adversarial tenant-isolation test suite (a request in tenant A must never see tenant B data) | NFR-S6 | M |
| 0.1.5 | `BigDecimal` scale-4 monetary value conventions + shared money type/utility | ARCH-5 | S |

### Epic 0.2 — Identity & Access (`PLAT-IAM`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.2.1 | Auth: email/password, Argon2id hashing, JWT access (15 min) + rotating refresh (7 days) | IAM-1 | M |
| 0.2.2 | TOTP 2FA, mandatory for users with approval permissions | IAM-2 | M |
| 0.2.3 | RBAC model: `module:entity:action` permission triples, role CRUD | IAM-3 | L |
| 0.2.4 | Per-company role assignment (user can hold different roles per company) | IAM-4 | M |
| 0.2.5 | Field-level permission engine (e.g. salary field gating) | IAM-5 | L |
| 0.2.6 | Record-level permission engine (warehouse/cost-centre/branch scoping) — coordinate with Epic 0.6 | IAM-6 | L |
| 0.2.7 | Segregation-of-Duties rule engine: configurable conflicting-permission pairs block assignment | IAM-7 | M |
| 0.2.8 | Session management: active session list, force logout, configurable idle timeout | IAM-8 | M |
| 0.2.9 | Password policy engine: length/complexity/history/expiry, configurable per tenant | IAM-9 | S |
| 0.2.10 | Auth audit events feed into Epic 0.3 audit log (login success/failure, grant/revoke, role change) | IAM-10 | S |

### Epic 0.3 — Audit Trail (`PLAT-AUDIT`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.3.1 | Append-only audit table + write path (entity type/id, action, JSONB old→new diff, user, timestamp, IP, request id) | AUD-1, AUD-2 | M |
| 0.3.2 | Enforce immutability (no UPDATE/DELETE grants/paths on audit table) | AUD-3 | S |
| 0.3.3 | Generic interceptor/aspect so every transactional & master-data mutation auto-audits (avoids per-module boilerplate) | AUD-1 | L |
| 0.3.4 | Document version history UI with field-level diff | AUD-4 | M |
| 0.3.5 | Retention/archival job: cold-storage move after 2 years, 7-year minimum retention | AUD-5 | M |

### Epic 0.4 — Workflow Engine (`PLAT-WF`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.4.1 | Approval chain configuration model per document type per company | WF-1 | L |
| 0.4.2 | Condition expressions on document fields (e.g. amount thresholds) | WF-2 | M |
| 0.4.3 | Approver resolution: by role, named user, or reporting hierarchy | WF-3 | M |
| 0.4.4 | Parallel and sequential step execution engine | WF-4 | L |
| 0.4.5 | Delegation (date-ranged) and timeout escalation | WF-5 | M |
| 0.4.6 | Rejection flow: return to draft with mandatory comment | WF-6 | S |
| 0.4.7 | Approval history panel (timestamps, comments) on documents | WF-7 | S |
| 0.4.8 | Email + in-app notifications on pending approval | WF-8 | M |

### Epic 0.5 — Numbering (`PLAT-NUM`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.5.1 | Naming-series config per doc type per company (prefix, date parts, counter width) | NUM-1 | M |
| 0.5.2 | Gapless sequence guarantee for statutory documents | NUM-2 | M |
| 0.5.3 | Fiscal-year reset behavior, configurable | NUM-3 | S |
| 0.5.4 | Concurrency-safe allocation (DB sequence/advisory lock under load test) | NUM-4 | M |

### Epic 0.6 — Master Data (`PLAT-MDM`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.6.1 | Company master (legal name, reg no., VAT no., address, base currency, fiscal year, logo) | MDM-1 | S |
| 0.6.2 | Multi-company within tenant (inter-company txns deferred to v2.0 per MDM-2) | MDM-2 | M |
| 0.6.3 | Chart of Accounts: hierarchical tree, account types, group vs ledger accounts | MDM-3 | L |
| 0.6.4 | Cost Centre master, hierarchical | MDM-4 | M |
| 0.6.5 | Customer / Supplier master (contacts, tax reg, credit terms, default accounts, bank details) | MDM-5 | L |
| 0.6.6 | Item master (code, UOM, item group, valuation method, reorder level, batch/serial flags, tax category, HS code) | MDM-6 | L |
| 0.6.7 | UOM + conversion factors, purchase vs stock UOM | MDM-7 | S |
| 0.6.8 | Currency master, date-effective exchange rates, optional CBSL rate import job | MDM-8 | M |
| 0.6.9 | Fiscal Year / Accounting Period master with open/closed status | MDM-9 | S |
| 0.6.10 | Soft-delete-only enforcement (disabled flag) once a master record is referenced | MDM-10 | M |

### Epic 0.7 — Documents & Attachments (`PLAT-DOC`)

| Task | SRS Ref | Size |
|---|---|---|
| 0.7.1 | Generic attachment model + S3-compatible object storage integration | DOC-1 | M |
| 0.7.2 | Print-format template editor, configurable per document type | DOC-2 | L |
| 0.7.3 | Server-side deterministic PDF generation (JasperReports/Flying Saucer) | DOC-3 | M |
| 0.7.4 | Virus scanning on upload | DOC-4 | S |
| 0.7.5 | Attachment access inherits parent document permission checks | DOC-5 | S |

### Epic 0.8 — Branding & White-Label (`PLAT-BRAND`)

Build for Model B (reseller white-label) from day one — see SRS note that
retrofitting from Model A is a rewrite.

| Task | SRS Ref | Size |
|---|---|---|
| 0.8.1 | `Brand` entity above `Tenant`; full identity fields (name, logos, favicon, colour tokens, fonts, support contacts, legal name, terms/privacy URLs) | BRD-1 | M |
| 0.8.2 | Static-analysis/build rule: fail on hardcoded product name/logo/colour/URL outside seed data | BRD-2 | M |
| 0.8.3 | Host-based brand resolution: Next.js middleware (Host header) + backend JWT-claim resolution; neutral error page for unknown host | BRD-3 | L |
| 0.8.4 | Custom domain + ACME TLS automation; subdomain fallback on brand creation | BRD-4 | L |
| 0.8.5 | Runtime CSS-custom-property theming (no build-time compile, no redeploy to add a brand) | BRD-5 | L |
| 0.8.6 | Full theme token set (primary/secondary/accent/destructive/muted/etc., radius, font) with light+dark variants | BRD-6 | M |
| 0.8.7 | Brand asset storage in object storage + CDN with version-hash cache busting | BRD-7 | M |
| 0.8.8 | Brand-aware transactional email templates; per-brand sending domain with DKIM/SPF status in admin UI | BRD-8 | L |
| 0.8.9 | Brand/tenant-overridable PDF print formats (invoice, salary slip, statement, PO) | BRD-9 | M |
| 0.8.10 | Brand-aware login, password reset, error, and maintenance pages | BRD-10 | M |
| 0.8.11 | Brand-derived page title, meta tags, OG image, PWA manifest, app icons | BRD-11 | S |
| 0.8.12 | Per-brand feature entitlements (hide modules from nav, permissions, and API surface) | BRD-12 | L |
| 0.8.13 | Per-brand help content / doc URLs / support-widget routing | BRD-13 | S |
| 0.8.14 | Brand-level admin console (partner self-service for own tenants/users/branding) | BRD-14 | L |
| 0.8.15 | Per-brand usage reporting for partner billing (tenants, active users, storage, tx volume) | BRD-15 | M |
| 0.8.16 | `MOD-LK` wired as a feature entitlement, cleanly disable-able per brand | BRD-16 | S |
| 0.8.17 | End-to-end "new white-label brand in under 1 hour, zero code/redeploy" runbook + verification test (Acceptance Criterion 8) | BRD-1..16 | M |

### Epic 0.9 — Branch & Organisational Dimensions (`PLAT-ORG`)

**Do not defer `branch_id NOT NULL` on ledger tables — SRS explicitly flags this
as unrecoverable without a corrupting backfill.**

| Task | SRS Ref | Size |
|---|---|---|
| 0.9.1 | Hierarchy model: Tenant → Brand → Company → Branch → Warehouse; Branch master (code, name, parent, company, address, cost-centre default, warehouse defaults, tax reg, active flag) | ORG-1 | M |
| 0.9.2 | `branch_id NOT NULL` on every transactional document, `GLEntry`, and `StockLedgerEntry` from the very first migration; persistence-layer rejection if absent | ORG-2 | L |
| 0.9.3 | Branch hierarchy rollup for reporting (region aggregates children) | ORG-3 | M |
| 0.9.4 | User↔branch assignment; record visibility filtered by branch assignment | ORG-4 | M |
| 0.9.5 | Branch-scoped roles (different role per branch for same user/company) | ORG-5 | M |
| 0.9.6 | Numbering series configurable per branch (integrates with Epic 0.5) | ORG-6 | S |
| 0.9.7 | Approval workflow resolution by branch (integrates with Epic 0.4) | ORG-7 | M |
| 0.9.8 | Inter-branch stock transfer with in-transit warehouse + discrepancy record on receipt variance | ORG-8 | L |
| 0.9.9 | Inter-branch clearing accounts, net-to-zero check + report | ORG-9 | M |
| 0.9.10 | Branch-wise reports: Trial Balance, P&L, Stock Balance, Sales Register, AR/AP Ageing (framework; full reports land with their owning modules) | ORG-10 | M |
| 0.9.11 | Consolidated company view with drill-down for company-level permission holders | ORG-11 | M |
| 0.9.12 | Branch-level budgets + variance reporting | ORG-12 | M |
| 0.9.13 | Branch open/close lifecycle (closed blocks new postings, retains history) | ORG-13 | S |
| 0.9.14 | Cost Centre kept orthogonal to Branch; both present on every document | ORG-14 | S |
| 0.9.15 | Extensible dimension framework (Project/Territory/Product Line as config, no schema change) | ORG-15 | L |

### Epic 0.10 — Reporting Framework Foundation (partial `PLAT-RPT`)

Only the framework — most concrete reports ship with their owning module.

| Task | SRS Ref | Size |
|---|---|---|
| 0.10.1 | Report-definition-as-metadata engine (no redeploy to add a report) | RPT-1 | L |
| 0.10.2 | Standard filter framework (date range, company, cost centre) + saved filter sets per user | RPT-2 | M |
| 0.10.3 | Export pipeline: XLSX, CSV, PDF | RPT-3 | M |
| 0.10.4 | Scheduled report delivery by email | RPT-4 | M |
| 0.10.5 | Dashboard widget-grid framework (number cards, charts, lists) | RPT-5 | L |
| 0.10.6 | Async execution for reports >5s with completion notification | NFR-P4 | M |
| 0.10.7 | Read-replica routing for report queries where configured | RPT-6 | M |

### Epic 0.11 — Tenant Onboarding & Administration (`PLAT-ADMIN`)

Added as an addendum after a gap review found no requirement covering who
provisions a Brand, or how a Tenant actually gets onboarded — BRD-14 only
specified the brand-level partner console. See `docs/SRS.md` §3.10.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| 0.11.1 | Platform Admin Console backend: Brand CRUD (create/suspend/reactivate), cross-brand tenant/usage read APIs, platform-wide default entitlements | ADM-1 | L |
| 0.11.2 | Tenant onboarding orchestration: create Tenant + first Company + initial admin user + entitlement assignment as one transactional flow | ADM-2 | L |
| 0.11.3 | Default data seeding service invoked by onboarding: localised Chart of Accounts template, default numbering series, default fiscal year/period | ADM-3 | M |
| 0.11.4 | Setup-checklist progress tracking API per tenant | ADM-4 | M |
| 0.11.5 | Brand Admin Console backend: tenant CRUD scoped to the caller's own brand, tenant-admin invite flow, per-tenant usage read API | ADM-5, BRD-14, BRD-15 | L |
| 0.11.6 | Tenant suspension/reactivation: block auth + posting endpoints for a suspended tenant while preserving admin read/report access | ADM-6 | M |
| 0.11.7 | Impersonation: time-boxed scoped token issuance for admin-as-tenant-admin sessions, mandatory audit tagging + tenant notification | ADM-7 | L |
| 0.11.8 | Data export/erasure request workflow API, reusing the full data export capability | ADM-8, NFR-S7, NFR-D5 | M |
| 0.11.9 | Platform usage/health rollup API: tenant count, active users, storage, tx volume, system health, grouped by brand | ADM-9 | M |
| 0.11.10 | Platform/brand-admin authentication: login endpoint scoped to the admin realm (no caller-supplied `tenantId`), admin-scope session claims, and a one-shot first-platform-admin bootstrap path that is inert once an admin exists | ADM-1, ADM-5, IAM-1 | M |

Model platform-admin and brand-admin as distinct permission scopes from the
start (SRS design note on ADM-1/ADM-5) — not as ordinary IAM-3 roles with a
higher permission count. That scope split is why 0.11.10 exists: the tenant
login contract takes a caller-supplied `tenantId` (see `AuthController`),
which a platform admin — who belongs to no tenant — has nothing to put in.
The admin realm needs its own authentication entry point rather than a
sentinel tenant value threaded through the tenant one. 0.11.10 blocks
F0.11.7 and is the practical prerequisite for reaching *any* other 0.11
task in a running environment: until it lands there is no way to obtain the
first admin credential at all. Frontend companion: Epic F0.11, in the
Frontend Track section below.

**Phase 0 gate tasks:** tenant-isolation adversarial test (0.1.4), dummy
document full-lifecycle test (0.1.2), second brand via config-only (0.8.17),
branch-filtered visibility test (0.9.4). Epic 0.11/F0.11 (ADM-2, ADM-3)
directly serve Acceptance Criterion 1 (zero-to-first-invoice in under 4
hours) and should be verified against it explicitly.

---

## Frontend Track — Phase 0 (Epics 0.0–0.4)

The original breakdown wove UI work into whichever backend epic owned the
feature. This section pulls the frontend slice for the Phase 0 epics
completed so far (0.0 Bootstrap, 0.1 Document Model, 0.2 IAM, 0.3 Audit,
0.4 Workflow Engine, 0.5 Numbering) into its own explicit track so frontend
work can be staffed and issued independently of backend progress. Frontend
tasks for Epics 0.6–0.10 follow the same pattern once those backend epics
land. Epic F0.11 (Admin Portal UI) is included here too since its backend
companion, Epic 0.11, was added as a gap-review addendum rather than
waiting in sequence behind 0.6–0.10.

### Epic F0.0 — Frontend App Shell & Bootstrap

Companion to Epic 0.0. No screens yet — this is the scaffold everything
else in the frontend track builds on.

| Task | Description | Size |
|---|---|---|
| F0.0.1 | Next.js 15 App Router project structure: route groups, layout hierarchy, error/loading boundaries | M |
| F0.0.2 | TanStack Query client + API client generated from the backend's published OpenAPI spec (consumes 0.0.8) | M |
| F0.0.3 | Zustand store scaffold for client state (auth/session, active tenant/company context) | S |
| F0.0.4 | shadcn/ui + Tailwind base install, design tokens wired to CSS custom properties (lays groundwork for Epic 0.8 theming) | M |
| F0.0.5 | Base app shell: nav frame, header, responsive layout from 360px (NFR-U1) | M |
| F0.0.6 | Frontend CI job: typecheck (strict TS), lint, unit tests, build — extends 0.0.4 | S |

### Epic F0.1 — Document Lifecycle UI Primitives

Companion to Epic 0.1. Reusable components every later document screen
(invoices, orders, entries) will be built from — build once here, not
per-module.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.1.1 | Document status badge + lifecycle action bar (Draft/Submit/Cancel/Amend) as a shared component | ARCH-4 | M |
| F0.1.2 | Optimistic-lock conflict handling: version-mismatch dialog on stale submit | ARCH-6 | S |
| F0.1.3 | Generic document list/table view (server-paginated, status filter, sort) for reuse across modules | NFR-P2 | M |
| F0.1.4 | Inline field-level validation pattern (react-hook-form + Zod) — never a generic failure toast | NFR-U4 | M |
| F0.1.5 | Tenant/company context indicator in the app shell header | ARCH-2 | S |

### Epic F0.2 — Auth & Access UI

Companion to Epic 0.2. First screens a real user touches.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.2.1 | Login page + rotating-refresh session handling in the API client | IAM-1 | M |
| F0.2.2 | TOTP 2FA enrollment and verify flow | IAM-2 | M |
| F0.2.3 | Forgot/reset password flow with live password-policy validation | IAM-9 | M |
| F0.2.4 | Session management page: active sessions list, force logout | IAM-8 | M |
| F0.2.5 | Role & permission admin: role CRUD, `module:entity:action` assignment matrix | IAM-3 | L |
| F0.2.6 | Per-company role switcher (user holds different roles per company) | IAM-4 | S |
| F0.2.7 | SoD conflict warning surfaced inline during role assignment | IAM-7 | M |
| F0.2.8 | Field-level permission pattern: masked/hidden field rendering (e.g. salary visible only to HR roles) | IAM-5 | M |

### Epic F0.3 — Audit Trail UI

Companion to Epic 0.3.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.3.1 | Document version history panel with field-level diff, embedded on document detail views | AUD-4 | L |
| F0.3.2 | Audit log browser/search screen for admins (entity, user, date range, action filters) | AUD-2 | M |
| F0.3.3 | Retention/archival status indicator on the audit browser (admin-only) | AUD-5 | S |

### Epic F0.4 — Workflow & Approvals UI

Companion to Epic 0.4.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.4.1 | Approval chain configuration screen per document type per company | WF-1 | L |
| F0.4.2 | Condition builder UI for field-based approval rules (e.g. amount thresholds) | WF-2 | M |
| F0.4.3 | Pending-approval inbox (dashboard widget + full list view) | WF-8 | M |
| F0.4.4 | Approve/reject action screen with mandatory comment on rejection | WF-6 | M |
| F0.4.5 | Delegation setup (date range) and escalation status indicator | WF-5 | M |
| F0.4.6 | Approval history timeline component embedded on document detail views (shares groundwork with F0.3.1) | WF-7 | M |

### Epic F0.5 — Numbering UI

Frontend companion to Epic 0.5 (`PLAT-NUM`), cut once the backend epic
landed (per the "not yet broken out" note this section used to carry).
Depends on Epic F0.0 (app shell) and Epic F0.2 (auth/permission patterns).

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.5.1 | Naming-series configuration screen: prefix/date-part template editor, counter width, per doc type per company | NUM-1 | M |
| F0.5.2 | Live preview of the next formatted document number as the template is edited | NUM-1 | S |
| F0.5.3 | Fiscal-year reset policy control (never / annual + fiscal-year-start-month) on the series form | NUM-3 | S |
| F0.5.4 | Series list view with activate/deactivate lifecycle actions | NUM-1 | S |

### Epic F0.6 — Master Data UI

Frontend companion to Epic 0.6 (`PLAT-MDM`), cut once the backend epic
landed (per the "not yet broken out" note this section used to carry).
Depends on Epic F0.0 (app shell) and Epic F0.2 (auth/permission patterns).

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.6.1 | Company management screen: list/switch companies within the tenant, create-another-company form, edit legal name/address/logo | MDM-1, MDM-2 | M |
| F0.6.2 | Chart of Accounts tree view/editor: create/rename nodes, group-vs-ledger toggle, activate/deactivate | MDM-3 | L |
| F0.6.3 | Cost Centre hierarchical tree management screen | MDM-4 | M |
| F0.6.4 | Customer/Supplier master list + detail form: contacts, credit terms, default account, bank details | MDM-5 | L |
| F0.6.5 | Item master list + detail form: item group, stock/purchase UOM, valuation method, batch/serial flags, tax category, HS code | MDM-6 | L |
| F0.6.6 | UOM management screen + conversion-factor editor | MDM-7 | S |
| F0.6.7 | Currency management screen + exchange-rate entry/history view | MDM-8 | M |
| F0.6.8 | Fiscal Year / Accounting Period administration: close/reopen actions with the open-period guard surfaced inline | MDM-9 | S |
| F0.6.9 | Consistent disable/enable (soft-delete) affordance across every master list screen — no delete action anywhere | MDM-10 | S |

### Epic F0.11 — Admin Portal UI

Frontend companion to Epic 0.11 (`PLAT-ADMIN`). Depends on Epic F0.0 (app
shell) and Epic F0.2 (auth/permission patterns).

F0.11.7 (admin login) is the entry point for every other task in this epic
and should land first; it consumes the admin-realm auth endpoint from
0.11.10. Keep it a genuinely separate screen and session surface from the
F0.2.1 tenant login — reusing the tenant login page with a hidden or
sentinel tenant field is the failure mode this task exists to avoid.

| Task | Description | SRS Ref | Size |
|---|---|---|---|
| F0.11.1 | Platform Admin Console UI: brand list/create/suspend, cross-brand usage dashboard | ADM-1, ADM-9 | L |
| F0.11.2 | Tenant onboarding wizard: company details → branches → fiscal year → initial admin user → plan/entitlement → review & create | ADM-2 | L |
| F0.11.3 | Post-onboarding guided setup checklist widget with completion tracking | ADM-4 | M |
| F0.11.4 | Brand Admin Console UI: tenant list/create/suspend within the brand, tenant-admin invite screen, per-tenant usage view | ADM-5, BRD-14, BRD-15 | L |
| F0.11.5 | Impersonation entry point ("log in as tenant admin") with a persistent on-screen banner for the duration of an impersonated session | ADM-7 | M |
| F0.11.6 | Tenant data export/erasure request screen for admins, with status tracking | ADM-8 | M |
| F0.11.7 | Separate admin login UI at its own route, distinct from the tenant login: no tenant field, admin-realm branding, its own post-login redirect into the admin console, and forced change of a bootstrap/temporary credential on first use | ADM-1, ADM-5, IAM-1, IAM-9 | M |

---

## Phase 1 — Finance Core (~3 months)

**Gate:** balanced books on a 3-month simulated dataset, verified by an
accountant; branch-wise P&L reconciles to company total.

### Epic 1.1 — General Ledger

| Task | SRS Ref | Size |
|---|---|---|
| 1.1.1 | `GLEntry` immutable table (account, debit, credit, posting date, company, cost centre, party, voucher type/number, branch) — persistence-layer balance enforcement | FIN-1, FIN-2 | L |
| 1.1.2 | Journal Entry: manual multi-line posting, attachment, narration | FIN-3 | M |
| 1.1.3 | Reversal flow: linked contra entry, original never mutated | FIN-4 | M |
| 1.1.4 | Period lock: block posting to closed periods; unlock requires elevated permission + audit | FIN-5 | M |
| 1.1.5 | Year-end close: P&L → retained earnings, opening balance carry-forward | FIN-6 | L |
| 1.1.6 | Multi-currency posting: txn currency, base currency, rate, realised/unrealised gain-loss | FIN-7 | L |

### Epic 1.2 — Accounts Receivable

| Task | SRS Ref | Size |
|---|---|---|
| 1.2.1 | Sales Invoice → GL posting, customer balance update, stock reduction hook | FIN-8 | L |
| 1.2.2 | Credit Note referencing original invoice | FIN-9 | M |
| 1.2.3 | Payment Entry with multi-invoice allocation, partial payments, advances | FIN-10 | L |
| 1.2.4 | Customer statement + ageing analysis (30/60/90/120+) | FIN-11 | M |
| 1.2.5 | Credit limit enforcement (block/warn, configurable) | FIN-12 | M |

### Epic 1.3 — Accounts Payable

| Task | SRS Ref | Size |
|---|---|---|
| 1.3.1 | Purchase Invoice with three-way match (PO/GRN) and configurable tolerance | FIN-13 | L |
| 1.3.2 | Debit Note referencing original invoice | FIN-14 | M |
| 1.3.3 | Payment Entry: allocation, advances, withholding-tax deduction | FIN-15 | L |
| 1.3.4 | Supplier ageing / payment-due report | FIN-16 | M |

### Epic 1.4 — Banking (reconciliation)

| Task | SRS Ref | Size |
|---|---|---|
| 1.4.1 | Bank account master linked to GL account | FIN-17 | S |
| 1.4.2 | Bank statement import (CSV/MT940) with configurable column mapping per bank | FIN-18 | L |
| 1.4.3 | Reconciliation screen: auto-match, manual match, unmatched queue | FIN-19 | L |
| 1.4.4 | Petty cash / cash book | FIN-20 | M |

### Epic 1.5 — Finance Reports

| Task | Covers |
|---|---|
| 1.5.1 | Trial Balance, General Ledger report | |
| 1.5.2 | Balance Sheet, Profit & Loss | |
| 1.5.3 | Cash Flow (indirect method) | |
| 1.5.4 | AR/AP Ageing, Customer/Supplier Ledger | |
| 1.5.5 | Cost Centre P&L, Day Book | |
| 1.5.6 | Branch-wise variants of all of the above, reconciling to company total (gate criterion) | |

---

## Phase 2 — Inventory + Procurement (~3.5 months)

**Gate:** perpetual inventory reconciles to GL to the cent across 1,000
transactions; inter-branch clearing nets to zero.

### Epic 2.1 — Inventory Core

| Task | SRS Ref | Size |
|---|---|---|
| 2.1.1 | Multi-warehouse, hierarchical warehouse tree | INV-1 | M |
| 2.1.2 | `StockLedgerEntry` as single source of truth (qty + value posted atomically) | INV-2 | L |
| 2.1.3 | Valuation engines: FIFO, Moving Average, Specific/batch — per item | INV-3 | XL |
| 2.1.4 | Perpetual inventory: every stock movement posts GL in the same transaction | INV-4 | L |
| 2.1.5 | Batch tracking (mfg/expiry dates), FEFO picking option | INV-5 | L |
| 2.1.6 | Serial number tracking with full movement history | INV-6 | L |
| 2.1.7 | Stock Entry types: Receipt, Issue, Transfer, Manufacture, Repack | INV-7 | L |
| 2.1.8 | Stock Reconciliation (physical count) with variance GL posting | INV-8 | M |
| 2.1.9 | Reorder level + automatic reorder request generation | INV-9 | M |
| 2.1.10 | Negative stock blocked by default, config + elevated permission to allow | INV-10 | S |
| 2.1.11 | Landed cost voucher: apportion freight/duty/clearing by amount or qty | INV-11 | L |
| 2.1.12 | Backdated entry → automatic revaluation of subsequent entries; block backdate beyond period lock | INV-12 | XL |
| 2.1.13 | Nightly stock↔GL reconciliation job with alert (risk mitigation) | — | M |

### Epic 2.2 — Inventory Reports

Stock Balance, Stock Ledger, Stock Ageing, Batch Expiry, Item-wise Valuation,
Warehouse-wise Stock, Slow Moving Items, Reorder Report.

### Epic 2.3 — Procurement

| Task | SRS Ref | Size |
|---|---|---|
| 2.3.1 | Purchase Requisition with departmental approval routing | PRC-1 | M |
| 2.3.2 | RFQ to multiple suppliers + quotation comparison view | PRC-2 | M |
| 2.3.3 | Purchase Order (from requisition or quotation) with value-based approval | PRC-3 | L |
| 2.3.4 | Partial receipt / partial invoicing against PO with running balance | PRC-4 | L |
| 2.3.5 | GRN: stock update + accrual posting | PRC-5 | L |
| 2.3.6 | Quality rejection at GRN with return-to-supplier flow | PRC-6 | M |
| 2.3.7 | Purchase Return with stock + GL reversal | PRC-7 | M |
| 2.3.8 | Supplier price list with validity dates and quantity breaks | PRC-8 | M |
| 2.3.9 | Import documentation on PO/GRN: LC reference, BOE number, shipping details | PRC-9 | M |

---

## Phase 3 — Sales + POS API (~3 months)

**Gate:** full order-to-cash cycle with correct GL/stock impact; 500-receipt
batch posts idempotently from a sample POS client.

### Epic 3.1 — Sales

| Task | SRS Ref | Size |
|---|---|---|
| 3.1.1 | Quotation with validity period → conversion to Sales Order | SLS-1 | M |
| 3.1.2 | Sales Order: delivery schedule, partial fulfilment tracking | SLS-2 | L |
| 3.1.3 | Price List: customer group, qty break, date validity | SLS-3 | M |
| 3.1.4 | Discount rules: line, document, promotional (buy X get Y) | SLS-4 | L |
| 3.1.5 | Delivery Note (stock reduction) linked to invoice | SLS-5 | M |
| 3.1.6 | Sales Invoice with/without preceding delivery note | SLS-6 | M |
| 3.1.7 | Sales Return with stock + GL reversal | SLS-7 | M |
| 3.1.8 | Sales commission calculation per salesperson | SLS-8 | M |
| 3.1.9 | Territory + salesperson hierarchy for reporting | SLS-9 | S |

### Epic 3.2 — Sales Reports

Sales Register, Item-wise Sales, Customer-wise Sales, Salesperson Performance,
Gross Margin by Item/Customer, Order Fulfilment Status.

### Epic 3.3 — POS Integration API (`MOD-POSAPI`)

| Task | SRS Ref | Size |
|---|---|---|
| 3.3.1 | OpenAPI-documented public REST API; per-terminal API key scoped to branch+warehouse | PAPI-1 | M |
| 3.3.2 | `POST /pos/sales` batch ingestion (lines, tenders, customer ref) | PAPI-2 | L |
| 3.3.3 | Idempotency by `(terminal_id, receipt_number)` | PAPI-3 | M |
| 3.3.4 | Batch up to 500 receipts, partial-success per-receipt status | PAPI-4 | M |
| 3.3.5 | Accepted sale → Sales Invoice (or aggregated daily invoice, configurable), stock + GL posting with branch dimension | PAPI-5 | L |
| 3.3.6 | Item resolution by SKU/barcode; explicit rejection of unresolved items | PAPI-6 | S |
| 3.3.7 | `POST /pos/shifts/close`: declared/expected cash, card totals, variance → Shift Reconciliation + variance posting | PAPI-7 | M |
| 3.3.8 | `POST /pos/returns`: validate against original receipt, post credit note + stock receipt | PAPI-8 | M |
| 3.3.9 | `GET /pos/catalogue` with `updated_since` incremental sync | PAPI-9 | M |
| 3.3.10 | `GET /pos/stock` for terminal's warehouse | PAPI-10 | S |
| 3.3.11 | Webhook/polling endpoint for price & promotion changes | PAPI-11 | M |
| 3.3.12 | Terminal master (last-seen, unsynced count) + full audit logging of API activity | PAPI-12 | M |
| 3.3.13 | Per-terminal rate limiting; suspended-terminal distinguishable status code | PAPI-13 | S |
| 3.3.14 | Reference TypeScript sample client demonstrating offline queueing + retry | — | M |

---

## Phase 4 — Payroll + LK Statutory (~3 months)

**Gate:** parallel run against a real payroll matches to the cent, verified
by a CA.

### Epic 4.1 — HR / Payroll Core (`MOD-HR`)

| Task | SRS Ref | Size |
|---|---|---|
| 4.1.1 | Employee master (personal, employment, bank, statutory IDs, reporting manager) | HR-1 | M |
| 4.1.2 | Department, Designation, Employment Type, Branch masters | HR-2 | S |
| 4.1.3 | Salary Component master (earning/deduction, taxable flag, EPF-eligible flag, formula/fixed) | HR-3 | M |
| 4.1.4 | Salary Structure with component list, effective-dated employee assignment | HR-4 | L |
| 4.1.5 | Attendance capture: manual, bulk upload, biometric import | HR-5 | L |
| 4.1.6 | Leave types: entitlement, accrual, carry-forward, encashment rules | HR-6 | L |
| 4.1.7 | Leave application with approval workflow + balance validation | HR-7 | M |
| 4.1.8 | Payroll Entry: employee filter selection, salary slip generation, review, submit | HR-8 | L |
| 4.1.9 | Salary Slip (earnings, deductions, employer contributions, net pay) | HR-9 | M |
| 4.1.10 | Payroll → GL posting (salary expense, statutory liabilities, net payable) | HR-10 | M |
| 4.1.11 | Off-cycle payment / arrears processing | HR-11 | M |
| 4.1.12 | Loan/advance management with payroll instalment recovery | HR-12 | M |
| 4.1.13 | Final settlement on resignation (gratuity + leave encashment) | HR-13 | L |

### Epic 4.2 — Sri Lanka Statutory Calculations (`MOD-LK`)

**Non-negotiable per SRS. Build as pure, unit-tested functions independent of
persistence (LK-14); 100% branch coverage required (test requirement in §4.6).**

| Task | SRS Ref | Size |
|---|---|---|
| 4.2.1 | Date-effective rate/threshold/slab configuration model (never hardcoded) | LK-1 | M |
| 4.2.2 | EPF calculation: employee 8% / employer 12% of eligible earnings, configurable eligible components, overtime excluded by default | LK-2 | M |
| 4.2.3 | ETF calculation: employer 3% of EPF-eligible earnings | LK-3 | S |
| 4.2.4 | APIT progressive-slab calculation per IRD tables, EPF deduction before computation, date-effective slab sets | LK-4 | L |
| 4.2.5 | Gratuity: half-month per year after 5 years completed service (Payment of Gratuity Act) | LK-5 | M |
| 4.2.6 | Historic recalculation uses period-effective rate set, not current | LK-6 | M |
| 4.2.7 | Unit test matrix per calculation: zero, below-threshold, each slab boundary, above-top-slab — 100% branch coverage gate in CI | LK-1..6 | XL |
| 4.2.8 | VAT: output/input tracking, configurable rate, return schedule | LK-10 | L |
| 4.2.9 | SVAT: suspended supply handling, credit vouchers, schedule generation | LK-11 | L |
| 4.2.10 | Withholding tax on supplier payments + certificate generation | LK-12 | M |
| 4.2.11 | Stamp duty tracking where applicable | LK-13 | S |

### Epic 4.3 — Statutory Returns & Reminders

| Task | SRS Ref | Size |
|---|---|---|
| 4.3.1 | EPF C Form generation in CBSL-submitted format | LK-7 | M |
| 4.3.2 | ETF return generation in submitted format | LK-8 | M |
| 4.3.3 | Statutory remittance calendar with deadline reminders (EPF 15th, ETF last working day+1 month) | LK-9 | M |

**Risk mitigation task:** schedule mandatory CA-verified parallel payroll run
before any go-live (see §9 risk register).

---

## Phase 5 — Bank Files + Reporting (~2 months)

**Gate:** generated files accepted by all four bank portals on a live upload
test.

### Epic 5.1 — Bank Payment Files (`MOD-BANK`)

| Task | SRS Ref | Size |
|---|---|---|
| 5.1.1 | Bank File Format master (bank, format code, delimiter, header/trailer, date/amount format, column mapping) | BNK-1 | M |
| 5.1.2 | Generator as strategy interface, one concrete class per bank | BNK-2 | M |
| 5.1.3 | Format implementations: Commercial Bank, HNB, Sampath, BOC — **obtain real sample files from each bank before building** (risk mitigation) | BNK-3 | XL |
| 5.1.4 | Bulk Payment Batch from Payroll Entry or approved Purchase Invoices | BNK-4 | L |
| 5.1.5 | Pre-generation validation (missing account/bank-branch code, zero amount, duplicate account) with exception report | BNK-5 | M |
| 5.1.6 | Generated file attached to batch, downloadable, batch marked generated (timestamp+user) | BNK-6 | S |
| 5.1.7 | Regeneration requires elevated permission + audit | BNK-7 | S |
| 5.1.8 | Batch totals (count, amount) for manual bank-portal verification | BNK-8 | S |
| 5.1.9 | Statutory remittance files for EPF/ETF via same mechanism | BNK-9 | M |
| 5.1.10 | Live upload test against all four bank portals (gate criterion) | BNK-3, BNK-10 | M |

### Epic 5.2 — Reporting & Dashboard Completion

| Task | Description |
|---|---|
| 5.2.1 | Report builder UI on top of Phase-0 metadata engine (0.10.1) | |
| 5.2.2 | Configurable dashboard widget grid populated with cross-module KPIs | RPT-5 |
| 5.2.3 | Scheduled report delivery rollout across modules | RPT-4 |

---

## Phase 6 — Hardening (~2 months)

**Gate:** penetration test passed; load test at 100 concurrent users.

### Epic 6.1 — Performance

| Task | SRS Ref |
|---|---|
| 6.1.1 | Load test: API p95 <500ms at 100 concurrent users on transactional endpoints | NFR-P1 |
| 6.1.2 | Audit all list endpoints for server-side pagination (no unbounded result sets) | NFR-P2 |
| 6.1.3 | Payroll run performance test: 500 employees within 3 minutes | NFR-P3 |
| 6.1.4 | Stock ledger partitioning by company+year, performance test at 10M entries | NFR-P5 |

### Epic 6.2 — Security

| Task | SRS Ref |
|---|---|
| 6.2.1 | TLS 1.3 in transit, AES-256 at rest (DB + object storage) | NFR-S1 |
| 6.2.2 | Field-level encryption: salary amounts, bank account numbers, NIC numbers | NFR-S2 |
| 6.2.3 | OWASP Top 10 review; CI dependency scan blocking on high-severity CVE (extends 0.0.4) | NFR-S3 |
| 6.2.4 | Rate limiting per user and per IP (platform-wide, beyond POS terminal limiting) | NFR-S4 |
| 6.2.5 | Secrets audit: externalise all config via env/vault | NFR-S5 |
| 6.2.6 | Re-run tenant isolation adversarial test suite at scale | NFR-S6 |
| 6.2.7 | PDPA (Sri Lanka Act No. 9 of 2022) workflows: data subject access, correction, erasure | NFR-S7 |
| 6.2.8 | External penetration test (gate criterion) | — |

### Epic 6.3 — Reliability

| Task | SRS Ref |
|---|---|
| 6.3.1 | Automated daily backup, 30-day retention, weekly restore verification | NFR-R1 |
| 6.3.2 | Point-in-time recovery drill: verify RPO 1h / RTO 4h | NFR-R2 |
| 6.3.3 | Graceful degradation: confirm reporting failure never blocks transaction posting | NFR-R3 |
| 6.3.4 | Idempotency audit across all submission endpoints | NFR-R4 |

### Epic 6.4 — Usability & i18n

| Task | SRS Ref |
|---|---|
| 6.4.1 | Responsive audit from 360px; tablet usability pass on core transactional screens | NFR-U1 |
| 6.4.2 | i18n rollout: English/Sinhala/Tamil via next-intl, externalise all strings | NFR-U2 |
| 6.4.3 | Keyboard-first data entry audit on high-volume screens (invoice lines, stock entry) | NFR-U3 |
| 6.4.4 | Inline field-level validation audit (no generic failure toasts) | NFR-U4 |
| 6.4.5 | WCAG 2.1 AA accessibility audit | NFR-U5 |

### Epic 6.5 — Deployment & Documentation

| Task | SRS Ref |
|---|---|
| 6.5.1 | On-prem single `docker compose` deployment package | NFR-D1 |
| 6.5.2 | Kubernetes deployment for hosted multi-tenant clients | NFR-D2 |
| 6.5.3 | Zero-downtime deployment drill with backward-compatible migration | NFR-D3 |
| 6.5.4 | Health/readiness/metrics endpoints wired to Prometheus + Grafana, logs to Loki/ELK, errors to Sentry | NFR-D4 |
| 6.5.5 | Full data export tool, producing a restorable dataset (gate criterion 7) | NFR-D5 |
| 6.5.6 | Consultant runbook: zero-to-first-invoice setup in under 4 hours (Acceptance Criterion 1) | — |

---

## Cross-Cutting / Always-On Tracks

These aren't a phase — they run continuously starting Phase 0:

- **Test coverage gates:** 80% overall unit coverage, 100% branch coverage on
  financial (`MOD-FIN`) and statutory (`MOD-LK`) packages — enforced in CI,
  not just checked at the end (NFR-M1).
- **Integration tests per document lifecycle** using Testcontainers, added as
  each document type ships (NFR-M2).
- **ArchUnit module-boundary enforcement**, extended as new modules land
  (NFR-M3).
- **Nightly stock↔GL and inter-branch clearing reconciliation jobs**, live
  from the first module that posts to both (risk mitigation, §9).

---

## Risk-Driven Tasks (SRS §9)

| Risk | Task(s) already listed above |
|---|---|
| Statutory calculation error in production | 4.2.7 (100% branch coverage), CA-verified parallel run before go-live |
| Bank file format rejection at go-live | 5.1.3 (real sample files first), 5.1.10 (live portal test) |
| Stock/GL divergence | 2.1.4 (same-transaction posting), 2.1.13 (nightly reconciliation + alert) |
| Backdated entries corrupting valuation | 2.1.12 (auto-revaluation + period-lock block) |
| Scope expansion during implementations | Not an engineering task — contract/process control (fixed-scope, billed change requests) |
| Build completes with no customers | Not an engineering task — business/sales gate before Phase 1 |

---

## Deferred to v2.0 (parking lot — no tasks planned)

Native POS application (§4.9, its own 3–4 month track with dedicated
engineers — do not fold into any phase above), manufacturing (BOM/work
orders/MRP/capacity), fixed asset depreciation, project accounting/timesheets,
CRM pipeline, quality management, maintenance management, e-commerce
integration, direct bank API integration, BI layer, mobile apps,
inter-company transactions, cross-company consolidation.

---

## Acceptance Criteria Traceability (§10)

| # | Criterion | Primarily verified by |
|---|---|---|
| 1 | Zero-to-first-invoice in <4 hours | 0.11.2, 0.11.3, F0.11.2, F0.11.3, 6.5.6 |
| 2 | Trial balance balances across 10,000 mixed transactions | 1.1.1, 1.5.1 |
| 3 | Stock valuation reconciles exactly to inventory GL | 2.1.4, 2.1.13 |
| 4 | 200-employee payroll matches accountant calc to the cent | Phase 4 gate |
| 5 | Bank files upload successfully to all four portals | 5.1.10 |
| 6 | Tenant isolation verified by adversarial test | 0.1.4, 6.2.6 |
| 7 | Full data export produces a restorable dataset | 6.5.5 |
| 8 | New white-label brand provisioned in <1 hour, no code/redeploy, no leaked default-brand string | 0.8.17 |
