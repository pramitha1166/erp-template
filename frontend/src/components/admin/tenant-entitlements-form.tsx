"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { setTenantEntitlement } from "@/lib/api/admin-api";

const FEATURE_CODE = /^[A-Z][A-Z0-9-]*$/;

const schema = z.object({
  featureCode: z.string().regex(FEATURE_CODE, "Uppercase, starting with a letter (e.g. MOD-LK)"),
  enabled: z.enum(["true", "false"]),
});

type Values = z.infer<typeof schema>;

export interface TenantEntitlementsFormProps {
  brandId: string;
  tenantId: string;
}

/**
 * F0.11.4 / ADM-5: brand-scoped tenant entitlements, bounded by what the
 * Brand itself is entitled to (`EntitlementBoundExceededException` surfaces
 * as a normal form error below). There's no list endpoint for a tenant's
 * current entitlements yet, so this is set-only, like its platform/brand
 * counterparts.
 */
export function TenantEntitlementsForm({ brandId, tenantId }: TenantEntitlementsFormProps) {
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { featureCode: "", enabled: "true" },
  });

  const mutation = useMutation({
    mutationFn: (values: Values) => setTenantEntitlement(brandId, tenantId, values.featureCode, values.enabled === "true"),
    onSuccess: () => form.reset(),
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Entitlements</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
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
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Saving…" : "Save"}
          </Button>
        </form>
      </Form>
      {mutation.isSuccess && <p className="text-sm text-muted-foreground">Saved.</p>}
      {mutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {mutation.error instanceof ApiError ? mutation.error.message : "Could not save that entitlement."}
        </p>
      )}
    </section>
  );
}
