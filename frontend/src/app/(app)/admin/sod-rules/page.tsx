"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createSodRule, deleteSodRule, listSodRules } from "@/lib/api/iam-api";
import { ApiError } from "@/lib/api/http";
import { RequireCompany } from "@/components/admin/require-company";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const ruleSchema = z
  .object({
    permissionCodeA: z.string().min(1, "Required"),
    permissionCodeB: z.string().min(1, "Required"),
    description: z.string().optional(),
  })
  .refine((values) => values.permissionCodeA !== values.permissionCodeB, {
    path: ["permissionCodeB"],
    message: "A rule can't pair a permission with itself",
  });

type RuleValues = z.infer<typeof ruleSchema>;

function SodRuleList({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();

  const { data: rules, isLoading } = useQuery({
    queryKey: ["sod-rules"],
    queryFn: listSodRules,
  });

  const form = useForm<RuleValues>({
    resolver: zodResolver(ruleSchema),
    defaultValues: { permissionCodeA: "", permissionCodeB: "", description: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: RuleValues) =>
      createSodRule(companyId, values.permissionCodeA, values.permissionCodeB, values.description || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sod-rules"] });
      form.reset();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: string) => deleteSodRule(ruleId, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sod-rules"] }),
  });

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Add a conflicting-permission rule</h2>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <ValidatedTextField
                control={form.control}
                name="permissionCodeA"
                label="Permission A"
                placeholder="procurement:supplier:create"
              />
            </div>
            <div className="flex-1">
              <ValidatedTextField
                control={form.control}
                name="permissionCodeB"
                label="Permission B"
                placeholder="finance:payment:approve"
              />
            </div>
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="description" label="Description" />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Adding…" : "Add rule"}
            </Button>
          </form>
        </Form>
        {createMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not add that rule."}
          </p>
        )}
      </section>

      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {rules && rules.length === 0 && <p className="text-sm text-muted-foreground">No SoD rules configured.</p>}
      {rules && rules.length > 0 && (
        <ul className="flex flex-col gap-2">
          {rules.map((rule) => (
            <li key={rule.id} className="flex items-center justify-between gap-3 rounded-md border px-3 py-2 text-sm">
              <div className="flex flex-col gap-1">
                <span className="flex flex-wrap items-center gap-2">
                  <code className="text-xs">{rule.permissionCodeA}</code>
                  <span className="text-muted-foreground">+</span>
                  <code className="text-xs">{rule.permissionCodeB}</code>
                  {!rule.active && <Badge variant="outline">Inactive</Badge>}
                </span>
                {rule.description && <span className="text-xs text-muted-foreground">{rule.description}</span>}
              </div>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={deleteMutation.isPending}
                onClick={() => deleteMutation.mutate(rule.id)}
              >
                Delete
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** F0.2.7 / IAM-7: configurable Segregation-of-Duties rules — the conflicting-permission pairs role assignment checks against. */
export default function SodRulesAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Segregation of Duties rules</h1>
        <p className="text-sm text-muted-foreground">
          A user can never end up holding both permissions in an active rule — assigning a role that would is
          blocked and the conflict is shown on the role&apos;s assignment screen.
        </p>
      </div>
      <RequireCompany>{(companyId) => <SodRuleList companyId={companyId} />}</RequireCompany>
    </div>
  );
}
