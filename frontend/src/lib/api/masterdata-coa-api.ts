import { apiFetch } from "./http";

/** Mirrors `AccountType` (MDM-3). */
export type AccountType = "ASSET" | "LIABILITY" | "EQUITY" | "INCOME" | "EXPENSE";

export const ACCOUNT_TYPE_OPTIONS: { value: AccountType; label: string }[] = [
  { value: "ASSET", label: "Asset" },
  { value: "LIABILITY", label: "Liability" },
  { value: "EQUITY", label: "Equity" },
  { value: "INCOME", label: "Income" },
  { value: "EXPENSE", label: "Expense" },
];

/** Mirrors `AccountController.AccountView` (MDM-3). */
export interface AccountView {
  id: string;
  code: string;
  name: string;
  accountType: AccountType;
  parentId: string | null;
  group: boolean;
  active: boolean;
}

export interface NewAccountRequest {
  code: string;
  name: string;
  accountType: AccountType;
  parentId: string | null;
  group: boolean;
}

export function listAccounts(companyId: string): Promise<AccountView[]> {
  return apiFetch<AccountView[]>("/masterdata/accounts", { query: { companyId } });
}

export function createAccount(companyId: string, request: NewAccountRequest): Promise<AccountView> {
  return apiFetch<AccountView>("/masterdata/accounts", { method: "POST", query: { companyId }, body: request });
}

export function renameAccount(accountId: string, companyId: string, name: string): Promise<AccountView> {
  return apiFetch<AccountView>(`/masterdata/accounts/${accountId}`, { method: "PUT", query: { companyId }, body: { name } });
}

export function deactivateAccount(accountId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/accounts/${accountId}/deactivate`, { method: "POST", query: { companyId } });
}

export function activateAccount(accountId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/accounts/${accountId}/activate`, { method: "POST", query: { companyId } });
}
