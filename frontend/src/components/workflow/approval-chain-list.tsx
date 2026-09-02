"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  activateChain,
  createChain,
  deactivateChain,
  listChains,
  type ChainView,
} from "@/lib/api/workflow-api";
import { ApprovalChainSteps } from "@/components/workflow/approval-chain-steps";

const createChainSchema = z.object({
  name: z.string().min(1, "Name is required"),
});

type CreateChainValues = z.infer<typeof createChainSchema>;

export interface ApprovalChainListProps {
  companyId: string;
  documentType: string;
}

/** F0.4.1 / WF-1: approval chain configuration for one document type in one company — create, activate/deactivate, and drill into a chain's steps. */
export function ApprovalChainList({ companyId, documentType }: ApprovalChainListProps) {
  const queryClient = useQueryClient();
  const queryKey = ["workflow", "chains", companyId, documentType];
  const [selectedChainId, setSelectedChainId] = useState<string | null>(null);

  const {
    data: chains,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: () => listChains(companyId, documentType),
  });

  const form = useForm<CreateChainValues>({
    resolver: zodResolver(createChainSchema),
    defaultValues: { name: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: CreateChainValues) => createChain(companyId, documentType, values.name),
    onSuccess: (chain) => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
      setSelectedChainId(chain.id);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({ chain }: { chain: ChainView }) =>
      chain.active ? deactivateChain(chain.id, companyId) : activateChain(chain.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Approval chains for {documentType || "…"}</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading chains…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load approval chains.
          </p>
        )}
        {chains && chains.length === 0 && (
          <p className="text-sm text-muted-foreground">No approval chain yet for this document type.</p>
        )}
        {chains && chains.length > 0 && (
          <ul className="flex flex-col gap-2">
            {chains.map((chain) => (
              <li
                key={chain.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <button
                  type="button"
                  className="text-left font-medium hover:underline"
                  onClick={() => setSelectedChainId(chain.id)}
                >
                  {chain.name}
                </button>
                <div className="flex items-center gap-2">
                  <Badge variant={chain.active ? "default" : "outline"}>
                    {chain.active ? "Active" : "Inactive"}
                  </Badge>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={toggleMutation.isPending}
                    onClick={() => toggleMutation.mutate({ chain })}
                  >
                    {chain.active ? "Deactivate" : "Activate"}
                  </Button>
                  <Button size="sm" variant="ghost" onClick={() => setSelectedChainId(chain.id)}>
                    Manage steps
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="name" label="New chain name" />
            </div>
            <Button type="submit" disabled={createMutation.isPending || !documentType}>
              {createMutation.isPending ? "Creating…" : "Create chain"}
            </Button>
          </form>
        </Form>
        {createMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create the chain."}
          </p>
        )}
      </section>

      {selectedChainId && (
        <section className="flex flex-col gap-3 rounded-lg border p-4">
          <h2 className="text-sm font-semibold">
            Steps for {chains?.find((chain) => chain.id === selectedChainId)?.name ?? "chain"}
          </h2>
          <ApprovalChainSteps chainId={selectedChainId} companyId={companyId} />
        </section>
      )}
    </div>
  );
}
