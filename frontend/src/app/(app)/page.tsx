"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { PendingApprovalsList } from "@/components/workflow/pending-approvals-list";

export default function DashboardPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Frontend scaffold — Epic F0.0. Module screens land as their epics do.
        </p>
      </div>

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Pending approvals</h2>
        <RequireCompany>
          {(companyId) => <PendingApprovalsList companyId={companyId} limit={5} showViewAllLink />}
        </RequireCompany>
      </section>
    </div>
  );
}
