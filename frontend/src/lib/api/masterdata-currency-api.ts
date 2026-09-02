import { apiFetch } from "./http";

/** Mirrors `ExchangeRateSource` (MDM-8). */
export type ExchangeRateSource = "MANUAL" | "CBSL";

/** Mirrors `CurrencyController.CurrencyView` (MDM-8). */
export interface CurrencyView {
  id: string;
  code: string;
  name: string;
  symbol: string | null;
  decimalPlaces: number;
  disabled: boolean;
}

/** Mirrors `CurrencyController.ExchangeRateView` (MDM-8). */
export interface ExchangeRateView {
  id: string;
  currencyCode: string;
  rateDate: string;
  rateToBase: number;
  source: ExchangeRateSource;
}

/** Every currency enabled for the caller's own tenant. Currencies are shared across a tenant's companies. */
export function listCurrencies(): Promise<CurrencyView[]> {
  return apiFetch<CurrencyView[]>("/masterdata/currencies");
}

/** `authorizingCompanyId` anchors the permission check — currencies aren't themselves company-scoped. */
export function createCurrency(
  authorizingCompanyId: string,
  request: { code: string; name: string; symbol?: string; decimalPlaces: number },
): Promise<CurrencyView> {
  return apiFetch<CurrencyView>("/masterdata/currencies", {
    method: "POST",
    query: { authorizingCompanyId },
    body: request,
  });
}

export function disableCurrency(currencyId: string, authorizingCompanyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/currencies/${currencyId}/disable`, { method: "POST", query: { authorizingCompanyId } });
}

export function enableCurrency(currencyId: string, authorizingCompanyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/currencies/${currencyId}/enable`, { method: "POST", query: { authorizingCompanyId } });
}

export function recordRate(
  authorizingCompanyId: string,
  request: { currencyCode: string; rateDate: string; rateToBase: number },
): Promise<ExchangeRateView> {
  return apiFetch<ExchangeRateView>("/masterdata/currencies/rates", {
    method: "POST",
    query: { authorizingCompanyId },
    body: request,
  });
}

export function rateHistory(currencyCode: string): Promise<ExchangeRateView[]> {
  return apiFetch<ExchangeRateView[]>(`/masterdata/currencies/${currencyCode}/rates`);
}
