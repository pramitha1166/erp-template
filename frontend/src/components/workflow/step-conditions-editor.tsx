"use client";

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
  CONDITION_OPERATOR_LABELS,
  addCondition,
  listConditions,
  type ConditionOperator,
} from "@/lib/api/workflow-api";

const OPERATOR_OPTIONS = (Object.keys(CONDITION_OPERATOR_LABELS) as ConditionOperator[]).map((operator) => ({
  value: operator,
  label: CONDITION_OPERATOR_LABELS[operator],
}));

const conditionSchema = z
  .object({
    fieldName: z.string().min(1, "Field name is required"),
    operator: z.enum(["EQ", "NE", "GT", "GTE", "LT", "LTE"]),
    valueType: z.enum(["text", "number"]),
    valueString: z.string().optional(),
    valueNumber: z.string().optional(),
  })
  .refine(
    (values) =>
      values.valueType === "text"
        ? !!values.valueString && values.valueString.trim().length > 0
        : !!values.valueNumber && values.valueNumber.trim().length > 0,
    { message: "A value is required", path: ["valueString"] },
  );

type ConditionValues = z.infer<typeof conditionSchema>;

export interface StepConditionsEditorProps {
  chainId: string;
  stepId: string;
  companyId: string;
}

/** F0.4.2 / WF-2: the condition builder for one approval step's field-based rules (e.g. amount thresholds). */
export function StepConditionsEditor({ chainId, stepId, companyId }: StepConditionsEditorProps) {
  const queryClient = useQueryClient();
  const queryKey = ["workflow", "chains", chainId, "steps", stepId, "conditions"];

  const { data: conditions, isLoading } = useQuery({
    queryKey,
    queryFn: () => listConditions(stepId, companyId),
  });

  const form = useForm<ConditionValues>({
    resolver: zodResolver(conditionSchema),
    defaultValues: { fieldName: "", operator: "EQ", valueType: "number", valueString: "", valueNumber: "" },
  });

  const valueType = form.watch("valueType");

  const createMutation = useMutation({
    mutationFn: (values: ConditionValues) =>
      addCondition(chainId, stepId, companyId, {
        fieldName: values.fieldName,
        operator: values.operator,
        valueString: values.valueType === "text" ? values.valueString : undefined,
        valueNumber: values.valueType === "number" ? values.valueNumber : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset({ fieldName: "", operator: "EQ", valueType: "number", valueString: "", valueNumber: "" });
    },
  });

  return (
    <div className="flex flex-col gap-3 rounded-md border border-dashed p-3">
      <h4 className="text-xs font-semibold text-muted-foreground uppercase">Conditions</h4>

      {isLoading && <p className="text-xs text-muted-foreground">Loading conditions…</p>}
      {conditions && conditions.length > 0 && (
        <ul className="flex flex-col gap-1">
          {conditions.map((condition) => (
            <li key={condition.id} className="text-sm">
              <code className="text-xs">{condition.fieldName}</code>{" "}
              {CONDITION_OPERATOR_LABELS[condition.operator]}{" "}
              <span className="font-medium">{condition.valueNumber ?? condition.valueString}</span>
            </li>
          ))}
        </ul>
      )}
      {conditions && conditions.length === 0 && (
        <p className="text-xs text-muted-foreground">
          No conditions — this step always applies when the chain runs.
        </p>
      )}

      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
          className="flex flex-col gap-2 sm:flex-row sm:items-end sm:flex-wrap"
        >
          <div className="w-full sm:w-40">
            <ValidatedTextField control={form.control} name="fieldName" label="Field" placeholder="e.g. amount" />
          </div>
          <div className="w-full sm:w-48">
            <ValidatedSelectField control={form.control} name="operator" label="Operator" options={OPERATOR_OPTIONS} />
          </div>
          <div className="w-full sm:w-32">
            <ValidatedSelectField
              control={form.control}
              name="valueType"
              label="Value type"
              options={[
                { value: "number", label: "Number" },
                { value: "text", label: "Text" },
              ]}
            />
          </div>
          <div className="w-full sm:w-40">
            {valueType === "number" ? (
              <ValidatedTextField control={form.control} name="valueNumber" label="Value" type="number" />
            ) : (
              <ValidatedTextField control={form.control} name="valueString" label="Value" />
            )}
          </div>
          <Button type="submit" size="sm" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Adding…" : "Add condition"}
          </Button>
        </form>
      </Form>
      {createMutation.isError && (
        <p role="alert" className="text-xs text-destructive">
          {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not add the condition."}
        </p>
      )}
    </div>
  );
}
