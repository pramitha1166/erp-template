"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  type CurrencyView,
  createCurrency,
  disableCurrency,
  enableCurrency,
  listCurrencies,
  rateHistory,
  recordRate,
} from "@/lib/api/masterdata-currency-api";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const currencySchema = z.object({
  code: z
    .string()
    .length(3, "Use a 3-letter ISO currency code, e.g. USD")
    .regex(/^[A-Z]+$/, "Use upper-case letters"),
  name: z.string().min(1, "Name is required"),
  symbol: z.string().optional(),
  decimalPlaces: z
    .string()
    .regex(/^\d+$/, "Must be a whole number")
    .refine((value) => Number(value) >= 0 && Number(value) <= 6, "Must be between 0 and 6"),
});
type CurrencyFormValues = z.infer<typeof currencySchema>;
const emptyCurrencyValues: CurrencyFormValues = { code: "", name: "", symbol: "", decimalPlaces: "2" };

const rateSchema = z.object({
  currencyCode: z.string().min(1, "Choose a currency"),
  rateDate: z.string().min(1, "Date is required"),
  rateToBase: z.string().regex(/^\d*\.?\d+$/, "Must be a positive number"),
});
type RateFormValues = z.infer<typeof rateSchema>;

export interface CurrencyPanelProps {
  companyId: string;
}

/** F0.6.7 / MDM-8: currency management and date-effective exchange rate entry/history. */
export function CurrencyPanel({ companyId }: CurrencyPanelProps) {
  const queryClient = useQueryClient();
  const currenciesQueryKey = ["masterdata", "currencies"];
  const [historyCode, setHistoryCode] = useState("");

  const { data: currencies, isLoading, isError } = useQuery({ queryKey: currenciesQueryKey, queryFn: listCurrencies });

  const historyQueryKey = ["masterdata", "exchange-rates", historyCode];
  const { data: history } = useQuery({
    queryKey: historyQueryKey,
    queryFn: () => rateHistory(historyCode),
    enabled: historyCode.length > 0,
  });

  const currencyForm = useForm<CurrencyFormValues>({ resolver: zodResolver(currencySchema), defaultValues: emptyCurrencyValues });
  const rateForm = useForm<RateFormValues>({
    resolver: zodResolver(rateSchema),
    defaultValues: { currencyCode: "", rateDate: "", rateToBase: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: CurrencyFormValues) =>
      createCurrency(companyId, { ...values, symbol: values.symbol || undefined, decimalPlaces: Number(values.decimalPlaces) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: currenciesQueryKey });
      currencyForm.reset(emptyCurrencyValues);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: CurrencyView) =>
      target.disabled ? enableCurrency(target.id, companyId) : disableCurrency(target.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: currenciesQueryKey }),
  });

  const rateMutation = useMutation({
    mutationFn: (values: RateFormValues) =>
      recordRate(companyId, { currencyCode: values.currencyCode, rateDate: values.rateDate, rateToBase: Number(values.rateToBase) }),
    onSuccess: (_result, values) => {
      queryClient.invalidateQueries({ queryKey: ["masterdata", "exchange-rates", values.currencyCode] });
      setHistoryCode(values.currencyCode);
      rateForm.reset({ currencyCode: values.currencyCode, rateDate: "", rateToBase: "" });
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Currencies</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load currencies.
          </p>
        )}
        {currencies && currencies.length === 0 && <p className="text-sm text-muted-foreground">No currencies enabled yet.</p>}
        {currencies && currencies.length > 0 && (
          <ul className="flex flex-col gap-2">
            {currencies.map((currency) => (
              <li
                key={currency.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <span className="font-medium">
                  {currency.code} — {currency.name}
                  {currency.symbol ? ` (${currency.symbol})` : ""}
                </span>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={currency.disabled} />
                  <DisableToggleButton
                    disabled={currency.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(currency)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">Add a currency</h3>
        <Form {...currencyForm}>
          <form
            onSubmit={currencyForm.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3"
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <ValidatedTextField control={currencyForm.control} name="code" label="ISO code" placeholder="e.g. USD" />
              <ValidatedTextField control={currencyForm.control} name="name" label="Name" placeholder="e.g. US Dollar" />
              <ValidatedTextField control={currencyForm.control} name="symbol" label="Symbol" placeholder="e.g. $" />
            </div>
            <ValidatedTextField control={currencyForm.control} name="decimalPlaces" label="Decimal places" type="number" />

            {createMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create this currency."}
              </p>
            )}
            <Button type="submit" disabled={createMutation.isPending} className="self-start">
              {createMutation.isPending ? "Adding…" : "Add"}
            </Button>
          </form>
        </Form>
      </section>

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h3 className="text-sm font-semibold">Exchange rates</h3>
        <Form {...rateForm}>
          <form onSubmit={rateForm.handleSubmit((values) => rateMutation.mutate(values))} className="flex flex-col gap-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="grid gap-2">
                <label className="text-sm font-medium" htmlFor="rate-currency-code">
                  Currency
                </label>
                <select
                  id="rate-currency-code"
                  className="h-9 rounded-md border bg-transparent px-3 text-sm"
                  {...rateForm.register("currencyCode")}
                >
                  <option value="">Select…</option>
                  {(currencies ?? []).map((currency) => (
                    <option key={currency.id} value={currency.code}>
                      {currency.code}
                    </option>
                  ))}
                </select>
              </div>
              <ValidatedTextField control={rateForm.control} name="rateDate" label="Effective date" type="date" />
              <ValidatedTextField control={rateForm.control} name="rateToBase" label="Rate to base currency" />
            </div>

            {rateMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {rateMutation.error instanceof ApiError ? rateMutation.error.message : "Could not record this rate."}
              </p>
            )}
            <Button type="submit" variant="outline" disabled={rateMutation.isPending} className="self-start">
              {rateMutation.isPending ? "Saving…" : "Record rate"}
            </Button>
          </form>
        </Form>

        <div className="flex flex-col gap-2 border-t pt-3">
          <label className="text-sm font-medium" htmlFor="rate-history-code">
            View rate history for
          </label>
          <select
            id="rate-history-code"
            className="h-9 rounded-md border bg-transparent px-3 text-sm"
            value={historyCode}
            onChange={(e) => setHistoryCode(e.target.value)}
          >
            <option value="">Select a currency…</option>
            {(currencies ?? []).map((currency) => (
              <option key={currency.id} value={currency.code}>
                {currency.code}
              </option>
            ))}
          </select>
          {historyCode.length > 0 && history && history.length === 0 && (
            <p className="text-sm text-muted-foreground">No rates recorded for {historyCode} yet.</p>
          )}
          {history && history.length > 0 && (
            <ul className="flex flex-col gap-2">
              {history.map((rate) => (
                <li key={rate.id} className="rounded-md border px-3 py-2 text-sm">
                  {rate.rateDate}: 1 {rate.currencyCode} = {rate.rateToBase}{" "}
                  <span className="text-xs text-muted-foreground">({rate.source === "CBSL" ? "CBSL import" : "manual"})</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}
