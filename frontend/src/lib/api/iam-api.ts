import { apiFetch } from "./http";

/** Mirrors `SecuritySettingsController` / `SecurityPolicy` (IAM-8 / IAM-9). */
export interface SecurityPolicy {
  idleTimeoutMinutes: number;
  minLength: number;
  requireUpper: boolean;
  requireLower: boolean;
  requireDigit: boolean;
  requireSymbol: boolean;
  historyCount: number;
  expiryDays: number | null;
}

export function getSecurityPolicy(): Promise<SecurityPolicy> {
  return apiFetch<SecurityPolicy>("/iam/security-settings");
}

export function updateSecurityPolicy(companyId: string, policy: SecurityPolicy): Promise<SecurityPolicy> {
  return apiFetch<SecurityPolicy>("/iam/security-settings", {
    method: "PUT",
    query: { companyId },
    body: policy,
  });
}

/** Mirrors `RoleController.RoleView` (IAM-3). */
export interface RoleView {
  id: string;
  name: string;
  description: string | null;
}

export function createRole(companyId: string, name: string, description?: string): Promise<RoleView> {
  return apiFetch<RoleView>("/iam/roles", {
    method: "POST",
    query: { companyId },
    body: { name, description },
  });
}

export interface PermissionView {
  permissionCode: string;
}

export function listRolePermissions(roleId: string): Promise<PermissionView[]> {
  return apiFetch<PermissionView[]>(`/iam/roles/${roleId}/permissions`);
}

export function grantPermission(roleId: string, companyId: string, permissionCode: string): Promise<void> {
  return apiFetch<void>(`/iam/roles/${roleId}/permissions`, {
    method: "POST",
    query: { companyId },
    body: { permissionCode },
  });
}

export function revokePermission(roleId: string, companyId: string, permissionCode: string): Promise<void> {
  return apiFetch<void>(`/iam/roles/${roleId}/permissions/${encodeURIComponent(permissionCode)}`, {
    method: "DELETE",
    query: { companyId },
  });
}

/** Mirrors the `FieldAccess` enum (IAM-5). */
export type FieldAccess = "NONE" | "READ" | "WRITE";

export function setFieldPermission(
  roleId: string,
  companyId: string,
  entityCode: string,
  fieldName: string,
  access: FieldAccess,
): Promise<void> {
  return apiFetch<void>(`/iam/roles/${roleId}/field-permissions`, {
    method: "POST",
    query: { companyId },
    body: { entityCode, fieldName, access },
  });
}

/** Mirrors `UserRoleController.RoleAssignmentView` (IAM-4). */
export interface RoleAssignmentView {
  roleId: string;
}

export function listUserRoles(userId: string, companyId: string): Promise<RoleAssignmentView[]> {
  return apiFetch<RoleAssignmentView[]>(`/iam/users/${userId}/roles`, { query: { companyId } });
}

export function assignRole(userId: string, roleId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/iam/users/${userId}/roles/${roleId}`, {
    method: "POST",
    query: { companyId },
  });
}

export function unassignRole(userId: string, roleId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/iam/users/${userId}/roles/${roleId}`, {
    method: "DELETE",
    query: { companyId },
  });
}

/** Mirrors `SodRuleController.SodRuleView` (IAM-7). */
export interface SodRuleView {
  id: string;
  permissionCodeA: string;
  permissionCodeB: string;
  description: string | null;
  active: boolean;
}

export function listSodRules(): Promise<SodRuleView[]> {
  return apiFetch<SodRuleView[]>("/iam/sod-rules");
}

export function createSodRule(
  companyId: string,
  permissionCodeA: string,
  permissionCodeB: string,
  description?: string,
): Promise<SodRuleView> {
  return apiFetch<SodRuleView>("/iam/sod-rules", {
    method: "POST",
    query: { companyId },
    body: { permissionCodeA, permissionCodeB, description },
  });
}

export function deleteSodRule(ruleId: string, companyId: string): Promise<void> {
  return apiFetch<void>(`/iam/sod-rules/${ruleId}`, { method: "DELETE", query: { companyId } });
}
