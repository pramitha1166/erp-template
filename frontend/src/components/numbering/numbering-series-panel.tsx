"use client";

import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  RESET_POLICY_OPTIONS,
  activateSeries,
  configureSeries,
  deactivateSeries,
  listSeries,
  type SeriesView,
} from "@/lib/api/numbering-api";
import { previewNextNumber } from "@/lib/numbering/preview-number";

const seriesSchema = z
  .object({
    docType: z
      .string()
      .min(1, "Document type is required")
      .regex(/^[A-Z][A-Z0-9_]*$/, "Use upper-case letters, digits, and underscores, e.g. SALES_INVOICE"),
    prefix: z.string().min(1, "Prefix is required"),
    counterWidth: z
      .string()
      .regex(/^\d+$/, "Counter width must be a whole number")
      .refine((value) => Number(value) >= 1 && Number(value) <= 10, "Counter width must be between 1 and 10"),
    resetPolicy: z.enum(["NEVER", "ANNUAL"]),
    fiscalYearStartMonth: z.string().optional(),
  })
  .refine(
    (values) =>
      values.resetPolicy !== "ANNUAL" ||
      (!!values.fiscalYearStartMonth &&
        /^\d+$/.test(values.fiscalYearStartMonth) &&
        Number(values.fiscalYearStartMonth) >= 1 &&
        Number(values.fiscalYearStartMonth) <= 12),
    { message: "Fiscal-year start month (1-12) is required when the reset policy is annual", path: ["fiscalYearStartMonth"] },
  );

type SeriesFormValues = z.infer<typeof seriesSchema>;

const emptyFormValues: SeriesFormValues = {
  docType: "",
  prefix: "",
  counterWidth: "5",
  resetPolicy: "NEVER",
  fiscalYearStartMonth: "1",
};

function toFormValues(series: SeriesView): SeriesFormValues {
  return {
    docType: series.docType,
    prefix: series.prefix,
    counterWidth: String(series.counterWidth),
    resetPolicy: series.resetPolicy,
    fiscalYearStartMonth: String(series.fiscalYearStartMonth),
  };
}

export interface NumberingSeriesPanelProps {
  companyId: string;
}

/** F0.5.1 / F0.5.2 / F0.5.3 / F0.5.4: naming-series configuration, live preview, and lifecycle actions. */
export function NumberingSeriesPanel({ companyId }: NumberingSeriesPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["numbering", "series", companyId];
  const [editingSeries, setEditingSeries] = useState<SeriesView | null>(null);

  const {
    data: series,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: () => listSeries(companyId),
  });

  const form = useForm<SeriesFormValues>({
    resolver: zodResolver(seriesSchema),
    defaultValues: emptyFormValues,
  });

  const resetPolicy = form.watch("resetPolicy");
  const watchedPrefix = form.watch("prefix");
  const watchedCounterWidth = form.watch("counterWidth");
  const watchedFiscalYearStartMonth = form.watch("fiscalYearStartMonth");

  const preview = useMemo(() => {
    const counterWidth = Number(watchedCounterWidth);
    const fiscalYearStartMonth = Number(watchedFiscalYearStartMonth);
    if (!watchedPrefix || !Number.isFinite(counterWidth) || counterWidth < 1) {
      return null;
    }
    const nextCounter = editingSeries ? editingSeries.nextCounter : 1;
    return previewNextNumber(
      watchedPrefix,
      counterWidth,
      resetPolicy,
      Number.isFinite(fiscalYearStartMonth) ? fiscalYearStartMonth : 1,
      nextCounter,
    );
  }, [watchedPrefix, watchedCounterWidth, watchedFiscalYearStartMonth, resetPolicy, editingSeries]);

  const configureMutation = useMutation({
    mutationFn: (values: SeriesFormValues) =>
      configureSeries(companyId, {
        docType: values.docType,
        prefix: values.prefix,
        counterWidth: Number(values.counterWidth),
        resetPolicy: values.resetPolicy,
        fiscalYearStartMonth: Number(values.fiscalYearStartMonth || "1"),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset(emptyFormValues);
      setEditingSeries(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: SeriesView) =>
      target.active ? deactivateSeries(target.id, companyId) : activateSeries(target.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEditing(target: SeriesView) {
    setEditingSeries(target);
    form.reset(toFormValues(target));
  }

  function cancelEditing() {
    setEditingSeries(null);
    form.reset(emptyFormValues);
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Naming series</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading series…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load naming series.
          </p>
        )}
        {series && series.length === 0 && (
          <p className="text-sm text-muted-foreground">No naming series configured yet for this company.</p>
        )}
        {series && series.length > 0 && (
          <ul className="flex flex-col gap-2">
            {series.map((item) => (
              <li
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{item.docType}</span>
                  <span className="text-xs text-muted-foreground">
                    {item.prefix} · width {item.counterWidth} ·{" "}
                    {item.resetPolicy === "ANNUAL"
                      ? `resets annually (FY starts month ${item.fiscalYearStartMonth})`
                      : "never resets"}{" "}
                    · next #{item.nextCounter}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={item.active ? "default" : "outline"}>{item.active ? "Active" : "Inactive"}</Badge>
                  <Button size="sm" variant="ghost" onClick={() => startEditing(item)}>
                    Edit
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={toggleMutation.isPending}
                    onClick={() => toggleMutation.mutate(item)}
                  >
                    {item.active ? "Deactivate" : "Activate"}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">{editingSeries ? `Edit ${editingSeries.docType}` : "Add a series"}</h3>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => configureMutation.mutate(values))}
            className="flex flex-col gap-3"
          >
            {editingSeries ? (
              <p className="text-sm text-muted-foreground">
                Document type: <span className="font-medium text-foreground">{editingSeries.docType}</span>
              </p>
            ) : (
              <ValidatedTextField
                control={form.control}
                name="docType"
                label="Document type"
                placeholder="e.g. SALES_INVOICE"
              />
            )}
            <ValidatedTextField
              control={form.control}
              name="prefix"
              label="Prefix template"
              placeholder="e.g. SINV-{YYYY}-"
            />
            <p className="text-xs text-muted-foreground">
              Placeholders: <code>{"{YYYY}"}</code>, <code>{"{YY}"}</code>, <code>{"{MM}"}</code>, <code>{"{FY}"}</code>
            </p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <ValidatedTextField control={form.control} name="counterWidth" label="Counter width" type="number" />
              <ValidatedSelectField
                control={form.control}
                name="resetPolicy"
                label="Reset policy"
                options={RESET_POLICY_OPTIONS}
              />
            </div>
            {resetPolicy === "ANNUAL" && (
              <ValidatedTextField
                control={form.control}
                name="fiscalYearStartMonth"
                label="Fiscal-year start month (1-12)"
                type="number"
              />
            )}

            {preview && (
              <p className="text-sm">
                Next number preview: <span className="font-mono font-medium">{preview}</span>
              </p>
            )}

            {configureMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {configureMutation.error instanceof ApiError
                  ? configureMutation.error.message
                  : "Could not save the naming series."}
              </p>
            )}

            <div className="flex gap-2">
              <Button type="submit" disabled={configureMutation.isPending} className="self-start">
                {configureMutation.isPending ? "Saving…" : editingSeries ? "Save changes" : "Add series"}
              </Button>
              {editingSeries && (
                <Button type="button" variant="ghost" onClick={cancelEditing}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </Form>
      </section>
    </div>
  );
}
