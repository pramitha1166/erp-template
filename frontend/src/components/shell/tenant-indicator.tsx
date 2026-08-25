"use client";

import { Building2 } from "lucide-react";

import { useTenantStore } from "@/stores/tenant-store";

/**
 * F0.1.5 / ARCH-2: keeps the active tenant/company scope visible at all
 * times so an operator can never lose track of which company's data
 * they're acting against.
 */
export function TenantIndicator() {
  const activeCompany = useTenantStore((state) => state.activeCompany);

  if (!activeCompany) {
    return (
      <span className="flex items-center gap-1.5">
        <Building2 className="size-4" aria-hidden="true" />
        No company selected
      </span>
    );
  }

  return (
    <span
      className="flex items-center gap-1.5 font-medium text-foreground"
      title={activeCompany.name}
    >
      <Building2 className="size-4" aria-hidden="true" />
      {activeCompany.name}
    </span>
  );
}
