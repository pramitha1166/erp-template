"use client";

import { useQuery } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { getApprovalHistory } from "@/lib/api/workflow-api";

export interface ApprovalHistoryTimelineProps {
  companyId: string;
  documentType: string;
  documentId: string;
}

const ACTION_LABELS: Record<string, string> = {
  SUBMITTED: "Submitted",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  ESCALATED: "Escalated",
  CANCELLED: "Cancelled",
};

function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action;
}

function actionVariant(action: string): "default" | "outline" | "destructive" | "secondary" {
  if (action === "REJECTED") {
    return "destructive";
  }
  if (action === "APPROVED") {
    return "default";
  }
  return "outline";
}

/**
 * F0.4.6 / WF-7: read-only approval-history timeline, meant to embed on a
 * document detail view once one exists — same placement note as
 * `DocumentHistoryPanel` (F0.3.1), whose groundwork this shares.
 */
export function ApprovalHistoryTimeline({ companyId, documentType, documentId }: ApprovalHistoryTimelineProps) {
  const {
    data: entries,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["workflow", "history", companyId, documentType, documentId],
    queryFn: () => getApprovalHistory(companyId, documentType, documentId),
  });

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading approval history…</p>;
  }

  if (isError) {
    return (
      <p role="alert" className="text-sm text-destructive">
        Could not load approval history.
      </p>
    );
  }

  if (!entries || entries.length === 0) {
    return <p className="text-sm text-muted-foreground">No approval activity yet.</p>;
  }

  return (
    <ol className="flex flex-col gap-3">
      {entries.map((entry, index) => (
        <li key={`${entry.instanceId}-${index}`} className="rounded-md border p-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <span className="flex items-center gap-2 text-sm font-medium">
              <Badge variant={actionVariant(entry.action)}>{actionLabel(entry.action)}</Badge>
              {entry.actorUserId ?? "System"}
            </span>
            <span className="text-xs text-muted-foreground">{new Date(entry.occurredAt).toLocaleString()}</span>
          </div>
          {entry.comment && <p className="mt-2 text-sm text-muted-foreground">{entry.comment}</p>}
        </li>
      ))}
    </ol>
  );
}
