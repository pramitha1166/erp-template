"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { createInvite, listInvites, revokeInvite, type InviteStatus } from "@/lib/api/admin-api";

const inviteSchema = z.object({ email: z.string().email("Enter a valid email") });
type InviteValues = z.infer<typeof inviteSchema>;

const STATUS_VARIANT: Record<InviteStatus, "outline" | "secondary" | "destructive"> = {
  PENDING: "outline",
  ACCEPTED: "secondary",
  EXPIRED: "destructive",
  REVOKED: "destructive",
};

export interface TenantInvitesPanelProps {
  tenantId: string;
}

/** F0.11.4 / ADM-5: invite a tenant-admin user by email; a 7-day link they accept at `/invite-accept`. */
export function TenantInvitesPanel({ tenantId }: TenantInvitesPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["tenant-invites", tenantId];

  const { data: invites, isLoading } = useQuery({ queryKey, queryFn: () => listInvites(tenantId) });

  const form = useForm<InviteValues>({ resolver: zodResolver(inviteSchema), defaultValues: { email: "" } });

  const inviteMutation = useMutation({
    mutationFn: (values: InviteValues) => createInvite(tenantId, values.email),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (inviteId: string) => revokeInvite(tenantId, inviteId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Invite a tenant admin</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => inviteMutation.mutate(values))}
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="email" label="Email" type="email" />
          </div>
          <Button type="submit" disabled={inviteMutation.isPending}>
            {inviteMutation.isPending ? "Sending…" : "Send invite"}
          </Button>
        </form>
      </Form>
      {inviteMutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {inviteMutation.error instanceof ApiError ? inviteMutation.error.message : "Could not send that invite."}
        </p>
      )}

      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {invites && invites.length === 0 && <p className="text-sm text-muted-foreground">No invites sent yet.</p>}
      {invites && invites.length > 0 && (
        <ul className="flex flex-col gap-2">
          {invites.map((invite) => (
            <li key={invite.id} className="flex items-center justify-between rounded-md border px-3 py-2 text-sm">
              <span>{invite.email}</span>
              <div className="flex items-center gap-2">
                <Badge variant={STATUS_VARIANT[invite.status]}>{invite.status}</Badge>
                {invite.status === "PENDING" && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={revokeMutation.isPending}
                    onClick={() => revokeMutation.mutate(invite.id)}
                  >
                    Revoke
                  </Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
