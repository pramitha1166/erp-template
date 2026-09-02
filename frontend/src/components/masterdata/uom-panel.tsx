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
  type UomView,
  configureConversion,
  conversionsFrom,
  createUom,
  disableUom,
  enableUom,
  listUoms,
} from "@/lib/api/masterdata-uom-api";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const uomSchema = z.object({ code: z.string().min(1, "Code is required"), name: z.string().min(1, "Name is required") });
type UomFormValues = z.infer<typeof uomSchema>;

const conversionSchema = z.object({
  fromUomId: z.string().min(1, "Choose a UOM"),
  toUomId: z.string().min(1, "Choose a UOM"),
  conversionFactor: z.string().regex(/^\d*\.?\d+$/, "Must be a positive number"),
});
type ConversionFormValues = z.infer<typeof conversionSchema>;

export interface UomPanelProps {
  companyId: string;
}

/** F0.6.6 / MDM-7: units of measure and their pairwise conversion factors. */
export function UomPanel({ companyId }: UomPanelProps) {
  const queryClient = useQueryClient();
  const uomsQueryKey = ["masterdata", "uoms"];
  const [conversionFromId, setConversionFromId] = useState("");

  const { data: uoms, isLoading, isError } = useQuery({ queryKey: uomsQueryKey, queryFn: listUoms });
  const uomOptions = (uoms ?? []).map((u) => ({ value: u.id, label: `${u.code} — ${u.name}` }));

  const conversionsQueryKey = ["masterdata", "uom-conversions", conversionFromId];
  const { data: conversions } = useQuery({
    queryKey: conversionsQueryKey,
    queryFn: () => conversionsFrom(conversionFromId),
    enabled: conversionFromId.length > 0,
  });

  const uomForm = useForm<UomFormValues>({ resolver: zodResolver(uomSchema), defaultValues: { code: "", name: "" } });
  const conversionForm = useForm<ConversionFormValues>({
    resolver: zodResolver(conversionSchema),
    defaultValues: { fromUomId: "", toUomId: "", conversionFactor: "" },
  });

  const createUomMutation = useMutation({
    mutationFn: (values: UomFormValues) => createUom(companyId, values.code, values.name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: uomsQueryKey });
      uomForm.reset({ code: "", name: "" });
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: UomView) => (target.disabled ? enableUom(target.id, companyId) : disableUom(target.id, companyId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: uomsQueryKey }),
  });

  const conversionMutation = useMutation({
    mutationFn: (values: ConversionFormValues) =>
      configureConversion(companyId, values.fromUomId, values.toUomId, Number(values.conversionFactor)),
    onSuccess: (_result, values) => {
      queryClient.invalidateQueries({ queryKey: ["masterdata", "uom-conversions", values.fromUomId] });
      conversionForm.reset({ fromUomId: values.fromUomId, toUomId: "", conversionFactor: "" });
      setConversionFromId(values.fromUomId);
    },
  });

  function uomLabel(uomId: string): string {
    return uoms?.find((u) => u.id === uomId)?.code ?? uomId;
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Units of measure</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load units of measure.
          </p>
        )}
        {uoms && uoms.length === 0 && <p className="text-sm text-muted-foreground">No units of measure yet.</p>}
        {uoms && uoms.length > 0 && (
          <ul className="flex flex-col gap-2">
            {uoms.map((uom) => (
              <li key={uom.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm">
                <span className="font-medium">
                  {uom.code} — {uom.name}
                </span>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={uom.disabled} />
                  <DisableToggleButton
                    disabled={uom.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(uom)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">Add a unit of measure</h3>
        <Form {...uomForm}>
          <form onSubmit={uomForm.handleSubmit((values) => createUomMutation.mutate(values))} className="flex flex-col gap-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <ValidatedTextField control={uomForm.control} name="code" label="Code" placeholder="e.g. KG" />
              <ValidatedTextField control={uomForm.control} name="name" label="Name" placeholder="e.g. Kilogram" />
            </div>
            {createUomMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {createUomMutation.error instanceof ApiError ? createUomMutation.error.message : "Could not create this UOM."}
              </p>
            )}
            <Button type="submit" disabled={createUomMutation.isPending} className="self-start">
              {createUomMutation.isPending ? "Adding…" : "Add"}
            </Button>
          </form>
        </Form>
      </section>

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h3 className="text-sm font-semibold">Conversion factors</h3>
        <Form {...conversionForm}>
          <form
            onSubmit={conversionForm.handleSubmit((values) => conversionMutation.mutate(values))}
            className="flex flex-col gap-3"
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <ValidatedSelectField
                control={conversionForm.control}
                name="fromUomId"
                label="From (e.g. purchase UOM)"
                options={[{ value: "", label: "Select…" }, ...uomOptions]}
              />
              <ValidatedSelectField
                control={conversionForm.control}
                name="toUomId"
                label="To (e.g. stock UOM)"
                options={[{ value: "", label: "Select…" }, ...uomOptions]}
              />
              <ValidatedTextField control={conversionForm.control} name="conversionFactor" label="Factor" />
            </div>
            <p className="text-xs text-muted-foreground">
              E.g. 1 BOX = 12 NOS -&gt; From BOX, To NOS, factor 12.
            </p>

            {conversionMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {conversionMutation.error instanceof ApiError
                  ? conversionMutation.error.message
                  : "Could not save this conversion."}
              </p>
            )}

            <Button type="submit" variant="outline" disabled={conversionMutation.isPending} className="self-start">
              {conversionMutation.isPending ? "Saving…" : "Save conversion"}
            </Button>
          </form>
        </Form>

        <div className="flex flex-col gap-2 border-t pt-3">
          <label className="text-sm font-medium" htmlFor="conversion-viewer-from">
            View conversions from
          </label>
          <select
            id="conversion-viewer-from"
            className="h-9 rounded-md border bg-transparent px-3 text-sm"
            value={conversionFromId}
            onChange={(e) => setConversionFromId(e.target.value)}
          >
            <option value="">Select a UOM…</option>
            {uomOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {conversionFromId.length > 0 && conversions && conversions.length === 0 && (
            <p className="text-sm text-muted-foreground">No conversions configured from this UOM yet.</p>
          )}
          {conversions && conversions.length > 0 && (
            <ul className="flex flex-col gap-2">
              {conversions.map((conversion) => (
                <li key={conversion.id} className="rounded-md border px-3 py-2 text-sm">
                  1 {uomLabel(conversion.fromUomId)} = {conversion.conversionFactor} {uomLabel(conversion.toUomId)}
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}
