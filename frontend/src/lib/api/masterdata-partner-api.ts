import { apiFetch } from "./http";

/** Mirrors `BusinessPartnerType` (MDM-5). */
export type BusinessPartnerType = "CUSTOMER" | "SUPPLIER" | "BOTH";

export const PARTNER_TYPE_OPTIONS: { value: BusinessPartnerType; label: string }[] = [
  { value: "CUSTOMER", label: "Customer" },
  { value: "SUPPLIER", label: "Supplier" },
  { value: "BOTH", label: "Customer & supplier" },
];

/** Mirrors `BusinessPartnerController.PartnerView` (MDM-5). */
export interface PartnerView {
  id: string;
  partnerType: BusinessPartnerType;
  code: string;
  name: string;
  taxRegistrationNo: string | null;
  creditLimit: number;
  creditTermsDays: number;
  defaultAccountId: string | null;
  bankName: string | null;
  bankBranch: string | null;
  bankAccountNo: string | null;
  bankSwiftCode: string | null;
  disabled: boolean;
}

export interface NewPartnerRequest {
  partnerType: BusinessPartnerType;
  code: string;
  name: string;
}

export interface UpdatePartnerRequest {
  name: string;
  taxRegistrationNo?: string;
  creditLimit: number;
  creditTermsDays: number;
  defaultAccountId?: string;
  bankName?: string;
  bankBranch?: string;
  bankAccountNo?: string;
  bankSwiftCode?: string;
}

/** Mirrors `BusinessPartnerController.ContactView` (MDM-5). */
export interface ContactView {
  id: string;
  name: string;
  designation: string | null;
  phone: string | null;
  email: string | null;
  primaryContact: boolean;
}

export interface NewContactRequest {
  name: string;
  designation?: string;
  phone?: string;
  email?: string;
  primaryContact: boolean;
}

export function listPartners(companyId: string, partnerType?: BusinessPartnerType): Promise<PartnerView[]> {
  return apiFetch<PartnerView[]>("/masterdata/business-partners", { query: { companyId, partnerType } });
}

export function createPartner(companyId: string, request: NewPartnerRequest): Promise<PartnerView> {
  return apiFetch<PartnerView>("/masterdata/business-partners", { method: "POST", query: { companyId }, body: request });
}

export function updatePartner(partnerId: string, companyId: string, request: UpdatePartnerRequest): Promise<PartnerView> {
  return apiFetch<PartnerView>(`/masterdata/business-partners/${partnerId}`, {
    method: "PUT",
    query: { companyId },
    body: request,
  });
}

export function disablePartner(partnerId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/business-partners/${partnerId}/disable`, { method: "POST", query: { companyId } });
}

export function enablePartner(partnerId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/business-partners/${partnerId}/enable`, { method: "POST", query: { companyId } });
}

export function addContact(partnerId: string, companyId: string, request: NewContactRequest): Promise<ContactView> {
  return apiFetch<ContactView>(`/masterdata/business-partners/${partnerId}/contacts`, {
    method: "POST",
    query: { companyId },
    body: request,
  });
}

export function listContacts(partnerId: string, companyId: string): Promise<ContactView[]> {
  return apiFetch<ContactView[]>(`/masterdata/business-partners/${partnerId}/contacts`, { query: { companyId } });
}
