"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/http";
import {
  createDataSubjectRequest,
  listDataSubjectRequests,
  type DataRequestStatus,
  type DataSubjectRequestView,
} from "@/lib/api/admin-api";

interface DataRequestsPageProps {
  params: Promise<{ brandId: string; tenantId: string }>;
}

const STATUS_VARIANT: Record<DataRequestStatus, "outline" | "secondary" | "destructive"> = {
  PENDING: "secondary",
  COMPLETED: "outline",
  FAILED: "destructive",
};

function formatResult(request: DataSubjectRequestView): string {
  if (!request.resultPayload) {
    return "—";
  }
  try {
    return JSON.stringify(JSON.parse(request.resultPayload), null, 2);
  } catch {
    return request.resultPayload;
  }
}

/** F0.11.6 / ADM-8: tenant-initiated data export/erasure requests, actioned and tracked from the admin console. */
function DataRequests({ brandId, tenantId }: { brandId: string; tenantId: string }) {
  const queryClient = useQueryClient();
  const queryKey = ["data-requests", tenantId];
  const [notes, setNotes] = useState("");
  const [confirmErasure, setConfirmErasure] = useState(false);

  const { data: requests, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () => listDataSubjectRequests(tenantId),
  });

  const exportMutation = useMutation({
    mutationFn: () => createDataSubjectRequest(tenantId, "EXPORT", notes || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setNotes("");
    },
  });

  const erasureMutation = useMutation({
    mutationFn: () => createDataSubjectRequest(tenantId, "ERASURE", notes || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setNotes("");
      setConfirmErasure(false);
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link
          href={`/admin/brands/${brandId}/tenants/${tenantId}`}
          className="text-xs text-muted-foreground hover:underline"
        >
          ← Tenant
        </Link>
        <h1 className="text-xl font-semibold">Data requests</h1>
        <p className="text-sm text-muted-foreground">
          Requests are actioned immediately and reuse the full data export capability (NFR-D5). Erasure disables
          the tenant&apos;s admin user and company and suspends the tenant — it does not delete audit history.
        </p>
      </div>

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="data-request-notes">Notes (optional)</Label>
          <Input id="data-request-notes" value={notes} onChange={(event) => setNotes(event.target.value)} />
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" disabled={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
            {exportMutation.isPending ? "Requesting…" : "Request export"}
          </Button>
          <Dialog open={confirmErasure} onOpenChange={setConfirmErasure}>
            <Button type="button" variant="destructive" onClick={() => setConfirmErasure(true)}>
              Request erasure
            </Button>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Erase this tenant&apos;s data?</DialogTitle>
                <DialogDescription>
                  This disables the tenant&apos;s admin user and company and suspends the tenant. It cannot be
                  undone from this screen.
                </DialogDescription>
              </DialogHeader>
              {erasureMutation.isError && (
                <p role="alert" className="text-sm text-destructive">
                  {erasureMutation.error instanceof ApiError
                    ? erasureMutation.error.message
                    : "Could not process the erasure request."}
                </p>
              )}
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setConfirmErasure(false)}>
                  Cancel
                </Button>
                <Button
                  type="button"
                  variant="destructive"
                  disabled={erasureMutation.isPending}
                  onClick={() => erasureMutation.mutate()}
                >
                  {erasureMutation.isPending ? "Erasing…" : "Confirm erasure"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
        {exportMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {exportMutation.error instanceof ApiError ? exportMutation.error.message : "Could not process the export request."}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Request history</h2>
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            {error instanceof ApiError ? error.message : "Could not load data requests."}
          </p>
        )}
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {requests && requests.length === 0 && (
          <p className="text-sm text-muted-foreground">No data requests yet.</p>
        )}
        {requests && requests.length > 0 && (
          <ul className="flex flex-col gap-2">
            {requests.map((request) => (
              <li key={request.id} className="flex flex-col gap-2 rounded-md border p-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-medium">{request.type}</span>
                  <Badge variant={STATUS_VARIANT[request.status]}>{request.status}</Badge>
                </div>
                <pre className="overflow-x-auto rounded-md bg-muted p-2 text-xs">{formatResult(request)}</pre>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

export default function DataRequestsPage({ params }: DataRequestsPageProps) {
  const { brandId, tenantId } = use(params);
  return <DataRequests brandId={brandId} tenantId={tenantId} />;
}
