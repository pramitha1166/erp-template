"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { FiscalYearPanel } from "@/components/masterdata/fiscal-year-panel";

/** F0.6.8: fiscal year / accounting period administration — close/reopen. */
export default function FiscalYearsAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Fiscal Years</h1>
        <p className="text-sm text-muted-foreground">
          Close or reopen the active company&apos;s fiscal years and accounting periods. A fiscal year can only be
          closed once every one of its periods is itself closed.
        </p>
      </div>

      <RequireCompany>{(companyId) => <FiscalYearPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
