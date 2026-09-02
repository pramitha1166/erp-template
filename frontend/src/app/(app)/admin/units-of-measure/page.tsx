"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { UomPanel } from "@/components/masterdata/uom-panel";

/** F0.6.6: unit-of-measure management + conversion-factor editor. */
export default function UnitsOfMeasureAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Units of Measure</h1>
        <p className="text-sm text-muted-foreground">
          Units of measure are shared across every company in your tenant. Configure conversion factors so a
          purchase UOM (e.g. box) can differ from an item&apos;s stock UOM (e.g. each).
        </p>
      </div>

      <RequireCompany>{(companyId) => <UomPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
