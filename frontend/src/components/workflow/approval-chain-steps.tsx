"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  addStep,
  configureEscalation,
  listSteps,
  type ApproverType,
  type StepView,
} from "@/lib/api/workflow-api";
import { StepConditionsEditor } from "@/components/workflow/step-conditions-editor";

const APPROVER_TYPE_OPTIONS: { value: ApproverType; label: string }[] = [
  { value: "ROLE", label: "Anyone holding a role" },
  { value: "USER", label: "A specific user" },
  { value: "HIERARCHY", label: "Submitter's manager chain" },
];

const stepSchema = z
  .object({
    sequenceOrder: z
      .string()
      .regex(/^\d+$/, "Sequence must be a whole number")
      .refine((value) => Number(value) >= 1, "Sequence must be at least 1"),
    name: z.string().min(1, "Name is required"),
    approverType: z.enum(["ROLE", "USER", "HIERARCHY"]),
    roleId: z.string().optional(),
    userId: z.string().optional(),
    hierarchyLevel: z.string().optional(),
  })
  .refine((values) => values.approverType !== "ROLE" || !!values.roleId, {
    message: "Role id is required for this approver type",
    path: ["roleId"],
  })
  .refine((values) => values.approverType !== "USER" || !!values.userId, {
    message: "User id is required for this approver type",
    path: ["userId"],
  })
  .refine((values) => values.approverType !== "HIERARCHY" || !!values.hierarchyLevel, {
    message: "Hierarchy level is required for this approver type",
    path: ["hierarchyLevel"],
  });

type StepValues = z.infer<typeof stepSchema>;

const escalationSchema = z.object({
  hours: z
    .string()
    .regex(/^\d+$/, "Escalation hours must be a whole number")
    .refine((value) => Number(value) >= 1, "Escalation hours must be at least 1"),
});

type EscalationValues = z.infer<typeof escalationSchema>;

function approverSummary(step: StepView): string {
  switch (step.approverType) {
    case "ROLE":
      return `Role ${step.approverRoleId}`;
    case "USER":
      return `User ${step.approverUserId}`;
    case "HIERARCHY":
      return `Manager chain, level ${step.hierarchyLevel}`;
  }
}

function StepEscalationForm({ chainId, stepId, companyId }: { chainId: string; stepId: string; companyId: string }) {
  const queryClient = useQueryClient();
  const form = useForm<EscalationValues>({
    resolver: zodResolver(escalationSchema),
    defaultValues: { hours: "24" },
  });

  const mutation = useMutation({
    mutationFn: (values: EscalationValues) =>
      configureEscalation(chainId, stepId, companyId, { hours: Number(values.hours) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["workflow", "chains", chainId, "steps", companyId] }),
  });

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        className="flex items-end gap-2"
      >
        <div className="w-28">
          <ValidatedTextField control={form.control} name="hours" label="Escalate after (hours)" type="number" />
        </div>
        <Button type="submit" size="sm" variant="outline" disabled={mutation.isPending}>
          {mutation.isPending ? "Saving…" : "Set escalation"}
        </Button>
        {mutation.isSuccess && <span className="text-xs text-muted-foreground">Saved</span>}
      </form>
    </Form>
  );
}

export interface ApprovalChainStepsProps {
  chainId: string;
  companyId: string;
}

/** F0.4.1: manages one approval chain's ordered steps, each step's escalation, and (via `StepConditionsEditor`) its conditions. */
export function ApprovalChainSteps({ chainId, companyId }: ApprovalChainStepsProps) {
  const queryClient = useQueryClient();
  const queryKey = ["workflow", "chains", chainId, "steps", companyId];
  const [expandedStepId, setExpandedStepId] = useState<string | null>(null);

  const { data: steps, isLoading } = useQuery({
    queryKey,
    queryFn: () => listSteps(chainId, companyId),
  });

  const form = useForm<StepValues>({
    resolver: zodResolver(stepSchema),
    defaultValues: {
      sequenceOrder: String((steps?.length ?? 0) + 1),
      name: "",
      approverType: "ROLE",
      roleId: "",
      userId: "",
      hierarchyLevel: "",
    },
  });

  const approverType = form.watch("approverType");

  const createMutation = useMutation({
    mutationFn: (values: StepValues) =>
      addStep(chainId, companyId, {
        sequenceOrder: Number(values.sequenceOrder),
        name: values.name,
        approverType: values.approverType,
        roleId: values.approverType === "ROLE" ? values.roleId : undefined,
        userId: values.approverType === "USER" ? values.userId : undefined,
        hierarchyLevel: values.approverType === "HIERARCHY" ? Number(values.hierarchyLevel) : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset({
        sequenceOrder: String((steps?.length ?? 0) + 2),
        name: "",
        approverType: "ROLE",
        roleId: "",
        userId: "",
        hierarchyLevel: "",
      });
    },
  });

  return (
    <div className="flex flex-col gap-4">
      {isLoading && <p className="text-sm text-muted-foreground">Loading steps…</p>}

      {steps && steps.length > 0 && (
        <ol className="flex flex-col gap-2">
          {[...steps]
            .sort((a, b) => a.sequenceOrder - b.sequenceOrder)
            .map((step) => (
              <li key={step.id} className="rounded-md border p-3">
                <button
                  type="button"
                  className="flex w-full items-center justify-between gap-2 text-left"
                  onClick={() => setExpandedStepId((current) => (current === step.id ? null : step.id))}
                >
                  <span className="text-sm">
                    <span className="font-medium">
                      {step.sequenceOrder}. {step.name}
                    </span>{" "}
                    <span className="text-muted-foreground">— {approverSummary(step)}</span>
                    {step.escalationHours != null && (
                      <span className="ml-2 text-xs text-muted-foreground">
                        (escalates after {step.escalationHours}h)
                      </span>
                    )}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {expandedStepId === step.id ? "Hide" : "Manage"}
                  </span>
                </button>
                {expandedStepId === step.id && (
                  <div className="mt-3 flex flex-col gap-3">
                    <StepEscalationForm chainId={chainId} stepId={step.id} companyId={companyId} />
                    <StepConditionsEditor chainId={chainId} stepId={step.id} companyId={companyId} />
                  </div>
                )}
              </li>
            ))}
        </ol>
      )}
      {steps && steps.length === 0 && (
        <p className="text-sm text-muted-foreground">No steps yet — add the first approver below.</p>
      )}

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">Add a step</h3>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3"
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <ValidatedTextField control={form.control} name="sequenceOrder" label="Sequence" type="number" />
              <div className="sm:col-span-2">
                <ValidatedTextField control={form.control} name="name" label="Step name" />
              </div>
            </div>
            <ValidatedSelectField
              control={form.control}
              name="approverType"
              label="Approver"
              options={APPROVER_TYPE_OPTIONS}
            />
            {approverType === "ROLE" && (
              <ValidatedTextField control={form.control} name="roleId" label="Role id" />
            )}
            {approverType === "USER" && (
              <ValidatedTextField control={form.control} name="userId" label="User id" />
            )}
            {approverType === "HIERARCHY" && (
              <ValidatedTextField control={form.control} name="hierarchyLevel" label="Hierarchy level" type="number" />
            )}
            {createMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not add the step."}
              </p>
            )}
            <Button type="submit" disabled={createMutation.isPending} className="self-start">
              {createMutation.isPending ? "Adding…" : "Add step"}
            </Button>
          </form>
        </Form>
      </section>
    </div>
  );
}
