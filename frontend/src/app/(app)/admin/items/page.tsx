"use client";

import { RequireCompany } from "@/components/admin/require-company";
import { ItemGroupPanel } from "@/components/masterdata/item-group-panel";
import { ItemPanel } from "@/components/masterdata/item-panel";
import { Separator } from "@/components/ui/separator";

/** F0.6.5: item master list + detail form, with item groups as the prerequisite classification data. */
export default function ItemsAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Items</h1>
        <p className="text-sm text-muted-foreground">
          Manage the active company&apos;s item groups and item master records.
        </p>
      </div>

      <RequireCompany>
        {(companyId) => (
          <div className="flex flex-col gap-8">
            <ItemGroupPanel companyId={companyId} />
            <Separator />
            <ItemPanel companyId={companyId} />
          </div>
        )}
      </RequireCompany>
    </div>
  );
}
