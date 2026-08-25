"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { AuditArchiveStatusIndicator } from "@/components/audit/audit-archive-status-indicator";
import { RequireCompany } from "@/components/admin/require-company";
import { DocumentTable, type DocumentTableColumn } from "@/components/documents/document-table";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/lib/api/http";
import { searchAuditLog, type AuditAction, type AuditLogEntry } from "@/lib/api/audit-api";

const PAGE_SIZE = 20;

const ACTION_OPTIONS: { value: AuditAction | "ALL"; label: string }[] = [
  { value: "ALL", label: "All actions" },
  { value: "INSERT", label: "Created" },
  { value: "UPDATE", label: "Updated" },
  { value: "DELETE", label: "Deleted" },
];

const columns: DocumentTableColumn<AuditLogEntry>[] = [
  { id: "occurredAt", header: "When", cell: (row) => new Date(row.occurredAt).toLocaleString() },
  { id: "entityType", header: "Entity", cell: (row) => row.entityType },
  { id: "entityId", header: "Record", cell: (row) => row.entityId },
  { id: "action", header: "Action", cell: (row) => <Badge variant="outline">{row.action}</Badge> },
  { id: "actor", header: "User", cell: (row) => row.actor },
];

function toIsoInstant(dateInputValue: string): string | undefined {
  if (!dateInputValue) {
    return undefined;
  }
  return new Date(dateInputValue).toISOString();
}

/** F0.3.2 / AUD-2: admin-only browse/search across every entity's audit trail. */
function AuditLogBrowser({ companyId }: { companyId: string }) {
  const [entityType, setEntityType] = useState("");
  const [actor, setActor] = useState("");
  const [action, setAction] = useState<AuditAction | "ALL">("ALL");
  const [from, setFrom] = useState("");
  const [through, setThrough] = useState("");
  const [page, setPage] = useState(0);

  const {
    data,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["audit-log", companyId, entityType, actor, action, from, through, page],
    queryFn: () =>
      searchAuditLog({
        companyId,
        entityType: entityType || undefined,
        actor: actor || undefined,
        action: action === "ALL" ? undefined : action,
        from: toIsoInstant(from),
        through: toIsoInstant(through),
        page,
        size: PAGE_SIZE,
      }),
  });

  return (
    <div className="flex flex-col gap-4">
      <AuditArchiveStatusIndicator companyId={companyId} />

      <div className="flex flex-wrap items-end gap-3 rounded-lg border p-4">
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-filter-entity-type">Entity type</Label>
          <Input
            id="audit-filter-entity-type"
            placeholder="sales.invoice"
            value={entityType}
            onChange={(event) => {
              setEntityType(event.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-filter-actor">User</Label>
          <Input
            id="audit-filter-actor"
            placeholder="user@example.com"
            value={actor}
            onChange={(event) => {
              setActor(event.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-filter-action">Action</Label>
          <Select
            id="audit-filter-action"
            value={action}
            onChange={(event) => {
              setAction(event.target.value as AuditAction | "ALL");
              setPage(0);
            }}
          >
            {ACTION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-filter-from">From</Label>
          <Input
            id="audit-filter-from"
            type="date"
            value={from}
            onChange={(event) => {
              setFrom(event.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-filter-through">Through</Label>
          <Input
            id="audit-filter-through"
            type="date"
            value={through}
            onChange={(event) => {
              setThrough(event.target.value);
              setPage(0);
            }}
          />
        </div>
      </div>

      {isError && (
        <p role="alert" className="text-sm text-destructive">
          {error instanceof ApiError ? error.message : "Could not load the audit log."}
        </p>
      )}

      <DocumentTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(row) => row.id}
        totalCount={data?.totalElements ?? 0}
        page={page}
        pageSize={PAGE_SIZE}
        onPageChange={setPage}
        isLoading={isLoading}
        emptyMessage="No audit entries match these filters."
      />
    </div>
  );
}

export default function AuditLogPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Audit log</h1>
        <p className="text-sm text-muted-foreground">
          Every insert, update, and delete captured across the system (AUD-1). Filter by entity, user, action, or date
          range.
        </p>
      </div>
      <RequireCompany>{(companyId) => <AuditLogBrowser companyId={companyId} />}</RequireCompany>
    </div>
  );
}
