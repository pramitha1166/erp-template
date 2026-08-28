"use client";

import { SetupChecklist } from "@/components/admin/setup-checklist";
import { getCurrentTenantId } from "@/lib/auth/session";

/** F0.11.3 / ADM-4: entry point for the tenant's own post-onboarding checklist. */
export default function SetupChecklistPage() {
  const tenantId = getCurrentTenantId();

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Setup checklist</h1>
        <p className="text-sm text-muted-foreground">
          A few steps to get from onboarding to your first invoice.
        </p>
      </div>
      {tenantId ? (
        <SetupChecklist tenantId={tenantId} />
      ) : (
        <p className="text-sm text-muted-foreground">Sign in to see your setup checklist.</p>
      )}
    </div>
  );
}
