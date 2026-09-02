import { apiFetch } from "./http";

/** Mirrors `CompanyController.CompanyView` (MDM-1). */
export interface CompanyView {
  id: string;
  legalName: string;
  registrationNo: string | null;
  vatNo: string | null;
  address: string | null;
  baseCurrency: string;
  fiscalYearStartMonth: number;
  logoUrl: string | null;
  disabled: boolean;
}

export interface NewCompanyRequest {
  legalName: string;
  registrationNo?: string;
  vatNo?: string;
  address?: string;
  baseCurrency: string;
  fiscalYearStartMonth: number;
}

export interface UpdateCompanyRequest {
  legalName: string;
  address?: string;
  logoUrl?: string;
}

/** Every company belonging to the caller's own tenant. */
export function listCompanies(): Promise<CompanyView[]> {
  return apiFetch<CompanyView[]>("/masterdata/companies");
}

export function getCompany(companyId: string): Promise<CompanyView> {
  return apiFetch<CompanyView>(`/masterdata/companies/${companyId}`);
}

/** MDM-2: adds a further company to the tenant. `authorizingCompanyId` anchors the permission check. */
export function createCompany(authorizingCompanyId: string, request: NewCompanyRequest): Promise<CompanyView> {
  return apiFetch<CompanyView>("/masterdata/companies", {
    method: "POST",
    query: { authorizingCompanyId },
    body: request,
  });
}

export function updateCompany(companyId: string, request: UpdateCompanyRequest): Promise<CompanyView> {
  return apiFetch<CompanyView>(`/masterdata/companies/${companyId}`, { method: "PUT", body: request });
}

export function disableCompany(companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/companies/${companyId}/disable`, { method: "POST" });
}

export function enableCompany(companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/companies/${companyId}/enable`, { method: "POST" });
}
