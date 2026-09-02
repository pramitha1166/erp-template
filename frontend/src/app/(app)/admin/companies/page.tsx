"use client";

import { CompanyPanel } from "@/components/masterdata/company-panel";

/** F0.6.1: company management — list/switch companies within the tenant, create another, edit legal name/address/logo. */
export default function CompaniesAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Companies</h1>
        <p className="text-sm text-muted-foreground">
          Manage the companies in your tenant. A tenant may hold several companies (MDM-2) — switch the active one
          here or from the header.
        </p>
      </div>

      <CompanyPanel />
    </div>
  );
}
