import { apiFetch } from "./http";

/** Mirrors `BrandController.BrandView` / `Brand.status` (ADM-1). */
export type BrandStatus = "ACTIVE" | "SUSPENDED";

export interface BrandView {
  id: string;
  name: string;
  legalName: string | null;
  status: BrandStatus;
}

export function listBrands(): Promise<BrandView[]> {
  return apiFetch<BrandView[]>("/admin/brands");
}

export function getBrand(brandId: string): Promise<BrandView> {
  return apiFetch<BrandView>(`/admin/brands/${brandId}`);
}

export function createBrand(name: string, legalName?: string, supportEmail?: string): Promise<BrandView> {
  return apiFetch<BrandView>("/admin/brands", {
    method: "POST",
    body: { name, legalName: legalName || undefined, supportEmail: supportEmail || undefined },
  });
}

export function suspendBrand(brandId: string, reason?: string): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/suspend`, { method: "POST", body: { reason } });
}

export function reactivateBrand(brandId: string): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/reactivate`, { method: "POST" });
}

/** Mirrors `BrandController.ProvisionedAdminView`. Also reused for the platform bootstrap admin. */
export interface ProvisionedAdminView {
  userId: string;
  email: string;
  temporaryPassword: string;
}

export function provisionBrandAdmin(brandId: string, email: string): Promise<ProvisionedAdminView> {
  return apiFetch<ProvisionedAdminView>(`/admin/brands/${brandId}/admins`, {
    method: "POST",
    body: { email },
  });
}

export function setBrandEntitlement(brandId: string, featureCode: string, enabled: boolean): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/entitlements/${encodeURIComponent(featureCode)}`, {
    method: "PUT",
    body: { enabled },
  });
}

/** Mirrors `TenantController.TenantView` / `Tenant.status` (ADM-2, ADM-5). */
export type TenantStatus = "ACTIVE" | "SUSPENDED";

export interface TenantView {
  id: string;
  brandId: string;
  name: string;
  status: TenantStatus;
  primaryCompanyId: string | null;
}

/** Mirrors `TenantController.NewCompanyRequest` (MDM-1 fields carried by onboarding). */
export interface NewCompanyRequest {
  legalName: string;
  registrationNo?: string;
  vatNo?: string;
  address?: string;
  baseCurrency: string;
  fiscalYearStartMonth: number;
}

/** Mirrors `TenantController.OnboardTenantRequest` (ADM-2). */
export interface OnboardTenantRequest {
  tenantName: string;
  company: NewCompanyRequest;
  adminEmail: string;
  initialEntitlementFeatureCodes?: string[];
}

export function onboardTenant(brandId: string, request: OnboardTenantRequest): Promise<TenantView> {
  return apiFetch<TenantView>(`/admin/brands/${brandId}/tenants`, { method: "POST", body: request });
}

export function listBrandTenants(brandId: string): Promise<TenantView[]> {
  return apiFetch<TenantView[]>(`/admin/brands/${brandId}/tenants`);
}

export function listAllTenants(): Promise<TenantView[]> {
  return apiFetch<TenantView[]>("/admin/tenants");
}

/** Mirrors `TenantController.TenantUsage` / `PlatformController.TenantUsage` (ADM-9). */
export interface TenantUsage {
  tenantId: string;
  tenantName: string;
  status: string;
  activeUserCount: number;
}

export function getTenantUsage(brandId: string, tenantId: string): Promise<TenantUsage> {
  return apiFetch<TenantUsage>(`/admin/brands/${brandId}/tenants/${tenantId}/usage`);
}

export function suspendTenant(brandId: string, tenantId: string, reason?: string): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/tenants/${tenantId}/suspend`, {
    method: "POST",
    body: { reason },
  });
}

export function reactivateTenant(brandId: string, tenantId: string): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/tenants/${tenantId}/reactivate`, { method: "POST" });
}

export function setTenantEntitlement(
  brandId: string,
  tenantId: string,
  featureCode: string,
  enabled: boolean,
): Promise<void> {
  return apiFetch<void>(`/admin/brands/${brandId}/tenants/${tenantId}/entitlements/${encodeURIComponent(featureCode)}`, {
    method: "PUT",
    body: { enabled },
  });
}

/** Mirrors `ChecklistController` / `ChecklistItemKey` (ADM-4). */
export const CHECKLIST_ITEM_KEYS = [
  "BRANCHES",
  "USERS_AND_ROLES",
  "CHART_OF_ACCOUNTS_REVIEW",
  "OPENING_BALANCES",
  "FIRST_MASTER_RECORD",
  "FIRST_INVOICE",
] as const;

export type ChecklistItemKey = (typeof CHECKLIST_ITEM_KEYS)[number];

export const CHECKLIST_ITEM_LABELS: Record<ChecklistItemKey, string> = {
  BRANCHES: "Set up branches",
  USERS_AND_ROLES: "Invite users and assign roles",
  CHART_OF_ACCOUNTS_REVIEW: "Review the chart of accounts",
  OPENING_BALANCES: "Enter opening balances",
  FIRST_MASTER_RECORD: "Create your first item, customer, or supplier",
  FIRST_INVOICE: "Post your first invoice",
};

export interface ChecklistItemView {
  itemKey: ChecklistItemKey;
  completed: boolean;
}

export function getChecklist(tenantId: string): Promise<ChecklistItemView[]> {
  return apiFetch<ChecklistItemView[]>(`/admin/tenants/${tenantId}/checklist`);
}

export function setChecklistItemCompleted(
  tenantId: string,
  itemKey: ChecklistItemKey,
  completed: boolean,
): Promise<void> {
  return apiFetch<void>(`/admin/tenants/${tenantId}/checklist/${itemKey}`, {
    method: "PUT",
    body: { completed },
  });
}

/** Mirrors `InviteController.InviteView` / `TenantAdminInvite.status` (ADM-5). */
export type InviteStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";

export interface InviteView {
  id: string;
  email: string;
  status: InviteStatus;
}

export function createInvite(tenantId: string, email: string): Promise<void> {
  return apiFetch<void>(`/admin/tenants/${tenantId}/invites`, { method: "POST", body: { email } });
}

export function listInvites(tenantId: string): Promise<InviteView[]> {
  return apiFetch<InviteView[]>(`/admin/tenants/${tenantId}/invites`);
}

export function revokeInvite(tenantId: string, inviteId: string): Promise<void> {
  return apiFetch<void>(`/admin/tenants/${tenantId}/invites/${inviteId}`, { method: "DELETE" });
}

export interface AcceptedInviteView {
  userId: string;
}

/** Public — no bearer token attached (mirrors `SecurityConfig.PUBLIC_PATHS`). */
export function acceptInvite(tenantId: string, token: string, password: string): Promise<AcceptedInviteView> {
  return apiFetch<AcceptedInviteView>(`/admin/tenants/${tenantId}/invites/accept`, {
    method: "POST",
    body: { token, password },
    auth: false,
  });
}

/** Mirrors `ImpersonationController.StartedView` (ADM-7). */
export interface ImpersonationStartedView {
  sessionId: string;
  token: string;
  expiresAt: string;
}

export function startImpersonationSession(tenantId: string, reason: string): Promise<ImpersonationStartedView> {
  return apiFetch<ImpersonationStartedView>(`/admin/tenants/${tenantId}/impersonation`, {
    method: "POST",
    body: { reason },
  });
}

export function endImpersonationSession(tenantId: string, sessionId: string): Promise<void> {
  return apiFetch<void>(`/admin/tenants/${tenantId}/impersonation/${sessionId}/end`, { method: "POST" });
}

/** Mirrors `DataSubjectRequestController` / `DataRequestType` / `DataRequestStatus` (ADM-8). */
export type DataRequestType = "EXPORT" | "ERASURE";
export type DataRequestStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface DataSubjectRequestView {
  id: string;
  type: DataRequestType;
  status: DataRequestStatus;
  resultPayload: string | null;
}

export function createDataSubjectRequest(
  tenantId: string,
  type: DataRequestType,
  notes?: string,
): Promise<DataSubjectRequestView> {
  return apiFetch<DataSubjectRequestView>(`/admin/tenants/${tenantId}/data-requests`, {
    method: "POST",
    body: { type, notes: notes || undefined },
  });
}

export function listDataSubjectRequests(tenantId: string): Promise<DataSubjectRequestView[]> {
  return apiFetch<DataSubjectRequestView[]>(`/admin/tenants/${tenantId}/data-requests`);
}

/** Mirrors `PlatformController.EntitlementView` (ADM-1 platform defaults). */
export interface EntitlementView {
  featureCode: string;
  enabled: boolean;
}

export function listPlatformEntitlements(): Promise<EntitlementView[]> {
  return apiFetch<EntitlementView[]>("/admin/platform/entitlements");
}

export function setPlatformEntitlement(featureCode: string, enabled: boolean): Promise<void> {
  return apiFetch<void>(`/admin/platform/entitlements/${encodeURIComponent(featureCode)}`, {
    method: "PUT",
    body: { enabled },
  });
}

/** Mirrors `PlatformController.BrandUsage` / `PlatformUsage` (ADM-9). */
export interface BrandUsage {
  brandId: string;
  brandName: string;
  tenantCount: number;
  activeTenantCount: number;
  activeUserCount: number;
}

export interface PlatformUsage {
  byBrand: BrandUsage[];
  totalTenants: number;
  totalActiveUsers: number;
  systemHealth: string;
}

export function getPlatformUsage(): Promise<PlatformUsage> {
  return apiFetch<PlatformUsage>("/admin/platform/usage");
}
