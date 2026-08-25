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
  { label: "Finance" },
  { label: "Inventory" },
  { label: "Procurement" },
  { label: "Sales" },
  { label: "Payroll" },
  { label: "Statutory" },
  { label: "Banking" },
  { label: "Master Data" },
];
