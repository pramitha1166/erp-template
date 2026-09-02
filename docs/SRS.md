# Eudext ERP — Software Requirements Specification

**Version:** 1.0 (Draft)
**Owner:** Eudext
**Target market:** Sri Lankan SMEs, 20–200 employees
**Stack:** Java 21 / Spring Boot 3.x · PostgreSQL · Next.js 15 · TypeScript

---

## 1. Purpose and Scope

### 1.1 Purpose
Define functional and non-functional requirements for a multi-tenant, modular ERP platform serving Sri Lankan importers, distributors, manufacturers and retail chains.

### 1.2 In Scope (v1.0)
Financial accounting, inventory, procurement, sales, payroll, multi-branch operations, POS integration API, white-label branding, and the Sri Lanka statutory/banking layer.

### 1.3 Out of Scope (v1.0)
Manufacturing MRP, quality management, fixed asset depreciation automation, CRM pipeline, project accounting, native POS application, e-commerce sync. Deferred to v2.0+.

### 1.4 Definitions

| Term | Meaning |
|---|---|
| Tenant | A customer organisation with isolated data |
| Company | A legal entity within a tenant (multi-company support) |
| Document | Any transactional record with a lifecycle (Draft → Submitted → Cancelled) |
| GL | General Ledger |
| GRN | Goods Received Note |
| APIT | Advance Personal Income Tax (Sri Lanka) |
| SVAT | Simplified Value Added Tax (Sri Lanka) |

---

## 2. Architecture Requirements

### 2.1 System Architecture

```
┌──────────────────────────────────────────────┐
│  Next.js 15 (App Router) — SSR + Client       │
│  TanStack Query · Zustand · shadcn/ui         │
└────────────────────┬─────────────────────────┘
                     │ REST + JWT
┌────────────────────▼─────────────────────────┐
│  Spring Boot API Gateway                      │
│  Spring Security · OAuth2 Resource Server     │
├───────────────────────────────────────────────┤
│  Domain Modules (Spring Modulith)             │
│  finance · inventory · procurement · sales    │
│  payroll · statutory · banking · masterdata   │
├───────────────────────────────────────────────┤
│  Platform Services                            │
│  audit · workflow · numbering · notification  │
│  documents · reporting · scheduler            │
└────────┬──────────────────┬──────────────────┘
         │                  │
   ┌─────▼─────┐      ┌─────▼─────┐
   │PostgreSQL │      │  Redis    │
   │  (RLS)    │      │ cache/jobs│
   └───────────┘      └───────────┘
```

### 2.2 Architectural Requirements

**ARCH-1** — Modular monolith using Spring Modulith. Modules communicate via published events and explicit APIs, not direct repository access across boundaries. Microservices are explicitly rejected for v1.0.

**ARCH-2** — Multi-tenancy by shared schema with a `tenant_id` discriminator on every table, enforced by PostgreSQL Row-Level Security. Tenant context resolved from JWT and set as a session variable per request.

**ARCH-3** — Every transactional entity implements a common `Document` supertype with fields: `id`, `tenantId`, `companyId`, `docNumber`, `docStatus`, `postingDate`, `createdBy`, `createdAt`, `modifiedBy`, `modifiedAt`, `version`.

**ARCH-4** — Document lifecycle is a state machine: `DRAFT → SUBMITTED → CANCELLED`, with `AMENDED` creating a new linked document. Submitted documents are immutable except for designated amendable fields.

**ARCH-5** — All monetary values use `BigDecimal` with scale 4 internally, displayed at currency precision. `double`/`float` are prohibited for money by static analysis rule.

**ARCH-6** — Optimistic locking via `@Version` on all aggregates.

**ARCH-7** — Database migrations via Flyway. No Hibernate DDL auto-generation in any environment.

---

## 3. Platform Services (Build First)

These underpin every module and must exist before functional work begins.

### 3.1 Identity and Access — `PLAT-IAM`

| ID | Requirement |
|---|---|
| IAM-1 | Users authenticate via email/password with Argon2id hashing; JWT access token (15 min) + refresh token (7 days, rotating) |
| IAM-2 | TOTP-based 2FA, mandatory for users holding approval permissions |
| IAM-3 | Role-Based Access Control: permissions are `module:entity:action` triples (e.g. `finance:journal-entry:submit`) |
| IAM-4 | Roles assignable per company; a user may hold different roles in different companies of the same tenant |
| IAM-5 | Field-level permissions: designated fields readable/writable by role (e.g. salary visible only to HR roles) |
| IAM-6 | Record-level permissions: restrict by warehouse, cost centre, or branch |
| IAM-7 | Segregation of Duties rules — configurable conflicting-permission pairs that block assignment (e.g. create supplier + approve payment) |
| IAM-8 | Session management: list active sessions, force logout, configurable idle timeout |
| IAM-9 | Password policy: length, complexity, history, expiry — configurable per tenant |
| IAM-10 | Full audit of all auth events: login success/failure, permission grant/revoke, role change |

### 3.2 Audit Trail — `PLAT-AUDIT`

| ID | Requirement |
|---|---|
| AUD-1 | Every insert/update/delete on transactional and master data written to an append-only audit table |
| AUD-2 | Audit record captures: entity type, entity id, action, changed fields (old → new as JSONB), user, timestamp, IP, request id |
| AUD-3 | Audit records are immutable; no application path permits update or delete |
| AUD-4 | Document version history viewable in UI with field-level diff |
| AUD-5 | Audit retention minimum 7 years; archival to cold storage after 2 years |

### 3.3 Workflow Engine — `PLAT-WF`

| ID | Requirement |
|---|---|
| WF-1 | Configurable multi-level approval chains per document type, per company |
| WF-2 | Approval conditions expressible on document fields (e.g. amount > 500,000 requires Director) |
| WF-3 | Approver resolution by role, by named user, or by reporting hierarchy |
| WF-4 | Parallel and sequential approval steps |
| WF-5 | Delegation with date range; escalation on timeout |
| WF-6 | Rejection returns document to draft with mandatory comment |
| WF-7 | Approval history visible on document with timestamps and comments |
| WF-8 | Email and in-app notification on pending approval |

**Implementation note (added post-build, Epic 0.4/#5):** WF-6 reads as
"rejection returns document to draft," which could be misread as a
`SUBMITTED → DRAFT` transition — ARCH-4 defines no such transition. The
built contract is: a document-owning module calls
`WorkflowApi.startApproval()` before `Document.submit()`, and only calls
`submit()` once the approval instance reaches `APPROVED`. A rejection
therefore satisfies WF-6 by never letting the document leave `DRAFT` in
the first place, not by reversing a submission.

### 3.4 Numbering — `PLAT-NUM`

| ID | Requirement |
|---|---|
| NUM-1 | Configurable naming series per document type per company: prefix, date components, counter width |
| NUM-2 | Gapless sequences for statutory documents (invoices, credit notes) — required for tax audit |
| NUM-3 | Series resettable per fiscal year with configurable behaviour |
| NUM-4 | Concurrency-safe allocation under load |

### 3.5 Master Data — `PLAT-MDM`

| ID | Requirement |
|---|---|
| MDM-1 | Company: legal name, registration number, VAT number, address, base currency, fiscal year, logo |
| MDM-2 | Multi-company within tenant; inter-company transactions supported in v2.0 |
| MDM-3 | Chart of Accounts: hierarchical tree, account types (Asset/Liability/Equity/Income/Expense), group vs ledger accounts |
| MDM-4 | Cost Centres: hierarchical, assignable to transactions |
| MDM-5 | Customers, Suppliers: contact details, tax registration, credit terms, default accounts, bank details |
| MDM-6 | Items: code, name, UOM, item group, valuation method, reorder level, batch/serial flags, tax category, HS code |
| MDM-7 | UOM with conversion factors; purchase UOM may differ from stock UOM |
| MDM-8 | Currency master with date-effective exchange rates; manual entry plus optional CBSL rate import |
| MDM-9 | Fiscal Year and Accounting Periods with open/closed status |
| MDM-10 | All master data soft-deletable only (disabled flag), never hard-deleted once referenced |

### 3.6 Reporting — `PLAT-RPT`

| ID | Requirement |
|---|---|
| RPT-1 | Report definitions stored as metadata; new reports addable without redeployment |
| RPT-2 | Standard filters: date range, company, cost centre, with saved filter sets per user |
| RPT-3 | Export to XLSX, CSV, PDF |
| RPT-4 | Scheduled report delivery by email |
| RPT-5 | Dashboard with configurable widget grid; number cards, charts, and lists |
| RPT-6 | Reports execute against read replicas where configured |

### 3.7 Documents and Attachments — `PLAT-DOC`

| ID | Requirement |
|---|---|
| DOC-1 | File attachment to any document; S3-compatible object storage |
| DOC-2 | Configurable print formats per document type with template editor |
| DOC-3 | PDF generation server-side, deterministic output |
| DOC-4 | Virus scanning on upload |
| DOC-5 | Attachment access respects parent document permissions |

### 3.8 Branding and White-Labelling — `PLAT-BRAND`

**Two distinct models must be supported.**

- **Model A — Client branding.** Each customer organisation sees its own logo and colours inside the product. The product is still identifiably Eudext.
- **Model B — Reseller white-label.** A partner resells the platform under their own product name and domain. No Eudext reference is visible anywhere to their end users.

Model B is the harder constraint and drives the requirements below. Building for A only, then retrofitting B, is a rewrite.

| ID | Requirement |
|---|---|
| BRD-1 | A `Brand` entity sits above `Tenant`. Every tenant belongs to exactly one brand. Brand carries all identity: product name, short name, logo (light/dark/icon), favicon, colour tokens, font stack, support email, support phone, legal entity name, terms URL, privacy URL |
| BRD-2 | Zero hardcoded product name, logo path, colour, or URL anywhere in backend or frontend source. Static analysis rule fails the build on literal occurrences of the default brand name outside the seed data file |
| BRD-3 | Brand resolution by request host. Next.js middleware maps `Host` header → brand; backend resolves brand from JWT claim. Unknown host returns a neutral error page, never a default brand |
| BRD-4 | Custom domain per brand with automated TLS certificate provisioning (ACME). Subdomain fallback (`{brand}.platform.tld`) available immediately on brand creation |
| BRD-5 | Theming via CSS custom properties resolved at runtime from brand config — not build-time compilation. Adding a brand must not require a frontend rebuild or redeploy |
| BRD-6 | Theme tokens cover: primary, primary-foreground, secondary, accent, destructive, muted, background, foreground, border, radius, and font family. Light and dark variants |
| BRD-7 | Brand assets stored in object storage, served via CDN, cache-busted by version hash |
| BRD-8 | Transactional email templates carry brand logo, colours, sender name, and reply-to. Sending domain configurable per brand with DKIM/SPF verification status surfaced in the admin UI |
| BRD-9 | PDF print formats (invoice, salary slip, statement, purchase order) render brand assets. Print format templates overridable per brand and per tenant |
| BRD-10 | Login, password reset, error and maintenance pages are brand-aware — these are the highest-visibility surfaces and the most commonly missed |
| BRD-11 | Page title, meta tags, Open Graph image, PWA manifest and app icons are brand-derived |
| BRD-12 | Feature entitlements configurable per brand: a brand may enable or hide entire modules. Hidden modules are absent from navigation, permission lists, and API surface |
| BRD-13 | Per-brand help content and documentation URLs; in-app support widget routes to the brand's own support channel |
| BRD-14 | Brand-level administration console: partner can manage their own tenants, users and branding without platform-owner access |
| BRD-15 | Per-brand usage reporting for partner billing (tenants, active users, storage, transaction volume) |
| BRD-16 | Sri Lanka statutory module (`MOD-LK`) is a feature entitlement, not a core assumption — a brand serving another market must be able to disable it cleanly |

**Design rule:** brand configuration is data, never deployment. Onboarding a new white-label partner must be a records-and-DNS operation completable in under an hour, with no code change, no rebuild, and no release.

**Anti-requirement:** per-brand code forks are prohibited. Any partner-specific behaviour must be expressed as configuration, entitlement, or an override record. One codebase, always.

### 3.9 Branch and Organisational Dimensions — `PLAT-ORG`

Branch is a first-class posting dimension, not an attribute. Retail chains, multi-outlet distributors and manufacturers with regional depots require branch-level accountability across every module.

**Hierarchy:** `Tenant → Brand → Company → Branch → Warehouse`. A branch belongs to one company. A branch may own multiple warehouses. A warehouse belongs to exactly one branch.

| ID | Requirement |
|---|---|
| ORG-1 | Branch master: code, name, parent branch (hierarchical), company, address, contact, cost centre default, warehouse defaults, tax registration if separate, active flag |
| ORG-2 | Branch is a mandatory dimension on every transactional document and on every `GLEntry` and `StockLedgerEntry` row. Posting without a branch is rejected at the persistence layer |
| ORG-3 | Branch hierarchy supports rollup: a region node aggregates its child branches in all reporting |
| ORG-4 | Users are assigned to one or more branches. Record visibility is filtered by branch assignment; a user sees only documents for branches they hold |
| ORG-5 | Branch-scoped roles: a user may be Manager in Branch A and Viewer in Branch B within the same company |
| ORG-6 | Document numbering series configurable per branch (e.g. `INV-COL-2026-00001`, `INV-KDY-2026-00001`) |
| ORG-7 | Approval workflows resolvable by branch — branch manager approves up to a limit, regional above it |
| ORG-8 | Inter-branch stock transfer with in-transit accounting: stock leaves source branch, sits in a transit warehouse, is received at destination. Variance on receipt raises a discrepancy record |
| ORG-9 | Inter-branch transfers post to inter-branch clearing accounts; these must net to zero at company level and are reported as such |
| ORG-10 | Branch-wise reports: Trial Balance, P&L, Stock Balance, Sales Register, AR/AP Ageing, all filterable and groupable by branch and by branch hierarchy node |
| ORG-11 | Consolidated company view available to users with company-level permission, showing all branches with drill-down |
| ORG-12 | Branch-level budgets with variance reporting against actuals |
| ORG-13 | Branch opening and closing: a closed branch blocks new postings but retains full history and reporting |
| ORG-14 | Cost Centre remains an independent dimension, orthogonal to Branch. A document carries both |
| ORG-15 | Dimension model is extensible: additional analytical dimensions (Project, Territory, Product Line) definable as configuration without schema change |

**Design note:** ORG-2 is the requirement that must not be compromised. Adding a mandatory dimension to the ledger after data exists means backfilling every historic row with a guess, and every report written before it becomes wrong. Design the ledger tables with `branch_id` NOT NULL from the first migration.

---

### 3.10 Tenant Onboarding and Administration — `PLAT-ADMIN`

**Gap identified post-draft (addendum).** BRD-14 specifies a brand-level
partner console but stops there; nothing in v1.0 specifies who provisions a
*Brand* itself, and Acceptance Criterion 1 ("a company can be set up from
zero to first invoice in under 4 hours") implies an onboarding workflow
without ever defining one. Two consoles are required, at two different
altitudes, plus the onboarding flow both of them drive:

- **Platform Admin Console** — Eudext's own operators. Creates and manages
  Brands, sees usage across all brands, sets platform-wide default
  entitlements.
- **Brand Admin Console** — the reseller partner (BRD-14). Creates and
  manages the Tenants under their own Brand only.
- **Tenant onboarding workflow** — what either console triggers to actually
  stand up a new Tenant: first Company, first admin user, seeded master
  data, ready for its first transaction.

| ID | Requirement |
|---|---|
| ADM-1 | Platform Admin Console (Eudext operator role, distinct from any tenant or brand role): create, suspend, and reactivate Brands; view a cross-brand tenant/usage summary; set platform-wide default feature entitlements that brands inherit unless overridden (BRD-12) |
| ADM-2 | Tenant onboarding workflow: creates a Tenant under a Brand, its first Company (MDM-1 fields), the initial tenant-admin user, and an assigned entitlement/plan set, in one guided flow — completable in under 4 hours per Acceptance Criterion 1 |
| ADM-3 | Onboarding seeds default data so the tenant is transaction-ready: a Chart of Accounts template (localised for Sri Lanka where `MOD-LK` is entitled), default numbering series (NUM-1), and a default fiscal year/accounting period (MDM-9) |
| ADM-4 | Post-onboarding guided setup checklist shown to the tenant admin, tracking completion of: branches, users/roles, chart of accounts review, opening balances, first item/customer/supplier, first invoice |
| ADM-5 | Brand Admin Console (detail on BRD-14): brand partner staff create/suspend Tenants within their own Brand only, invite tenant-admin users, view per-tenant usage (BRD-15), and assign tenant-level entitlements bounded by what the Brand itself is entitled to |
| ADM-6 | Tenant suspension and reactivation: a suspended tenant blocks new user logins and new transactional postings, but retains full data and read/report access for platform and brand admins |
| ADM-7 | Support impersonation: a platform or brand admin may start a time-boxed, scoped session as a tenant-admin for support purposes. Every impersonated action is written to the audit trail (AUD-1/AUD-2) explicitly tagged as impersonated, and the tenant is notified when it happens |
| ADM-8 | Tenant-initiated data export and erasure requests (PDPA, NFR-S7) are logged, tracked to completion, and actioned from the admin console, reusing the full data export capability (NFR-D5) |
| ADM-9 | Platform-level usage and health dashboard: tenant count, active users, storage consumption, and transaction volume, rolled up by Brand, plus basic system health indicators |

**Design note:** ADM-1 and ADM-5 are genuinely different roles at different
altitudes — a Brand admin must never see another Brand's tenants, and a
Tenant admin must never see the admin console at all. Model these as
distinct permission scopes from the start, not as "IAM-3 roles with a
higher number."

---

## 4. Functional Modules

### 4.1 Financial Accounting — `MOD-FIN`

**General Ledger**

| ID | Requirement |
|---|---|
| FIN-1 | Double-entry enforced at the persistence layer; no path permits an unbalanced posting |
| FIN-2 | Every GL entry carries: account, debit, credit, posting date, company, cost centre, party (optional), voucher type, voucher number |
| FIN-3 | Journal Entry supports manual multi-line postings with attachment and narration |
| FIN-4 | Reversal of a posted entry creates a linked contra entry; original never mutated |
| FIN-5 | Accounting period lock prevents posting to closed periods; unlock requires elevated permission and is audited |
| FIN-6 | Year-end closing: P&L accounts closed to retained earnings, opening balances carried forward |
| FIN-7 | Multi-currency: transaction currency, company base currency, exchange rate, and realised/unrealised gain-loss calculation |

**Accounts Receivable**

| ID | Requirement |
|---|---|
| FIN-8 | Sales Invoice posts to GL, updates customer balance, and reduces stock where applicable |
| FIN-9 | Credit Note with reference to original invoice |
| FIN-10 | Payment Entry with allocation across multiple invoices, partial payments, and advances |
| FIN-11 | Customer statement and ageing analysis (30/60/90/120+) |
| FIN-12 | Credit limit enforcement with configurable block or warn |

**Accounts Payable**

| ID | Requirement |
|---|---|
| FIN-13 | Purchase Invoice with three-way match against PO and GRN; configurable tolerance |
| FIN-14 | Debit Note with reference to original invoice |
| FIN-15 | Payment Entry with allocation, advances, and withholding tax deduction |
| FIN-16 | Supplier ageing and payment due report |

**Banking**

| ID | Requirement |
|---|---|
| FIN-17 | Bank account master linked to GL account |
| FIN-18 | Bank statement import (CSV/MT940) with configurable column mapping per bank |
| FIN-19 | Reconciliation screen: auto-match by amount/date/reference, manual match, unmatched queue |
| FIN-20 | Petty cash and cash book |

**Reports**

Trial Balance · General Ledger · Balance Sheet · Profit & Loss · Cash Flow (indirect) · AR/AP Ageing · Customer/Supplier Ledger · Cost Centre P&L · Day Book

---

### 4.2 Inventory — `MOD-INV`

| ID | Requirement |
|---|---|
| INV-1 | Multi-warehouse with hierarchical warehouse tree |
| INV-2 | Stock Ledger Entry as the single source of stock truth; quantity and value posted together |
| INV-3 | Valuation methods: FIFO, Moving Average, and Specific (batch) — set per item |
| INV-4 | Perpetual inventory: every stock movement posts corresponding GL entries |
| INV-5 | Batch tracking with manufacture and expiry dates; FEFO picking option |
| INV-6 | Serial number tracking with full movement history per serial |
| INV-7 | Stock Entry types: Receipt, Issue, Transfer, Manufacture, Repack |
| INV-8 | Stock Reconciliation for physical count adjustment with variance GL posting |
| INV-9 | Reorder level with automatic reorder request generation |
| INV-10 | Negative stock blocked by default; permitted only by configuration with elevated permission |
| INV-11 | Landed cost voucher: apportion freight/duty/clearing across received items by amount or quantity |
| INV-12 | Backdated stock entry triggers revaluation of all subsequent entries for affected items |

**Reports**

Stock Balance · Stock Ledger · Stock Ageing · Batch Expiry · Item-wise Valuation · Warehouse-wise Stock · Slow Moving Items · Reorder Report

---

### 4.3 Procurement — `MOD-PRC`

| ID | Requirement |
|---|---|
| PRC-1 | Purchase Requisition raised by department, routed through approval |
| PRC-2 | Request for Quotation to multiple suppliers; quotation comparison view |
| PRC-3 | Purchase Order generated from requisition or quotation; approval workflow by value |
| PRC-4 | Partial receipt and partial invoicing against a PO with running balance |
| PRC-5 | Goods Received Note updates stock and creates accrual posting |
| PRC-6 | Quality rejection at GRN with return-to-supplier flow |
| PRC-7 | Purchase Return with stock and GL reversal |
| PRC-8 | Supplier price list with validity dates and quantity breaks |
| PRC-9 | Import documentation: LC reference, BOE number, shipping details on PO/GRN |

---

### 4.4 Sales — `MOD-SLS`

| ID | Requirement |
|---|---|
| SLS-1 | Quotation with validity period and conversion to Sales Order |
| SLS-2 | Sales Order with delivery schedule and partial fulfilment tracking |
| SLS-3 | Price List with customer group, quantity break, and date validity |
| SLS-4 | Discount rules: line-level, document-level, and promotional (buy X get Y) |
| SLS-5 | Delivery Note reducing stock, with linkage to invoice |
| SLS-6 | Sales Invoice with or without preceding delivery note |
| SLS-7 | Sales Return with stock and GL reversal |
| SLS-8 | Sales commission calculation per salesperson |
| SLS-9 | Territory and salesperson hierarchy for reporting |

**Reports**

Sales Register · Item-wise Sales · Customer-wise Sales · Salesperson Performance · Gross Margin by Item/Customer · Order Fulfilment Status

---

### 4.5 Payroll and HR — `MOD-HR`

| ID | Requirement |
|---|---|
| HR-1 | Employee master: personal, employment, bank, statutory identifiers, reporting manager |
| HR-2 | Department, Designation, Employment Type, Branch |
| HR-3 | Salary Component master: earning or deduction, taxable flag, EPF-eligible flag, formula or fixed |
| HR-4 | Salary Structure with component list, assignable to employee with effective date |
| HR-5 | Attendance capture: manual, bulk upload, or biometric device import |
| HR-6 | Leave types with entitlement, accrual, carry-forward and encashment rules |
| HR-7 | Leave application with approval workflow and balance validation |
| HR-8 | Payroll Entry: select employees by filter, generate salary slips, review, submit |
| HR-9 | Salary Slip shows all earnings, deductions, employer contributions, and net pay |
| HR-10 | Payroll posts to GL: salary expense, statutory liabilities, net payable |
| HR-11 | Off-cycle payment and arrears processing |
| HR-12 | Loan and advance management with instalment recovery from payroll |
| HR-13 | Final settlement on resignation including gratuity and leave encashment |

---

### 4.6 Sri Lanka Statutory Layer — `MOD-LK`

**This module is the commercial differentiator. Requirements here are non-negotiable.**

| ID | Requirement |
|---|---|
| LK-1 | All statutory rates, thresholds and slabs held as date-effective configuration records, never hardcoded |
| LK-2 | EPF: employee 8%, employer 12% of EPF-eligible earnings; eligible components configurable, overtime excluded by default |
| LK-3 | ETF: employer 3% of EPF-eligible earnings |
| LK-4 | APIT: progressive slabs per IRD tables; employee EPF contribution deducted from taxable income before computation; date-effective slab sets |
| LK-5 | Gratuity: half month per year of completed service after 5 years, per Payment of Gratuity Act |
| LK-6 | Historic recalculation must use the rate set effective on the payroll period, not current rates |
| LK-7 | EPF C Form generation in CBSL-submitted format |
| LK-8 | ETF return generation in submitted format |
| LK-9 | Statutory remittance calendar with deadline reminders (EPF 15th, ETF last working day of following month) |
| LK-10 | VAT: output and input tax tracking, configurable rate, VAT return schedule |
| LK-11 | SVAT: suspended supply handling, SVAT credit vouchers, and schedule generation |
| LK-12 | Withholding tax on supplier payments with certificate generation |
| LK-13 | Stamp duty tracking where applicable |
| LK-14 | All calculation logic implemented as pure, unit-tested functions independent of persistence |

**Test requirement:** every statutory calculation must have unit tests covering zero, below-threshold, each slab boundary, and above-top-slab cases. Coverage on this package must be 100% of branches.

---

### 4.7 Bank Payment Files — `MOD-BANK`

| ID | Requirement |
|---|---|
| BNK-1 | Bank File Format master: bank, format code, delimiter, header/trailer structure, date format, amount format, column mapping |
| BNK-2 | Generator implemented as a strategy interface with one concrete class per bank format |
| BNK-3 | Supported at v1.0: Commercial Bank, HNB, Sampath, BOC |
| BNK-4 | Bulk Payment Batch created from a Payroll Entry (salary) or a set of approved Purchase Invoices (suppliers) |
| BNK-5 | Pre-generation validation: missing account number, missing bank/branch code, zero amount, duplicate account within batch — all blocked with a listed exception report |
| BNK-6 | Generated file attached to the batch and downloadable; batch marked as generated with timestamp and user |
| BNK-7 | Regeneration of an already-generated batch requires elevated permission and is audited |
| BNK-8 | Batch totals (record count, total amount) displayed for manual verification against the bank portal |
| BNK-9 | Statutory remittance files for EPF and ETF generated by the same mechanism |
| BNK-10 | System generates files for manual upload to the bank's corporate portal. Direct host-to-host bank integration is explicitly out of scope for v1.0 |

### 4.8 POS Integration API — `MOD-POSAPI`

**v1.0 scope: an integration contract, not a POS application.** Retail clients keep their existing till software and push transactions into the ERP. A native POS is deferred to v2.0 (section 4.9).

| ID | Requirement |
|---|---|
| PAPI-1 | Public REST API, documented in OpenAPI, authenticated by per-terminal API key scoped to one branch and one warehouse |
| PAPI-2 | `POST /pos/sales` accepts a batch of completed sales: terminal id, shift id, receipt number, timestamp, lines (item, qty, unit price, discount, tax), tender lines (cash, card, voucher, credit), customer reference (optional) |
| PAPI-3 | Idempotency by `(terminal_id, receipt_number)` — a resubmitted receipt returns the original result and never double-posts |
| PAPI-4 | Batch submission up to 500 receipts per call; partial success returns per-receipt status with error detail |
| PAPI-5 | Each accepted sale creates a Sales Invoice (or aggregated daily invoice, configurable), reduces stock at the terminal's warehouse, and posts to GL with the terminal's branch dimension |
| PAPI-6 | Item resolution by SKU or barcode; unresolved items rejected with a clear error rather than silently created |
| PAPI-7 | `POST /pos/shifts/close` accepts declared cash, expected cash, card settlement totals, and variance; creates a Shift Reconciliation record and posts variance to a configurable account |
| PAPI-8 | `POST /pos/returns` accepts returns referencing an original receipt; validates against the original and posts a credit note plus stock receipt |
| PAPI-9 | `GET /pos/catalogue` returns items, prices for the terminal's price list, tax rates and barcodes, with an `updated_since` parameter for incremental sync |
| PAPI-10 | `GET /pos/stock` returns current quantity for the terminal's warehouse |
| PAPI-11 | Webhook or polling endpoint for price and promotion changes |
| PAPI-12 | All API activity logged to the audit trail with terminal identity; a Terminal master tracks last-seen timestamp and unsynced receipt count |
| PAPI-13 | Rate limiting per terminal key; suspended terminals rejected with a distinguishable status code |

**Reference implementation requirement:** ship a documented sample client (TypeScript) demonstrating offline queueing and retry, so third-party POS vendors have a working pattern rather than prose.

---

### 4.9 Native POS — `MOD-POS` (v2.0)

Specified here for architectural awareness. Not built in v1.0.

**Treat this as a second product, not a module.** Its failure modes are physical — a till that cannot take payment during a power cut is a business stoppage, not a bug ticket.

| ID | Requirement |
|---|---|
| POS-1 | Offline-first. Full sale, tender and receipt printing with no network. Local storage (IndexedDB or SQLite), sync queue on reconnect, conflict resolution favouring the terminal for completed sales |
| POS-2 | Sub-second item lookup by barcode scan or code entry against a locally cached catalogue |
| POS-3 | Keyboard-first operation; every function reachable without a mouse. Touch layout as an alternative, not a replacement |
| POS-4 | Tender types: cash with change calculation, card via external terminal reference, voucher, loyalty points, customer credit, split across multiple tenders |
| POS-5 | Held bills, recalled bills, and parked orders |
| POS-6 | Line and bill level discounts with permission-gated override; discount above threshold requires supervisor authorisation |
| POS-7 | Returns and exchanges against a receipt, and blind returns with permission |
| POS-8 | Shift management: open with float declaration, cash in/out during shift, close with blind count and variance report |
| POS-9 | Hardware: ESC/POS receipt printers, cash drawer trigger, barcode scanners (HID), customer display, weighing scale |
| POS-10 | Receipt format configurable per brand and per branch, including bilingual receipts |
| POS-11 | Fiscal compliance hooks for future e-invoicing/e-receipt mandates |
| POS-12 | Loyalty: member lookup by phone, points accrual and redemption |
| POS-13 | Terminal provisioning and remote configuration from the ERP admin console |

**Delivery estimate:** 3–4 months for a dedicated pair of engineers, plus a hardware certification period. Do not fold this into an existing phase.

---

## 5. Non-Functional Requirements

### 5.1 Performance

| ID | Requirement |
|---|---|
| NFR-P1 | API p95 response time under 500ms for transactional endpoints at 100 concurrent users |
| NFR-P2 | List views paginated server-side; no endpoint returns unbounded result sets |
| NFR-P3 | Payroll run for 500 employees completes within 3 minutes |
| NFR-P4 | Reports exceeding 5 seconds execute asynchronously with notification on completion |
| NFR-P5 | Stock ledger queries remain performant at 10 million entries — partitioning by company and year |

### 5.2 Security

| ID | Requirement |
|---|---|
| NFR-S1 | TLS 1.3 in transit; AES-256 at rest for database and object storage |
| NFR-S2 | Field-level encryption for salary amounts, bank account numbers, and NIC numbers |
| NFR-S3 | OWASP Top 10 addressed; dependency scanning in CI with build failure on high-severity CVE |
| NFR-S4 | Rate limiting per user and per IP |
| NFR-S5 | No secrets in source; externalised configuration via environment or vault |
| NFR-S6 | Tenant isolation verified by automated test — a request in tenant A must never return tenant B data |
| NFR-S7 | Personal Data Protection Act (Sri Lanka, No. 9 of 2022) compliance: data subject access, correction, and erasure workflows |

### 5.3 Reliability

| ID | Requirement |
|---|---|
| NFR-R1 | Automated daily database backup with 30-day retention; weekly restore verification |
| NFR-R2 | Point-in-time recovery capability, RPO 1 hour, RTO 4 hours |
| NFR-R3 | Graceful degradation: reporting failure must not block transaction posting |
| NFR-R4 | Idempotent handling of duplicate submissions |

### 5.4 Maintainability

| ID | Requirement |
|---|---|
| NFR-M1 | Unit test coverage minimum 80% overall, 100% branch coverage on financial and statutory calculation packages |
| NFR-M2 | Integration tests for every document lifecycle using Testcontainers |
| NFR-M3 | Architecture rules enforced by ArchUnit — module boundary violations fail the build |
| NFR-M4 | OpenAPI specification generated from code, published and versioned |
| NFR-M5 | Structured JSON logging with correlation id propagated across all layers |

### 5.5 Usability

| ID | Requirement |
|---|---|
| NFR-U1 | Responsive from 360px; core transactional screens usable on tablet |
| NFR-U2 | Interface language switchable: English, Sinhala, Tamil. All user-facing strings externalised |
| NFR-U3 | Keyboard-first data entry on high-volume screens (invoice lines, stock entry) |
| NFR-U4 | Inline validation with field-level error messages, never a generic failure toast |
| NFR-U5 | WCAG 2.1 AA compliance |

### 5.6 Deployment

| ID | Requirement |
|---|---|
| NFR-D1 | Containerised; single `docker compose` deployment for on-premise clients |
| NFR-D2 | Cloud multi-tenant deployment on Kubernetes for hosted clients |
| NFR-D3 | Zero-downtime deployment with backward-compatible migrations |
| NFR-D4 | Health, readiness and metrics endpoints exposed for monitoring |
| NFR-D5 | Client data exportable in full on request — no lock-in |

---

## 6. Technology Specification

### 6.1 Backend

```
Java 21 (virtual threads for I/O-bound work)
Spring Boot 3.3+
Spring Modulith          — module boundaries and events
Spring Security          — OAuth2 resource server, method security
Spring Data JPA          — with explicit query tuning
PostgreSQL 16            — RLS for tenancy, partitioning for ledgers
Flyway                   — schema migrations
Redis                    — cache, session, job queue
MapStruct                — DTO mapping
Testcontainers           — integration tests
ArchUnit                 — architecture enforcement
JasperReports or Flying Saucer — PDF generation
```

### 6.2 Frontend

```
Next.js 15 (App Router)
TypeScript (strict)
TanStack Query           — server state
TanStack Table           — data grids
Zustand                  — client state
shadcn/ui + Tailwind     — components
react-hook-form + Zod    — forms and validation
next-intl                — i18n (en/si/ta)
Recharts                 — dashboards
```

### 6.3 Infrastructure

```
Docker + Docker Compose (on-prem)
Kubernetes (hosted)
GitHub Actions           — CI/CD
S3-compatible storage    — attachments
Prometheus + Grafana     — metrics
Loki or ELK              — logs
Sentry                   — error tracking
```

---

## 7. Data Model — Core Entities

Indicative only; full ERD to follow.

```
Tenant ──< Company ──< FiscalYear ──< AccountingPeriod
   │             │
 Brand           ├──< Branch (tree) ──< Warehouse (tree)
                 │                 └──< POSTerminal
                 ├──< Account (tree)
                 ├──< CostCentre (tree)
                 ├──< Customer, Supplier, Item, Employee
                 │
                 └──< Document (abstract)
                       ├── JournalEntry ──< JournalEntryLine
                       ├── SalesInvoice ──< SalesInvoiceLine
                       ├── PurchaseInvoice ──< PurchaseInvoiceLine
                       ├── StockEntry ──< StockEntryLine
                       ├── PaymentEntry ──< PaymentAllocation
                       ├── PayrollEntry ──< SalarySlip ──< SalarySlipComponent
                       ├── BulkPaymentBatch ──< BulkPaymentLine
                       └── ShiftReconciliation

GLEntry        — immutable, one row per debit/credit, references source document
StockLedgerEntry — immutable, quantity + value, references source document
AuditLog       — append-only, JSONB diff
```

**Critical invariant:** `GLEntry` and `StockLedgerEntry` are never updated or deleted. Corrections are new entries. Every reporting number derives from these two tables.

**Second invariant:** `GLEntry` and `StockLedgerEntry` carry `branch_id` NOT NULL from the first migration. This cannot be added later without corrupting historic reporting.

---

## 8. Phasing

| Phase | Duration | Deliverable | Gate |
|---|---|---|---|
| **0. Platform** | 4 months | IAM, audit, workflow, numbering, master data, document lifecycle, brand/theming layer, branch dimension | Tenant isolation test passes; a dummy document completes full lifecycle; a second brand added by config only; branch-filtered visibility verified |
| **1. Finance core** | 3 months | GL, journal entries, AR, AP, trial balance, P&L, balance sheet | Balanced books on a 3-month simulated dataset, verified by an accountant; branch-wise P&L reconciles to company total |
| **2. Inventory + Procurement** | 3.5 months | Stock ledger, valuation, warehouses, inter-branch transfer, PO/GRN, three-way match | Perpetual inventory reconciles to GL to the cent across 1,000 transactions; inter-branch clearing nets to zero |
| **3. Sales + POS API** | 3 months | Quotation → order → delivery → invoice, returns, price lists, POS integration API | Full order-to-cash cycle with correct GL and stock impact; 500-receipt batch posts idempotently from a sample POS client |
| **4. Payroll + LK statutory** | 3 months | Salary structures, payroll run, EPF/ETF/APIT/gratuity, C Form, ETF return | Parallel run against a real payroll matches to the cent, verified by a CA |
| **5. Bank files + reporting** | 2 months | Bulk payment batches, 4 bank formats, dashboard, report builder | Files accepted by all four bank portals on live upload test |
| **6. Hardening** | 2 months | Performance, security audit, i18n, documentation, deployment tooling | Penetration test passed; load test at 100 concurrent users |

**Total: ~20.5 months** for a team of 3–4 engineers, assuming no scope expansion and no client-driven interruption. Native POS (section 4.9) adds a further 3–4 months with dedicated engineers and is not included.

---

## 9. Explicit Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Statutory calculation error in production | Client compliance breach, reputational loss | 100% branch coverage, CA verification, mandatory parallel run before go-live |
| Bank file format rejection at go-live | Payroll delayed, client trust destroyed | Obtain real sample files and test upload before promising any bank |
| Stock/GL divergence | Books irreconcilable, audit failure | Perpetual inventory posting inside the same transaction; nightly reconciliation job with alert |
| Backdated entries corrupting valuation | Silent misstatement of COGS | Automatic revaluation of subsequent entries; block backdating beyond period lock |
| Scope expansion during client implementations | Timeline collapse | Fixed-scope contracts; change requests billed separately |
| Build completes with no customers | Total loss of investment | Signed pilot customer before Phase 1 begins |

---

## 10. Acceptance Criteria for v1.0

1. A company can be set up from zero to first invoice in under 4 hours by a trained consultant.
2. Trial balance balances to zero across 10,000 mixed transactions.
3. Stock valuation reconciles exactly to the inventory GL account.
4. A 200-employee payroll run produces statutory figures matching an accountant's independent calculation to the cent.
5. Generated bank files upload successfully to all four supported bank portals.
6. Tenant isolation verified by automated adversarial test.
7. Full data export produces a restorable dataset.
8. A new white-label brand — name, domain, logo, colour scheme, email sender, PDF templates — is provisioned end to end in under one hour with no code change or redeploy, and no default-brand string appears anywhere in the resulting UI, emails or documents.

---

## Appendix A — Deferred to v2.0

Manufacturing (BOM, work orders, MRP, capacity planning) · Native POS application (specified in 4.9) · Fixed asset register and depreciation · Project accounting and timesheets · CRM pipeline · Quality management · Maintenance management · E-commerce integration · Direct bank API integration · Business intelligence layer · Mobile applications · Inter-company transactions · Consolidation across companies
