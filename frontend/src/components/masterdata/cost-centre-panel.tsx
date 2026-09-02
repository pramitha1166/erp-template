"use client";

import {
  createCostCentre,
  disableCostCentre,
  enableCostCentre,
  listCostCentres,
  renameCostCentre,
} from "@/lib/api/masterdata-costcentre-api";
import { HierarchicalCodeNamePanel } from "./hierarchical-code-name-panel";

export interface CostCentrePanelProps {
  companyId: string;
}

/** F0.6.3 / MDM-4: hierarchical cost centre tree. */
export function CostCentrePanel({ companyId }: CostCentrePanelProps) {
  return (
    <HierarchicalCodeNamePanel
      companyId={companyId}
      queryKey={["masterdata", "cost-centres", companyId]}
      itemLabel="cost centre"
      emptyMessage="No cost centres yet."
      list={listCostCentres}
      create={createCostCentre}
      rename={renameCostCentre}
      disable={disableCostCentre}
      enable={enableCostCentre}
    />
  );
}
