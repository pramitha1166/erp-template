"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import {
  CHECKLIST_ITEM_LABELS,
  getChecklist,
  setChecklistItemCompleted,
  type ChecklistItemKey,
} from "@/lib/api/admin-api";

export interface SetupChecklistProps {
  tenantId: string;
}

/** F0.11.3 / ADM-4: post-onboarding guided checklist, with completion tracking. */
export function SetupChecklist({ tenantId }: SetupChecklistProps) {
  const queryClient = useQueryClient();
  const queryKey = ["setup-checklist", tenantId];

  const { data: items, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () => getChecklist(tenantId),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ itemKey, completed }: { itemKey: ChecklistItemKey; completed: boolean }) =>
      setChecklistItemCompleted(tenantId, itemKey, completed),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const completedCount = items?.filter((item) => item.completed).length ?? 0;
  const totalCount = items?.length ?? 0;

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold">Getting set up</h2>
        {items && items.length > 0 && (
          <span className="text-xs text-muted-foreground">
            {completedCount} of {totalCount} complete
          </span>
        )}
      </div>

      {isError && (
        <p role="alert" className="text-sm text-destructive">
          {error instanceof ApiError ? error.message : "Could not load the setup checklist."}
        </p>
      )}
      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

      {items && items.length > 0 && (
        <ul className="flex flex-col gap-2">
          {items.map((item) => (
            <li
              key={item.itemKey}
              className="flex items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
            >
              <span className={item.completed ? "text-muted-foreground line-through" : undefined}>
                {CHECKLIST_ITEM_LABELS[item.itemKey]}
              </span>
              <Button
                type="button"
                variant={item.completed ? "ghost" : "outline"}
                size="sm"
                disabled={toggleMutation.isPending}
                onClick={() =>
                  toggleMutation.mutate({ itemKey: item.itemKey, completed: !item.completed })
                }
              >
                {item.completed ? "Mark incomplete" : "Mark complete"}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
