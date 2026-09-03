/**
 * Primary nav sections, one per backend module (see root CLAUDE.md's
 * module list). No screens exist behind most of these yet — each becomes a
 * real route as its module's frontend epic lands; until then they render
 * as disabled placeholders instead of dead links.
 */
export interface NavItem {
  label: string;
  href?: string;
}

export const NAV_ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/" },
  // F0.4.3: cross-module — approval tasks can belong to any document type,
  // so this sits alongside Dashboard rather than under one business module.
  { label: "Approvals", href: "/approvals" },
  { label: "Finance" },
  { label: "Inventory" },
  { label: "Procurement" },
  { label: "Sales" },
  { label: "Payroll" },
  { label: "Statutory" },
  { label: "Banking" },
  // F0.6: Master Data's own admin screens, same flat pattern as the IAM/
  // workflow config screens below rather than a second nav surface.
  { label: "Companies", href: "/admin/companies" },
  { label: "Chart of Accounts", href: "/admin/chart-of-accounts" },
  { label: "Cost Centres", href: "/admin/cost-centres" },
  { label: "Customers & Suppliers", href: "/admin/business-partners" },
  { label: "Items", href: "/admin/items" },
  { label: "Units of Measure", href: "/admin/units-of-measure" },
  { label: "Currencies", href: "/admin/currencies" },
  { label: "Fiscal Years", href: "/admin/fiscal-years" },
  // IAM isn't a business module, but F0.2.5/F0.2.7 land its admin screens
  // here rather than inventing a second nav surface just for them.
  { label: "Roles & Permissions", href: "/admin/roles" },
  { label: "SoD Rules", href: "/admin/sod-rules" },
  { label: "Audit Log", href: "/admin/audit-log" },
  { label: "Setup Checklist", href: "/admin/setup-checklist" },
  { label: "Approval Chains", href: "/admin/workflow-chains" },
  { label: "Naming Series", href: "/admin/numbering-series" },
  // F0.7: Documents & Attachments' own admin screen, same flat pattern.
  { label: "Print Formats", href: "/admin/print-formats" },
  // F0.11.1/F0.11.4: platform- and brand-admin consoles are a separate
  // operator persona from the tenant-scoped items above (ADM-1/ADM-5 design
  // note) — same flat, unfiltered nav pattern as the rest of this list until
  // permission-aware nav exists; the backend enforces access either way. A
  // Brand admin's own console lives at /admin/brands/{brandId}, which this
  // nav can't link to directly since there's no "my brand" lookup endpoint
  // yet — they reach it from the link the platform admin gives them, or via
  // Platform Admin > Brands for platform admins browsing all of them.
  { label: "Platform Admin", href: "/admin/platform/brands" },
];
