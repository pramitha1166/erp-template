import { apiFetch } from "./http";

/** Mirrors `AuditAction` (AUD-1). */
export type AuditAction = "INSERT" | "UPDATE" | "DELETE";

/** Mirrors `AuditHistoryController.HistoryEntryView` (AUD-4). */
export interface AuditHistoryEntry {
  id: string;
  action: AuditAction;
  actor: string;
  occurredAt: string;
  oldValues: Record<string, unknown>;
  newValues: Record<string, unknown>;
}

export function getDocumentHistory(entityType: string, entityId: string, companyId: string): Promise<AuditHistoryEntry[]> {
  return apiFetch<AuditHistoryEntry[]>(
    `/audit/entities/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}/history`,
    { query: { companyId } },
  );
}

/** Mirrors `AuditLogSearchController.AuditLogEntryView` (AUD-2). */
export interface AuditLogEntry {
  id: string;
  entityType: string;
  entityId: string;
  action: AuditAction;
  actor: string;
  occurredAt: string;
  oldValues: Record<string, unknown>;
  newValues: Record<string, unknown>;
}

/** Mirrors `AuditLogSearchController.AuditLogPageView`. */
export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  page: number;
  size: number;
}

export interface AuditLogSearchParams {
  companyId: string;
  entityType?: string;
  actor?: string;
  action?: AuditAction;
  /** ISO-8601 instants, inclusive on both ends (matches `AuditLogRepository.search`). */
  from?: string;
  through?: string;
  page?: number;
  size?: number;
}

export function searchAuditLog(params: AuditLogSearchParams): Promise<AuditLogPage> {
  const { companyId, entityType, actor, action, from, through, page, size } = params;
  return apiFetch<AuditLogPage>("/audit/log", {
    query: {
      companyId,
      entityType,
      actor,
      action,
      from,
      through,
      page: page === undefined ? undefined : String(page),
      size: size === undefined ? undefined : String(size),
    },
  });
}

/** Mirrors `AuditArchiveStatusService.ArchiveStatus` (AUD-5). */
export interface AuditArchiveStatus {
  archivalEnabled: boolean;
  archivedThrough: string | null;
  lastObjectKey: string | null;
  coldStorageAfterYears: number;
  minimumRetentionYears: number;
}

export function getAuditArchiveStatus(companyId: string): Promise<AuditArchiveStatus> {
  return apiFetch<AuditArchiveStatus>("/audit/archive/status", { query: { companyId } });
}
