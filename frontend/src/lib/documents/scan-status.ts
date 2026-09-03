/** Mirrors the backend's `ScanStatus` enum (DOC-4). */
export const SCAN_STATUSES = ["PENDING", "CLEAN", "INFECTED", "FAILED"] as const;

export type ScanStatus = (typeof SCAN_STATUSES)[number];

export const SCAN_STATUS_LABELS: Record<ScanStatus, string> = {
  PENDING: "Scan pending",
  CLEAN: "Clean",
  INFECTED: "Infected",
  FAILED: "Scan failed",
};
