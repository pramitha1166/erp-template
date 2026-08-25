import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { DOC_STATUS_LABELS, type DocStatus } from "@/lib/documents/status";

const STATUS_STYLES: Record<DocStatus, string> = {
  DRAFT: "bg-muted text-muted-foreground border-transparent",
  SUBMITTED: "bg-primary/10 text-primary border-primary/20",
  CANCELLED: "bg-destructive/10 text-destructive border-destructive/20",
  AMENDED: "bg-accent text-accent-foreground border-transparent",
};

export interface DocumentStatusBadgeProps {
  status: DocStatus;
  className?: string;
}

/** F0.1.1 / ARCH-4: the one place a document's lifecycle state is styled. */
export function DocumentStatusBadge({
  status,
  className,
}: DocumentStatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn(STATUS_STYLES[status], className)}
    >
      {DOC_STATUS_LABELS[status]}
    </Badge>
  );
}
