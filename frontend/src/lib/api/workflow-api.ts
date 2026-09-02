import { apiFetch } from "./http";

/** Mirrors `ApproverType` (WF-3): how a step, or its escalation target, resolves to approver user ids. */
export type ApproverType = "ROLE" | "USER" | "HIERARCHY";

/** Mirrors `ConditionOperator` (WF-2). */
export type ConditionOperator = "EQ" | "NE" | "GT" | "GTE" | "LT" | "LTE";

export const CONDITION_OPERATOR_LABELS: Record<ConditionOperator, string> = {
  EQ: "equals",
  NE: "does not equal",
  GT: "is greater than",
  GTE: "is greater than or equal to",
  LT: "is less than",
  LTE: "is less than or equal to",
};

/** Mirrors `ApprovalChainController.ChainView` (WF-1). */
export interface ChainView {
  id: string;
  companyId: string;
  documentType: string;
  name: string;
  active: boolean;
}

export function listChains(companyId: string, documentType: string): Promise<ChainView[]> {
  return apiFetch<ChainView[]>("/workflow/chains", { query: { companyId, documentType } });
}

export function createChain(companyId: string, documentType: string, name: string): Promise<ChainView> {
  return apiFetch<ChainView>("/workflow/chains", {
    method: "POST",
    query: { companyId },
    body: { documentType, name },
  });
}

export function activateChain(chainId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/workflow/chains/${chainId}/activate`, { method: "POST", query: { companyId } });
}

export function deactivateChain(chainId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/workflow/chains/${chainId}/deactivate`, { method: "POST", query: { companyId } });
}

/** Mirrors `ApprovalChainController.StepView`. */
export interface StepView {
  id: string;
  sequenceOrder: number;
  name: string;
  approverType: ApproverType;
  approverRoleId: string | null;
  approverUserId: string | null;
  hierarchyLevel: number | null;
  escalationHours: number | null;
}

export interface CreateStepRequest {
  sequenceOrder: number;
  name: string;
  approverType: ApproverType;
  roleId?: string;
  userId?: string;
  hierarchyLevel?: number;
}

export function listSteps(chainId: string, companyId: string): Promise<StepView[]> {
  return apiFetch<StepView[]>(`/workflow/chains/${chainId}/steps`, { query: { companyId } });
}

export function addStep(chainId: string, companyId: string, request: CreateStepRequest): Promise<StepView> {
  return apiFetch<StepView>(`/workflow/chains/${chainId}/steps`, {
    method: "POST",
    query: { companyId },
    body: request,
  });
}

export interface EscalationRequest {
  hours: number;
  escalationType?: ApproverType;
  roleId?: string;
  userId?: string;
  hierarchyLevel?: number;
}

export function configureEscalation(
  chainId: string,
  stepId: string,
  companyId: string,
  request: EscalationRequest,
): Promise<void> {
  return apiFetch<void>(`/workflow/chains/${chainId}/steps/${stepId}/escalation`, {
    method: "POST",
    query: { companyId },
    body: request,
  });
}

/** Mirrors `ApprovalChainController.ConditionView` (WF-2). */
export interface ConditionView {
  id: string;
  fieldName: string;
  operator: ConditionOperator;
  valueString: string | null;
  valueNumber: string | null;
}

export interface CreateConditionRequest {
  fieldName: string;
  operator: ConditionOperator;
  valueString?: string;
  valueNumber?: string;
}

export function listConditions(stepId: string, companyId: string): Promise<ConditionView[]> {
  return apiFetch<ConditionView[]>(`/workflow/chains/steps/${stepId}/conditions`, { query: { companyId } });
}

export function addCondition(
  chainId: string,
  stepId: string,
  companyId: string,
  request: CreateConditionRequest,
): Promise<ConditionView> {
  return apiFetch<ConditionView>(`/workflow/chains/${chainId}/steps/${stepId}/conditions`, {
    method: "POST",
    query: { companyId },
    body: request,
  });
}

/** Mirrors `TaskStatus` (WF-5/WF-6/WF-8). `ESCALATED` is a pending task past its step's escalation deadline. */
export type TaskStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "ESCALATED";

/** Mirrors `ApprovalTaskController.TaskView`. */
export interface TaskView {
  id: string;
  instanceId: string;
  documentType: string;
  documentId: string;
  status: TaskStatus;
  dueAt: string | null;
}

/** WF-8: the pending-approval inbox — every task assigned to the caller, across document types. */
export function myPendingTasks(): Promise<TaskView[]> {
  return apiFetch<TaskView[]>("/workflow/tasks/mine");
}

export function approveTask(taskId: string, companyId: string, comment?: string): Promise<void> {
  return apiFetch<void>(`/workflow/tasks/${taskId}/approve`, {
    method: "POST",
    query: { companyId },
    body: comment ? { comment } : undefined,
  });
}

/** WF-6: rejection always requires a comment — enforced server-side, not just by this signature. */
export function rejectTask(taskId: string, companyId: string, comment: string): Promise<void> {
  return apiFetch<void>(`/workflow/tasks/${taskId}/reject`, {
    method: "POST",
    query: { companyId },
    body: { comment },
  });
}

/** Mirrors `ApprovalDelegationController.DelegationView` (WF-5). */
export interface DelegationView {
  id: string;
  delegatorUserId: string;
  delegateUserId: string;
  startDate: string;
  endDate: string;
  reason: string | null;
  revoked: boolean;
}

export function createDelegation(
  delegateUserId: string,
  startDate: string,
  endDate: string,
  reason?: string,
): Promise<DelegationView> {
  return apiFetch<DelegationView>("/workflow/delegations", {
    method: "POST",
    body: { delegateUserId, startDate, endDate, reason: reason || undefined },
  });
}

export function myDelegations(): Promise<DelegationView[]> {
  return apiFetch<DelegationView[]>("/workflow/delegations/mine");
}

export function revokeDelegation(delegationId: string): Promise<void> {
  return apiFetch<void>(`/workflow/delegations/${delegationId}`, { method: "DELETE" });
}

/** Mirrors `ApprovalHistoryController.HistoryEntryView` (WF-7). */
export interface ApprovalHistoryEntry {
  instanceId: string;
  action: string;
  actorUserId: string | null;
  comment: string | null;
  occurredAt: string;
}

export function getApprovalHistory(
  companyId: string,
  documentType: string,
  documentId: string,
): Promise<ApprovalHistoryEntry[]> {
  return apiFetch<ApprovalHistoryEntry[]>("/workflow/history", {
    query: { companyId, documentType, documentId },
  });
}
