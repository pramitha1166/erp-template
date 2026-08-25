"use client";

import { useQuery } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { getDocumentHistory, type AuditHistoryEntry } from "@/lib/api/audit-api";

export interface DocumentHistoryPanelProps {
  entityType: string;
  entityId: string;
  companyId: string;
}

const ACTION_LABELS: Record<AuditHistoryEntry["action"], string> = {
  INSERT: "Created",
  UPDATE: "Updated",
  DELETE: "Deleted",
};

function formatValue(value: unknown): string {
  if (value === undefined || value === null) {
    return "—";
  }
  return typeof value === "object" ? JSON.stringify(value) : String(value);
}

/** AUD-2's old/new JSONB diff, narrowed to the fields that actually changed on this entry. */
function changedFields(entry: AuditHistoryEntry): string[] {
  const keys = new Set([...Object.keys(entry.oldValues), ...Object.keys(entry.newValues)]);
  return [...keys]
    .filter((key) => JSON.stringify(entry.oldValues[key]) !== JSON.stringify(entry.newValues[key]))
    .sort();
}

/**
 * F0.3.1 / AUD-4: read-only version-history timeline with a field-level
 * diff per entry, meant to embed on a document detail view once one exists
 * (see `AuditHistoryController`'s Javadoc — this is that endpoint's UI).
 */
export function DocumentHistoryPanel({ entityType, entityId, companyId }: DocumentHistoryPanelProps) {
  const {
    data: entries,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["document-history", entityType, entityId, companyId],
    queryFn: () => getDocumentHistory(entityType, entityId, companyId),
  });

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading history…</p>;
  }

  if (isError) {
    return (
      <p role="alert" className="text-sm text-destructive">
        Could not load version history.
      </p>
    );
  }

  if (!entries || entries.length === 0) {
    return <p className="text-sm text-muted-foreground">No version history yet.</p>;
  }

  return (
    <ol className="flex flex-col gap-3">
      {entries.map((entry) => {
        const fields = changedFields(entry);
        return (
          <li key={entry.id} className="rounded-md border p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="flex items-center gap-2 text-sm font-medium">
                <Badge variant="outline">{ACTION_LABELS[entry.action]}</Badge>
                {entry.actor}
              </span>
              <span className="text-xs text-muted-foreground">{new Date(entry.occurredAt).toLocaleString()}</span>
            </div>
            {fields.length > 0 && (
              <table className="mt-2 w-full text-xs">
                <tbody>
                  {fields.map((field) => (
                    <tr key={field}>
                      <td className="py-1 pr-2 align-top font-medium text-muted-foreground">{field}</td>
                      <td className="py-1 pr-2 align-top text-destructive line-through">
                        {formatValue(entry.oldValues[field])}
                      </td>
                      <td className="py-1 align-top">{formatValue(entry.newValues[field])}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </li>
        );
      })}
    </ol>
  );
}
