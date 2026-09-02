import { apiFetch } from "./http";

/** Mirrors `CostCentreController.CostCentreView` (MDM-4). */
export interface CostCentreView {
  id: string;
  code: string;
  name: string;
  parentId: string | null;
  disabled: boolean;
}

export function listCostCentres(companyId: string): Promise<CostCentreView[]> {
  return apiFetch<CostCentreView[]>("/masterdata/cost-centres", { query: { companyId } });
}

export function createCostCentre(
  companyId: string,
  request: { code: string; name: string; parentId: string | null },
): Promise<CostCentreView> {
  return apiFetch<CostCentreView>("/masterdata/cost-centres", { method: "POST", query: { companyId }, body: request });
}

export function renameCostCentre(costCentreId: string, companyId: string, name: string): Promise<CostCentreView> {
  return apiFetch<CostCentreView>(`/masterdata/cost-centres/${costCentreId}`, {
    method: "PUT",
    query: { companyId },
    body: { name },
  });
}

export function disableCostCentre(costCentreId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/cost-centres/${costCentreId}/disable`, { method: "POST", query: { companyId } });
}

export function enableCostCentre(costCentreId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/cost-centres/${costCentreId}/enable`, { method: "POST", query: { companyId } });
}
