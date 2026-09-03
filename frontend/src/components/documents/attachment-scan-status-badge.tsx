import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { SCAN_STATUS_LABELS, type ScanStatus } from "@/lib/documents/scan-status";

const STATUS_STYLES: Record<ScanStatus, string> = {
  PENDING: "bg-muted text-muted-foreground border-transparent",
  CLEAN: "bg-primary/10 text-primary border-primary/20",
  INFECTED: "bg-destructive/10 text-destructive border-destructive/20",
  FAILED: "bg-destructive/10 text-destructive border-destructive/20",
};

export interface AttachmentScanStatusBadgeProps {
  status: ScanStatus;
  className?: string;
}

/** F0.7.5 / DOC-4: the virus-scan outcome for one attachment, styled the same way `DocumentStatusBadge` is. */
export function AttachmentScanStatusBadge({ status, className }: AttachmentScanStatusBadgeProps) {
  return (
    <Badge variant="outline" className={cn(STATUS_STYLES[status], className)}>
      {SCAN_STATUS_LABELS[status]}
    </Badge>
  );
}
