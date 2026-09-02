"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import {
  type AccountingPeriodView,
  type FiscalYearView,
  closeAccountingPeriod,
  closeFiscalYear,
  listAccountingPeriods,
  listFiscalYears,
  reopenAccountingPeriod,
  reopenFiscalYear,
} from "@/lib/api/masterdata-fiscalyear-api";
import { StatusBadge } from "./status-badge";

function PeriodRow({ companyId, period }: { companyId: string; period: AccountingPeriodView }) {
  const queryClient = useQueryClient();
  const queryKey = ["masterdata", "accounting-periods", period.fiscalYearId, companyId];
  const closed = period.status === "CLOSED";

  const toggleMutation = useMutation({
    mutationFn: () =>
      closed ? reopenAccountingPeriod(period.id, companyId) : closeAccountingPeriod(period.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-1.5 text-sm">
      <span>
        {period.name} <span className="text-xs text-muted-foreground">({period.startDate} – {period.endDate})</span>
      </span>
      <div className="flex items-center gap-2">
        <StatusBadge disabled={closed} />
        <Button size="sm" variant="outline" disabled={toggleMutation.isPending} onClick={() => toggleMutation.mutate()}>
          {closed ? "Reopen" : "Close"}
        </Button>
      </div>
      {toggleMutation.isError && (
        <p role="alert" className="w-full text-xs text-destructive">
          {toggleMutation.error instanceof ApiError ? toggleMutation.error.message : "Could not update this period."}
        </p>
      )}
    </li>
  );
}

function FiscalYearRow({ companyId, fiscalYear }: { companyId: string; fiscalYear: FiscalYearView }) {
  const queryClient = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const closed = fiscalYear.status === "CLOSED";

  const periodsQueryKey = ["masterdata", "accounting-periods", fiscalYear.id, companyId];
  const { data: periods } = useQuery({
    queryKey: periodsQueryKey,
    queryFn: () => listAccountingPeriods(fiscalYear.id, companyId),
    enabled: expanded,
  });

  const toggleMutation = useMutation({
    mutationFn: () => (closed ? reopenFiscalYear(fiscalYear.id, companyId) : closeFiscalYear(fiscalYear.id, companyId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["masterdata", "fiscal-years", companyId] }),
  });

  return (
    <li className="flex flex-col gap-2 rounded-md border p-3 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-col">
          <span className="font-medium">{fiscalYear.name}</span>
          <span className="text-xs text-muted-foreground">
            {fiscalYear.startDate} – {fiscalYear.endDate}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge disabled={closed} />
          <Button size="sm" variant="ghost" onClick={() => setExpanded((value) => !value)}>
            {expanded ? "Hide periods" : "View periods"}
          </Button>
          <Button size="sm" variant="outline" disabled={toggleMutation.isPending} onClick={() => toggleMutation.mutate()}>
            {closed ? "Reopen" : "Close"}
          </Button>
        </div>
      </div>

      {toggleMutation.isError && (
        <p role="alert" className="text-xs text-destructive">
          {toggleMutation.error instanceof ApiError
            ? toggleMutation.error.message
            : "Could not update this fiscal year."}
        </p>
      )}

      {expanded && (
        <ul className="flex flex-col gap-1.5 border-t pt-2">
          {(periods ?? []).map((period) => (
            <PeriodRow key={period.id} companyId={companyId} period={period} />
          ))}
        </ul>
      )}
    </li>
  );
}

export interface FiscalYearPanelProps {
  companyId: string;
}

/**
 * F0.6.8 / MDM-9: fiscal year / accounting period administration. Closing a fiscal year is rejected by the backend
 * while it still has an open period (`FiscalYearService.close`) — that guard surfaces here as the same inline error
 * every other master-data action uses, not a bespoke confirmation dialog.
 */
export function FiscalYearPanel({ companyId }: FiscalYearPanelProps) {
  const {
    data: fiscalYears,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["masterdata", "fiscal-years", companyId],
    queryFn: () => listFiscalYears(companyId),
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Fiscal years</h2>

      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {isError && (
        <p role="alert" className="text-sm text-destructive">
          Could not load fiscal years.
        </p>
      )}
      {fiscalYears && fiscalYears.length === 0 && (
        <p className="text-sm text-muted-foreground">No fiscal year yet — one is seeded when the tenant is onboarded.</p>
      )}
      {fiscalYears && fiscalYears.length > 0 && (
        <ul className="flex flex-col gap-2">
          {fiscalYears.map((fiscalYear) => (
            <FiscalYearRow key={fiscalYear.id} companyId={companyId} fiscalYear={fiscalYear} />
          ))}
        </ul>
      )}
    </section>
  );
}
