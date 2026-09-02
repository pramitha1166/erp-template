import { apiFetch } from "./http";

/** Mirrors `FiscalYearStatus` (MDM-9). */
export type FiscalYearStatus = "OPEN" | "CLOSED";

/** Mirrors `FiscalYearController.FiscalYearView` (MDM-9). Fiscal years are seeded at onboarding, not created here. */
export interface FiscalYearView {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  status: FiscalYearStatus;
}

/** Mirrors `AccountingPeriodController.AccountingPeriodView` (MDM-9). */
export interface AccountingPeriodView {
  id: string;
  fiscalYearId: string;
  name: string;
  startDate: string;
  endDate: string;
  status: FiscalYearStatus;
}

export function listFiscalYears(companyId: string): Promise<FiscalYearView[]> {
  return apiFetch<FiscalYearView[]>("/masterdata/fiscal-years", { query: { companyId } });
}

export function closeFiscalYear(fiscalYearId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/fiscal-years/${fiscalYearId}/close`, { method: "POST", query: { companyId } });
}

export function reopenFiscalYear(fiscalYearId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/fiscal-years/${fiscalYearId}/reopen`, { method: "POST", query: { companyId } });
}

export function listAccountingPeriods(fiscalYearId: string, companyId: string): Promise<AccountingPeriodView[]> {
  return apiFetch<AccountingPeriodView[]>("/masterdata/accounting-periods", { query: { fiscalYearId, companyId } });
}

export function closeAccountingPeriod(periodId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/accounting-periods/${periodId}/close`, { method: "POST", query: { companyId } });
}

export function reopenAccountingPeriod(periodId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/accounting-periods/${periodId}/reopen`, { method: "POST", query: { companyId } });
}
