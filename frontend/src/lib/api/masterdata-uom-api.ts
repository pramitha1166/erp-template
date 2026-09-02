import { apiFetch } from "./http";

/** Mirrors `UomController.UomView` (MDM-7). */
export interface UomView {
  id: string;
  code: string;
  name: string;
  disabled: boolean;
}

/** Mirrors `UomController.ConversionView` (MDM-7). */
export interface ConversionView {
  id: string;
  fromUomId: string;
  toUomId: string;
  conversionFactor: number;
}

/** Every UOM belonging to the caller's own tenant. UOMs are shared across a tenant's companies. */
export function listUoms(): Promise<UomView[]> {
  return apiFetch<UomView[]>("/masterdata/uoms");
}

/** `authorizingCompanyId` anchors the permission check — UOMs aren't themselves company-scoped. */
export function createUom(authorizingCompanyId: string, code: string, name: string): Promise<UomView> {
  return apiFetch<UomView>("/masterdata/uoms", { method: "POST", query: { authorizingCompanyId }, body: { code, name } });
}

export function disableUom(uomId: string, authorizingCompanyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/uoms/${uomId}/disable`, { method: "POST", query: { authorizingCompanyId } });
}

export function enableUom(uomId: string, authorizingCompanyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/uoms/${uomId}/enable`, { method: "POST", query: { authorizingCompanyId } });
}

export function configureConversion(
  authorizingCompanyId: string,
  fromUomId: string,
  toUomId: string,
  conversionFactor: number,
): Promise<ConversionView> {
  return apiFetch<ConversionView>("/masterdata/uoms/conversions", {
    method: "POST",
    query: { authorizingCompanyId },
    body: { fromUomId, toUomId, conversionFactor },
  });
}

export function conversionsFrom(uomId: string): Promise<ConversionView[]> {
  return apiFetch<ConversionView[]>(`/masterdata/uoms/${uomId}/conversions`);
}
