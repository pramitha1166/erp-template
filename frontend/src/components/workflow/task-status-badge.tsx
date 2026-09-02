import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { TaskStatus } from "@/lib/api/workflow-api";

const STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  ESCALATED: "Escalated",
};

const STATUS_STYLES: Record<TaskStatus, string> = {
  PENDING: "bg-muted text-muted-foreground border-transparent",
  APPROVED: "bg-primary/10 text-primary border-primary/20",
  REJECTED: "bg-destructive/10 text-destructive border-destructive/20",
  CANCELLED: "bg-muted text-muted-foreground border-transparent",
  ESCALATED: "bg-destructive/10 text-destructive border-destructive/20",
};

export interface TaskStatusBadgeProps {
  status: TaskStatus;
  className?: string;
}

/** F0.4.5: the escalation status indicator — `ESCALATED` (WF-5's sweep already flipped the task) styled like a rejection to draw the eye. */
export function TaskStatusBadge({ status, className }: TaskStatusBadgeProps) {
  return (
    <Badge variant="outline" className={cn(STATUS_STYLES[status], className)}>
      {STATUS_LABELS[status]}
    </Badge>
  );
}
