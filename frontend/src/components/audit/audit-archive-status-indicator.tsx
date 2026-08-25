"use client";

import { useQuery } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { getAuditArchiveStatus } from "@/lib/api/audit-api";

export interface AuditArchiveStatusIndicatorProps {
  companyId: string;
}

/** F0.3.3 / AUD-5: admin-only readout of the audit trail's retention/archival state, for the audit log browser. */
export function AuditArchiveStatusIndicator({ companyId }: AuditArchiveStatusIndicatorProps) {
  const { data: status, isLoading } = useQuery({
    queryKey: ["audit-archive-status", companyId],
    queryFn: () => getAuditArchiveStatus(companyId),
  });

  if (isLoading || !status) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border bg-muted/30 p-3 text-sm">
      <Badge variant={status.archivalEnabled ? "secondary" : "outline"}>
        {status.archivalEnabled ? "Archival enabled" : "Archival not configured"}
      </Badge>
      <span className="text-muted-foreground">
        Cold storage after {status.coldStorageAfterYears} {status.coldStorageAfterYears === 1 ? "year" : "years"} · minimum
        retention {status.minimumRetentionYears} years
      </span>
      <span className="text-muted-foreground">
        Archived through:{" "}
        {status.archivedThrough ? new Date(status.archivedThrough).toLocaleString() : "not yet archived"}
      </span>
    </div>
  );
}
