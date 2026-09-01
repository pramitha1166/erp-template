import { apiFetch } from "./http";

/** Mirrors `NumberingResetPolicy` (NUM-3). */
export type NumberingResetPolicy = "NEVER" | "ANNUAL";

export const RESET_POLICY_OPTIONS: { value: NumberingResetPolicy; label: string }[] = [
  { value: "NEVER", label: "Never resets" },
  { value: "ANNUAL", label: "Resets every fiscal year" },
];

/** Mirrors `NumberingSeriesController.SeriesView` (NUM-1). */
export interface SeriesView {
  id: string;
  companyId: string;
  docType: string;
  prefix: string;
  counterWidth: number;
  resetPolicy: NumberingResetPolicy;
  fiscalYearStartMonth: number;
  active: boolean;
  nextCounter: number;
}

export interface ConfigureSeriesRequest {
  docType: string;
  prefix: string;
  counterWidth: number;
  resetPolicy: NumberingResetPolicy;
  fiscalYearStartMonth: number;
}

export function listSeries(companyId: string): Promise<SeriesView[]> {
  return apiFetch<SeriesView[]>("/numbering/series", { query: { companyId } });
}

/** NUM-1: creates the series for (companyId, docType) if it doesn't exist yet, otherwise reconfigures it. */
export function configureSeries(companyId: string, request: ConfigureSeriesRequest): Promise<SeriesView> {
  return apiFetch<SeriesView>("/numbering/series", { method: "POST", query: { companyId }, body: request });
}

export function activateSeries(seriesId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/numbering/series/${seriesId}/activate`, { method: "POST", query: { companyId } });
}

export function deactivateSeries(seriesId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/numbering/series/${seriesId}/deactivate`, { method: "POST", query: { companyId } });
}
