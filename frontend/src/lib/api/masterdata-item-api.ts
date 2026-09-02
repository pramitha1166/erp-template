import { apiFetch } from "./http";

/** Mirrors `ItemGroupController.ItemGroupView` (MDM-6). */
export interface ItemGroupView {
  id: string;
  code: string;
  name: string;
  parentId: string | null;
  disabled: boolean;
}

export function listItemGroups(companyId: string): Promise<ItemGroupView[]> {
  return apiFetch<ItemGroupView[]>("/masterdata/item-groups", { query: { companyId } });
}

export function createItemGroup(
  companyId: string,
  request: { code: string; name: string; parentId: string | null },
): Promise<ItemGroupView> {
  return apiFetch<ItemGroupView>("/masterdata/item-groups", { method: "POST", query: { companyId }, body: request });
}

export function renameItemGroup(itemGroupId: string, companyId: string, name: string): Promise<ItemGroupView> {
  return apiFetch<ItemGroupView>(`/masterdata/item-groups/${itemGroupId}`, {
    method: "PUT",
    query: { companyId },
    body: { name },
  });
}

export function disableItemGroup(itemGroupId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/item-groups/${itemGroupId}/disable`, { method: "POST", query: { companyId } });
}

export function enableItemGroup(itemGroupId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/item-groups/${itemGroupId}/enable`, { method: "POST", query: { companyId } });
}

/** Mirrors `ValuationMethod` (MDM-6). */
export type ValuationMethod = "FIFO" | "WEIGHTED_AVERAGE" | "STANDARD_COST";

export const VALUATION_METHOD_OPTIONS: { value: ValuationMethod; label: string }[] = [
  { value: "FIFO", label: "FIFO" },
  { value: "WEIGHTED_AVERAGE", label: "Weighted average" },
  { value: "STANDARD_COST", label: "Standard cost" },
];

/** Mirrors `ItemController.ItemView` (MDM-6 / MDM-7). */
export interface ItemView {
  id: string;
  code: string;
  name: string;
  itemGroupId: string;
  stockUomId: string;
  purchaseUomId: string | null;
  valuationMethod: ValuationMethod;
  reorderLevel: number;
  batchTracked: boolean;
  serialTracked: boolean;
  taxCategoryCode: string | null;
  hsCode: string | null;
  disabled: boolean;
}

export interface NewItemRequest {
  code: string;
  name: string;
  itemGroupId: string;
  stockUomId: string;
  valuationMethod: ValuationMethod;
}

export interface UpdateItemRequest {
  name: string;
  itemGroupId: string;
  purchaseUomId?: string;
  valuationMethod: ValuationMethod;
  reorderLevel: number;
  batchTracked: boolean;
  serialTracked: boolean;
  taxCategoryCode?: string;
  hsCode?: string;
}

export function listItems(companyId: string): Promise<ItemView[]> {
  return apiFetch<ItemView[]>("/masterdata/items", { query: { companyId } });
}

export function createItem(companyId: string, request: NewItemRequest): Promise<ItemView> {
  return apiFetch<ItemView>("/masterdata/items", { method: "POST", query: { companyId }, body: request });
}

export function updateItem(itemId: string, companyId: string, request: UpdateItemRequest): Promise<ItemView> {
  return apiFetch<ItemView>(`/masterdata/items/${itemId}`, { method: "PUT", query: { companyId }, body: request });
}

export function disableItem(itemId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/items/${itemId}/disable`, { method: "POST", query: { companyId } });
}

export function enableItem(itemId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/masterdata/items/${itemId}/enable`, { method: "POST", query: { companyId } });
}
