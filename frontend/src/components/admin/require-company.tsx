"use client";

import { useTenantStore } from "@/stores/tenant-store";

/**
 * Every IAM admin write is scoped to a `companyId` query param
 * (`RoleController`, `UserRoleController`, `SodRuleController`) — there is
 * no tenant-wide equivalent. Rather than repeat this guard on every admin
 * screen, they render through this and get the active company id handed
 * to them once it exists.
 */
export function RequireCompany({
  children,
}: {
  children: (companyId: string) => React.ReactNode;
}) {
  const activeCompany = useTenantStore((state) => state.activeCompany);

  if (!activeCompany) {
    return (
      <p className="text-sm text-muted-foreground">
        Select a company from the switcher in the header before managing roles and permissions.
      </p>
    );
  }

  return <>{children(activeCompany.id)}</>;
}
