import { Badge } from "@/components/ui/badge";

/** F0.6.9: the one disabled/active rendering every master-data list screen uses — never a bespoke variant per screen. */
export function StatusBadge({ disabled }: { disabled: boolean }) {
  return <Badge variant={disabled ? "outline" : "default"}>{disabled ? "Disabled" : "Active"}</Badge>;
}
