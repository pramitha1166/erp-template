"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { ApiError } from "@/lib/api/http";
import { startImpersonation } from "@/lib/auth/impersonation";

export interface ImpersonateTenantButtonProps {
  tenantId: string;
  tenantName: string;
}

/**
 * F0.11.5 / ADM-7: "log in as tenant admin" entry point. A reason is
 * mandatory — it's carried into the audit trail server-side
 * (`ImpersonationStarted`) and the tenant is notified when it happens.
 */
export function ImpersonateTenantButton({ tenantId, tenantName }: ImpersonateTenantButtonProps) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleStart() {
    if (!reason.trim()) {
      setError("A reason is required.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await startImpersonation(tenantId, tenantName, reason.trim());
      setOpen(false);
      router.push("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not start the impersonation session.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm">
          Log in as tenant admin
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Impersonate {tenantName}</DialogTitle>
          <DialogDescription>
            Starts a 30-minute, audited session as this tenant&apos;s administrator. The tenant is notified.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="impersonation-reason">Reason</Label>
          <Input
            id="impersonation-reason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Support ticket #1234"
          />
        </div>
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}
        <DialogFooter>
          <Button type="button" disabled={submitting} onClick={handleStart}>
            {submitting ? "Starting…" : "Start impersonation"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
