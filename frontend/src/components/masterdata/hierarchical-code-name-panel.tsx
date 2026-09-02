"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { flattenTree, type TreeNode } from "@/lib/masterdata/tree";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

export interface HierarchicalCodeNameItem extends TreeNode {
  code: string;
  name: string;
  disabled: boolean;
}

export interface HierarchicalCodeNamePanelProps<T extends HierarchicalCodeNameItem> {
  companyId: string;
  /** React Query cache key namespace, e.g. `["masterdata", "cost-centres", companyId]`. */
  queryKey: unknown[];
  /** What to call this kind of node in copy, e.g. "cost centre" / "item group". */
  itemLabel: string;
  emptyMessage: string;
  list: (companyId: string) => Promise<T[]>;
  create: (companyId: string, request: { code: string; name: string; parentId: string | null }) => Promise<T>;
  rename: (id: string, companyId: string, name: string) => Promise<T>;
  disable: (id: string, companyId: string) => Promise<void>;
  enable: (id: string, companyId: string) => Promise<void>;
}

const formSchema = z.object({
  code: z.string().min(1, "Code is required"),
  name: z.string().min(1, "Name is required"),
  parentId: z.string(),
});

type FormValues = z.infer<typeof formSchema>;

const emptyFormValues: FormValues = { code: "", name: "", parentId: "" };

/**
 * F0.6.3 / F0.6.5 / MDM-4 / MDM-6: the hierarchical code/name/parent/disabled tree shape Cost Centres and Item
 * Groups both share byte-for-byte on the backend (`CostCentreService`, `ItemGroupService`) — one panel, one set of
 * tests, instead of two near-identical copies. Chart of Accounts carries extra fields (account type, group-vs-ledger)
 * so it gets its own panel rather than being forced through this shape.
 */
export function HierarchicalCodeNamePanel<T extends HierarchicalCodeNameItem>({
  companyId,
  queryKey,
  itemLabel,
  emptyMessage,
  list,
  create,
  rename,
  disable,
  enable,
}: HierarchicalCodeNamePanelProps<T>) {
  const queryClient = useQueryClient();
  const [editingItem, setEditingItem] = useState<T | null>(null);

  const {
    data: items,
    isLoading,
    isError,
  } = useQuery({ queryKey, queryFn: () => list(companyId) });

  const rows = items ? flattenTree(items) : [];

  const form = useForm<FormValues>({ resolver: zodResolver(formSchema), defaultValues: emptyFormValues });

  const saveMutation = useMutation({
    mutationFn: (values: FormValues) =>
      editingItem
        ? rename(editingItem.id, companyId, values.name)
        : create(companyId, { code: values.code, name: values.name, parentId: values.parentId || null }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset(emptyFormValues);
      setEditingItem(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: T) => (target.disabled ? enable(target.id, companyId) : disable(target.id, companyId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEditing(item: T) {
    setEditingItem(item);
    form.reset({ code: item.code, name: item.name, parentId: item.parentId ?? "" });
  }

  function cancelEditing() {
    setEditingItem(null);
    form.reset(emptyFormValues);
  }

  const parentOptions = [
    { value: "", label: "(no parent — top level)" },
    ...(items ?? []).map((item) => ({ value: item.id, label: `${item.code} — ${item.name}` })),
  ];

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold capitalize">{itemLabel} tree</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load {itemLabel}s.
          </p>
        )}
        {items && items.length === 0 && <p className="text-sm text-muted-foreground">{emptyMessage}</p>}
        {rows.length > 0 && (
          <ul className="flex flex-col gap-2">
            {rows.map(({ item, depth }) => (
              <li
                key={item.id}
                style={{ marginLeft: depth * 20 }}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">
                    {item.code} — {item.name}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={item.disabled} />
                  <Button size="sm" variant="ghost" onClick={() => startEditing(item)}>
                    Rename
                  </Button>
                  <DisableToggleButton
                    disabled={item.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(item)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">{editingItem ? `Rename ${editingItem.code}` : `Add a ${itemLabel}`}</h3>
        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="flex flex-col gap-3">
            {editingItem ? (
              <p className="text-sm text-muted-foreground">
                Code: <span className="font-medium text-foreground">{editingItem.code}</span>
              </p>
            ) : (
              <>
                <ValidatedTextField control={form.control} name="code" label="Code" />
                <ValidatedSelectField control={form.control} name="parentId" label="Parent" options={parentOptions} />
              </>
            )}
            <ValidatedTextField control={form.control} name="name" label="Name" />

            {saveMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {saveMutation.error instanceof ApiError ? saveMutation.error.message : `Could not save this ${itemLabel}.`}
              </p>
            )}

            <div className="flex gap-2">
              <Button type="submit" disabled={saveMutation.isPending} className="self-start">
                {saveMutation.isPending ? "Saving…" : editingItem ? "Save changes" : "Add"}
              </Button>
              {editingItem && (
                <Button type="button" variant="ghost" onClick={cancelEditing}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </Form>
      </section>
    </div>
  );
}
