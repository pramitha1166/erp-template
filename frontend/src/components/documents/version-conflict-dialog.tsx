"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export interface VersionConflictDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Discards local edits and re-fetches the current row (new `@Version`). */
  onReload: () => void;
}

/**
 * F0.1.2 / ARCH-6: every aggregate root carries an optimistic-lock
 * `@Version`. A submit against a stale version fails on the server with a
 * conflict rather than silently overwriting someone else's change — this
 * is the shared UI for that failure. It never auto-retries the write,
 * since the local edits were made against data that's no longer current.
 */
export function VersionConflictDialog({
  open,
  onOpenChange,
  onReload,
}: VersionConflictDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>This document changed elsewhere</DialogTitle>
          <DialogDescription>
            Someone else saved a newer version of this document while you
            were working on it. Reload the latest version before trying
            again — your changes on this screen will be lost.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Keep editing
          </Button>
          <Button onClick={onReload}>Reload latest version</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
