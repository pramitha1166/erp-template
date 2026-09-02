"use client";

import {
  createItemGroup,
  disableItemGroup,
  enableItemGroup,
  listItemGroups,
  renameItemGroup,
} from "@/lib/api/masterdata-item-api";
import { HierarchicalCodeNamePanel } from "./hierarchical-code-name-panel";

export interface ItemGroupPanelProps {
  companyId: string;
}

/** F0.6.5 / MDM-6: hierarchical item group tree. */
export function ItemGroupPanel({ companyId }: ItemGroupPanelProps) {
  return (
    <HierarchicalCodeNamePanel
      companyId={companyId}
      queryKey={["masterdata", "item-groups", companyId]}
      itemLabel="item group"
      emptyMessage="No item groups yet."
      list={listItemGroups}
      create={createItemGroup}
      rename={renameItemGroup}
      disable={disableItemGroup}
      enable={enableItemGroup}
    />
  );
}
