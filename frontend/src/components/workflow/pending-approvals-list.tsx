"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/api/http";
import { myPendingTasks, type TaskView } from "@/lib/api/workflow-api";
import { ApprovalDecisionDialog } from "@/components/workflow/approval-decision-dialog";
import { TaskStatusBadge } from "@/components/workflow/task-status-badge";

export interface PendingApprovalsListProps {
  companyId: string;
  /** Caps rows shown and hides the table chrome, for the dashboard-widget placement (F0.4.3). */
  limit?: number;
  /** Renders a "View all" link back to the full inbox — only meaningful alongside `limit`. */
  showViewAllLink?: boolean;
}

/** F0.4.3: the pending-approval inbox — a dashboard widget and the full list view share this component, differing only by `limit`. */
export function PendingApprovalsList({ companyId, limit, showViewAllLink }: PendingApprovalsListProps) {
  const queryClient = useQueryClient();
  const queryKey = ["workflow", "tasks", "mine"];
  const [decision, setDecision] = useState<{ task: TaskView; action: "approve" | "reject" } | null>(null);

  const {
    data: tasks,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey,
    queryFn: myPendingTasks,
  });

  const actionable = (tasks ?? []).filter((task) => task.status === "PENDING" || task.status === "ESCALATED");
  const visible = limit ? actionable.slice(0, limit) : actionable;

  function refresh() {
    queryClient.invalidateQueries({ queryKey });
  }

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading pending approvals…</p>;
  }

  if (isError) {
    return (
      <p role="alert" className="text-sm text-destructive">
        {error instanceof ApiError ? error.message : "Could not load pending approvals."}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {visible.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nothing waiting on your approval.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Document type</TableHead>
              <TableHead>Document</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Due</TableHead>
              <TableHead className="text-right">Decision</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {visible.map((task) => (
              <TableRow key={task.id}>
                <TableCell>{task.documentType}</TableCell>
                <TableCell className="font-mono text-xs">{task.documentId}</TableCell>
                <TableCell>
                  <TaskStatusBadge status={task.status} />
                </TableCell>
                <TableCell className="text-xs text-muted-foreground">
                  {task.dueAt ? new Date(task.dueAt).toLocaleString() : "—"}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button size="sm" variant="outline" onClick={() => setDecision({ task, action: "reject" })}>
                      Reject
                    </Button>
                    <Button size="sm" onClick={() => setDecision({ task, action: "approve" })}>
                      Approve
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {showViewAllLink && actionable.length > 0 && (
        <Link href="/approvals" className="self-start text-sm text-primary underline-offset-4 hover:underline">
          View all ({actionable.length})
        </Link>
      )}

      {decision && (
        <ApprovalDecisionDialog
          task={decision.task}
          companyId={companyId}
          action={decision.action}
          open={true}
          onOpenChange={(open) => {
            if (!open) {
              setDecision(null);
            }
          }}
          onDecided={refresh}
        />
      )}
    </div>
  );
}
