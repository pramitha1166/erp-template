"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { CurrencyPanel } from "@/components/masterdata/currency-panel";

/** F0.6.7: currency management + exchange-rate entry/history view. */
export default function CurrenciesAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Currencies</h1>
        <p className="text-sm text-muted-foreground">
          Currencies are shared across every company in your tenant. Record date-effective exchange rates by hand
          here — the optional CBSL import job records its own rates the same way, tagged as CBSL.
        </p>
      </div>

      <RequireCompany>{(companyId) => <CurrencyPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
