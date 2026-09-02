"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { CostCentrePanel } from "@/components/masterdata/cost-centre-panel";

/** F0.6.3: hierarchical cost centre tree management. */
export default function CostCentresAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Cost Centres</h1>
        <p className="text-sm text-muted-foreground">Manage the active company&apos;s cost/profit-centre hierarchy.</p>
      </div>

      <RequireCompany>{(companyId) => <CostCentrePanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
