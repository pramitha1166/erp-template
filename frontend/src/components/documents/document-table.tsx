"use client";

import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import type { ReactNode } from "react";

import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { DOC_STATUS_LABELS, DOC_STATUSES, type DocStatus } from "@/lib/documents/status";

export interface DocumentTableColumn<T> {
  id: string;
  header: string;
  cell: (row: T) => ReactNode;
  sortable?: boolean;
  className?: string;
}

export interface DocumentTableSort {
  columnId: string;
  direction: "asc" | "desc";
}

export interface DocumentTableProps<T> {
  columns: DocumentTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  /** Total rows on the server across all pages, for the "X–Y of Z" caption. */
  totalCount: number;
  /** Zero-based current page. */
  page: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  statusFilter?: DocStatus | "ALL";
  onStatusFilterChange?: (status: DocStatus | "ALL") => void;
  sort?: DocumentTableSort;
  onSortChange?: (sort: DocumentTableSort) => void;
  isLoading?: boolean;
  emptyMessage?: string;
}

/**
 * F0.1.3 / NFR-P2: generic, server-paginated document list. The table
 * itself never fetches — it renders one page of `rows` and reports page,
 * status-filter, and sort changes upward, so every module wires it to its
 * own query without re-implementing pagination or sort-header UI.
 */
export function DocumentTable<T>({
  columns,
  rows,
  rowKey,
  totalCount,
  page,
  pageSize,
  onPageChange,
  statusFilter,
  onStatusFilterChange,
  sort,
  onSortChange,
  isLoading = false,
  emptyMessage = "No documents found.",
}: DocumentTableProps<T>) {
  const pageCount = Math.max(1, Math.ceil(totalCount / pageSize));
  const rangeStart = totalCount === 0 ? 0 : page * pageSize + 1;
  const rangeEnd = Math.min(totalCount, (page + 1) * pageSize);

  function toggleSort(columnId: string) {
    if (!onSortChange) return;
    const direction: DocumentTableSort["direction"] =
      sort?.columnId === columnId && sort.direction === "asc" ? "desc" : "asc";
    onSortChange({ columnId, direction });
  }

  return (
    <div className="flex flex-col gap-3">
      {onStatusFilterChange && (
        <div className="flex items-center gap-2">
          <label htmlFor="document-table-status-filter" className="text-sm text-muted-foreground">
            Status
          </label>
          <select
            id="document-table-status-filter"
            className="h-9 rounded-md border border-input bg-transparent px-2 text-sm shadow-xs"
            value={statusFilter ?? "ALL"}
            onChange={(event) =>
              onStatusFilterChange(event.target.value as DocStatus | "ALL")
            }
          >
            <option value="ALL">All statuses</option>
            {DOC_STATUSES.map((status) => (
              <option key={status} value={status}>
                {DOC_STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column.id} className={column.className}>
                  {column.sortable ? (
                    <button
                      type="button"
                      className="flex items-center gap-1 font-medium hover:text-foreground"
                      onClick={() => toggleSort(column.id)}
                    >
                      {column.header}
                      {sort?.columnId === column.id ? (
                        sort.direction === "asc" ? (
                          <ArrowUp className="size-3.5" aria-hidden="true" />
                        ) : (
                          <ArrowDown className="size-3.5" aria-hidden="true" />
                        )
                      ) : (
                        <ArrowUpDown className="size-3.5 opacity-40" aria-hidden="true" />
                      )}
                    </button>
                  ) : (
                    column.header
                  )}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="text-center text-muted-foreground">
                  {emptyMessage}
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow key={rowKey(row)}>
                  {columns.map((column) => (
                    <TableCell key={column.id} className={column.className}>
                      {column.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <span>
          {totalCount === 0
            ? "0 results"
            : `${rangeStart}–${rangeEnd} of ${totalCount}`}
        </span>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
          >
            Previous
          </Button>
          <span className={cn("tabular-nums")}>
            Page {page + 1} of {pageCount}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page + 1 >= pageCount}
            onClick={() => onPageChange(page + 1)}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}
