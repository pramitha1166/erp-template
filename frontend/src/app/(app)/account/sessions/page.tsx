"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listSessions, revokeSession, type SessionView } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

function formatTimestamp(value: string): string {
  return new Date(value).toLocaleString();
}

/**
 * F0.2.4 / IAM-8: the caller's own active sessions, with force-logout per
 * session. `AuthController.revokeSession` only lets a user revoke a
 * session they own — admin-initiated force-logout of *other* users is
 * explicitly out of scope for this endpoint (see its Javadoc), so there's
 * no "revoke someone else's session" control here either.
 */
export default function SessionsPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const { data: sessions, isLoading } = useQuery({
    queryKey: ["sessions"],
    queryFn: listSessions,
  });

  const revokeMutation = useMutation({
    mutationFn: (sessionId: string) => revokeSession(sessionId),
    onSuccess: () => {
      setError(null);
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
    },
    onError: (mutationError: unknown) => {
      setError(mutationError instanceof ApiError ? mutationError.message : "Could not sign out that session.");
    },
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Active sessions</h1>
        <p className="text-sm text-muted-foreground">
          Every device currently signed in to your account. Sign out any session you don&apos;t recognize.
        </p>
      </div>

      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}

      {isLoading && (
        <div role="status" aria-label="Loading" className="flex justify-center py-8">
          <div className="size-6 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
        </div>
      )}

      {sessions && sessions.length === 0 && (
        <p className="text-sm text-muted-foreground">No active sessions.</p>
      )}

      {sessions && sessions.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Signed in</TableHead>
              <TableHead>Last active</TableHead>
              <TableHead>IP address</TableHead>
              <TableHead>Device</TableHead>
              <TableHead className="text-right">
                <span className="sr-only">Actions</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sessions.map((session: SessionView) => (
              <TableRow key={session.id}>
                <TableCell>{formatTimestamp(session.issuedAt)}</TableCell>
                <TableCell>{formatTimestamp(session.lastSeenAt)}</TableCell>
                <TableCell>{session.ipAddress ?? "—"}</TableCell>
                <TableCell className="max-w-64 truncate" title={session.userAgent ?? undefined}>
                  {session.userAgent ?? "—"}
                </TableCell>
                <TableCell className="text-right">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={revokeMutation.isPending && revokeMutation.variables === session.id}
                    onClick={() => revokeMutation.mutate(session.id)}
                  >
                    Sign out
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
