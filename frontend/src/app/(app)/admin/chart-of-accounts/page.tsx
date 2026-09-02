"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { ChartOfAccountsPanel } from "@/components/masterdata/chart-of-accounts-panel";

/** F0.6.2: Chart of Accounts tree view/editor. */
export default function ChartOfAccountsAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Chart of Accounts</h1>
        <p className="text-sm text-muted-foreground">
          Manage the active company&apos;s hierarchical Chart of Accounts — group accounts organise the tree, ledger
          accounts are where postings land.
        </p>
      </div>

      <RequireCompany>{(companyId) => <ChartOfAccountsPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
