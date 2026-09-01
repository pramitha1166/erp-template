"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { PendingApprovalsList } from "@/components/workflow/pending-approvals-list";

/** F0.4.3 / WF-8: the full pending-approval inbox — the dashboard widget links here for anything beyond its cap. */
export default function ApprovalsPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Pending approvals</h1>
        <p className="text-sm text-muted-foreground">Tasks assigned to you, across every document type.</p>
      </div>

      <RequireCompany>{(companyId) => <PendingApprovalsList companyId={companyId} />}</RequireCompany>
    </div>
  );
}
