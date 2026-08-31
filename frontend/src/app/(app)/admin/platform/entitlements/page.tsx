"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { PlatformAdminNav } from "@/components/admin/platform-admin-nav";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { listPlatformEntitlements, setPlatformEntitlement } from "@/lib/api/admin-api";

const FEATURE_CODE = /^[A-Z][A-Z0-9-]*$/;

const setEntitlementSchema = z.object({
  featureCode: z.string().regex(FEATURE_CODE, "Uppercase, starting with a letter (e.g. MOD-LK)"),
  enabled: z.enum(["true", "false"]),
});

type SetEntitlementValues = z.infer<typeof setEntitlementSchema>;

/**
 * F0.11.1 / ADM-1: platform-wide default feature entitlements that Brands
 * inherit unless overridden (BRD-12). There's no catalog of valid feature
 * codes on the backend (see `admin.FeatureCode`) — any `[A-Z][A-Z0-9-]*`
 * string is accepted, so this is a free-text set rather than a picklist.
 */
export default function PlatformEntitlementsPage() {
  const queryClient = useQueryClient();
  const queryKey = ["platform-entitlements"];

  const { data: entitlements, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: listPlatformEntitlements,
  });

  const form = useForm<SetEntitlementValues>({
    resolver: zodResolver(setEntitlementSchema),
    defaultValues: { featureCode: "", enabled: "true" },
  });

  const setMutation = useMutation({
    mutationFn: (values: SetEntitlementValues) =>
      setPlatformEntitlement(values.featureCode, values.enabled === "true"),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Platform Admin Console</h1>
        <p className="text-sm text-muted-foreground">
          Default feature entitlements every Brand inherits unless it overrides them.
        </p>
      </div>
      <PlatformAdminNav />

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Set a default entitlement</h2>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => setMutation.mutate(values))}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="featureCode" label="Feature code" placeholder="MOD-LK" />
            </div>
            <div className="w-40">
              <ValidatedSelectField
                control={form.control}
                name="enabled"
                label="Enabled"
                options={[
                  { value: "true", label: "Enabled" },
                  { value: "false", label: "Disabled" },
                ]}
              />
            </div>
            <Button type="submit" disabled={setMutation.isPending}>
              {setMutation.isPending ? "Saving…" : "Save"}
            </Button>
          </form>
        </Form>
        {setMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {setMutation.error instanceof ApiError ? setMutation.error.message : "Could not save that entitlement."}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Current defaults</h2>
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            {error instanceof ApiError ? error.message : "Could not load platform entitlements."}
          </p>
        )}
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {entitlements && entitlements.length === 0 && (
          <p className="text-sm text-muted-foreground">No default entitlements set.</p>
        )}
        {entitlements && entitlements.length > 0 && (
          <ul className="flex flex-col gap-2">
            {entitlements.map((entitlement) => (
              <li
                key={entitlement.featureCode}
                className="flex items-center justify-between rounded-md border px-3 py-2 text-sm"
              >
                <code className="text-xs">{entitlement.featureCode}</code>
                <Badge variant={entitlement.enabled ? "outline" : "secondary"}>
                  {entitlement.enabled ? "Enabled" : "Disabled"}
                </Badge>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
