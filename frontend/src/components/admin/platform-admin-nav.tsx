"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { cn } from "@/lib/utils";

const TABS = [
  { href: "/admin/platform/brands", label: "Brands" },
  { href: "/admin/platform/usage", label: "Usage & health" },
  { href: "/admin/platform/entitlements", label: "Default entitlements" },
];

/** F0.11.1 / ADM-1: tab strip shared by the three Platform Admin Console screens. */
export function PlatformAdminNav() {
  const pathname = usePathname();

  return (
    <nav aria-label="Platform admin" className="flex gap-1 border-b">
      {TABS.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={cn(
            "border-b-2 px-3 py-2 text-sm font-medium",
            pathname === tab.href
              ? "border-primary text-foreground"
              : "border-transparent text-muted-foreground hover:text-foreground",
          )}
        >
          {tab.label}
        </Link>
      ))}
    </nav>
  );
}
