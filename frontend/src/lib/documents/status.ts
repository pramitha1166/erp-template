/**
 * Mirrors the backend's ARCH-4 state machine (`DocStatus` and
 * `Document#transitionTo` in `config/document`) so the UI never offers an
 * action the backend would reject. Keep this in lockstep with that class —
 * it is not derived from the OpenAPI spec because document status is a
 * fixed platform-wide enum, not a per-module schema.
 */
export const DOC_STATUSES = ["DRAFT", "SUBMITTED", "CANCELLED", "AMENDED"] as const;

export type DocStatus = (typeof DOC_STATUSES)[number];

export const DOC_STATUS_LABELS: Record<DocStatus, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  CANCELLED: "Cancelled",
  AMENDED: "Amended",
};

const ALLOWED_TRANSITIONS: Record<DocStatus, readonly DocStatus[]> = {
  DRAFT: ["SUBMITTED"],
  SUBMITTED: ["CANCELLED", "AMENDED"],
  CANCELLED: [],
  AMENDED: [],
};

export type DocLifecycleAction = "submit" | "cancel" | "amend";

const ACTION_TARGET_STATUS: Record<DocLifecycleAction, DocStatus> = {
  submit: "SUBMITTED",
  cancel: "CANCELLED",
  amend: "AMENDED",
};

export function canTransition(
  status: DocStatus,
  action: DocLifecycleAction,
): boolean {
  return ALLOWED_TRANSITIONS[status].includes(ACTION_TARGET_STATUS[action]);
}
