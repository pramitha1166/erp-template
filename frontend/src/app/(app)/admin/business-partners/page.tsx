"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { BusinessPartnerPanel } from "@/components/masterdata/business-partner-panel";

/** F0.6.4: customer/supplier master list + detail form (contacts, credit terms, default account, bank details). */
export default function BusinessPartnersAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Customers &amp; Suppliers</h1>
        <p className="text-sm text-muted-foreground">
          Manage the active company&apos;s customer and supplier master records, their contacts, credit terms, and
          bank details.
        </p>
      </div>

      <RequireCompany>{(companyId) => <BusinessPartnerPanel companyId={companyId} />}</RequireCompany>
    </div>
  );
}
