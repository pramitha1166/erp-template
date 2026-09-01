"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { NumberingSeriesPanel } from "@/components/numbering/numbering-series-panel";

/** F0.5.1: naming-series configuration screen, per document type per company. */
export default function NumberingSeriesAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Naming series</h1>
        <p className="text-sm text-muted-foreground">
          Configure the prefix, date parts, counter width, and fiscal-year reset behaviour for each document
          type&apos;s numbering.
        </p>
      </div>

      <RequireCompany>{(companyId) => <NumberingSeriesPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
