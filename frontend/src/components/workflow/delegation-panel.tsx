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
import { createDelegation, myDelegations, revokeDelegation } from "@/lib/api/workflow-api";

const delegationSchema = z
  .object({
    delegateUserId: z.uuid("Enter the delegate's user id"),
    startDate: z.string().min(1, "Start date is required"),
    endDate: z.string().min(1, "End date is required"),
    reason: z.string().optional(),
  })
  .refine((values) => values.endDate >= values.startDate, {
    message: "End date must be on or after the start date",
    path: ["endDate"],
  });

type DelegationValues = z.infer<typeof delegationSchema>;

/** F0.4.5 / WF-5: self-service delegation of a user's own approval authority for a date range. */
export function DelegationPanel() {
  const queryClient = useQueryClient();
  const queryKey = ["workflow", "delegations", "mine"];

  const { data: delegations, isLoading, isError } = useQuery({
    queryKey,
    queryFn: myDelegations,
  });

  const form = useForm<DelegationValues>({
    resolver: zodResolver(delegationSchema),
    defaultValues: { delegateUserId: "", startDate: "", endDate: "", reason: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: DelegationValues) =>
      createDelegation(values.delegateUserId, values.startDate, values.endDate, values.reason || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (delegationId: string) => revokeDelegation(delegationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const active = (delegations ?? []).filter((delegation) => !delegation.revoked);

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Delegate your approval authority</h2>
        <p className="text-sm text-muted-foreground">
          While active, the delegate can act on approval tasks assigned to you.
        </p>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3"
          >
            <ValidatedTextField control={form.control} name="delegateUserId" label="Delegate user id" />
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <ValidatedTextField control={form.control} name="startDate" label="Start date" type="date" />
              <ValidatedTextField control={form.control} name="endDate" label="End date" type="date" />
            </div>
            <ValidatedTextField control={form.control} name="reason" label="Reason (optional)" />
            {createMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create the delegation."}
              </p>
            )}
            <Button type="submit" disabled={createMutation.isPending} className="self-start">
              {createMutation.isPending ? "Delegating…" : "Delegate"}
            </Button>
          </form>
        </Form>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Your delegations</h2>
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load your delegations.
          </p>
        )}
        {!isLoading && active.length === 0 && (
          <p className="text-sm text-muted-foreground">No active delegations.</p>
        )}
        {active.length > 0 && (
          <ul className="flex flex-col gap-2">
            {active.map((delegation) => (
              <li
                key={delegation.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <span>
                  <span className="font-mono text-xs">{delegation.delegateUserId}</span>{" "}
                  <span className="text-muted-foreground">
                    {delegation.startDate} – {delegation.endDate}
                  </span>
                  {delegation.reason && <span className="ml-2 text-muted-foreground">“{delegation.reason}”</span>}
                </span>
                <div className="flex items-center gap-2">
                  <Badge variant="outline">Active</Badge>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={revokeMutation.isPending}
                    onClick={() => revokeMutation.mutate(delegation.id)}
                  >
                    Revoke
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
