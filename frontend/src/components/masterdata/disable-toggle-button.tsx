import { Button } from "@/components/ui/button";

export interface DisableToggleButtonProps {
  disabled: boolean;
  pending: boolean;
  onToggle: () => void;
}

/**
 * F0.6.9 / MDM-10: the one enable/disable affordance every master-data list screen uses. There is deliberately no
 * delete action anywhere in this module — master data is soft-delete only, so this toggle is the only lifecycle
 * control a row gets (mirrors the backend's `MasterDataSoftDeleteOnlyTest`, which fails the build if any master-data
 * endpoint is ever a hard DELETE).
 */
export function DisableToggleButton({ disabled, pending, onToggle }: DisableToggleButtonProps) {
  return (
    <Button size="sm" variant="outline" disabled={pending} onClick={onToggle}>
      {disabled ? "Enable" : "Disable"}
    </Button>
  );
}
