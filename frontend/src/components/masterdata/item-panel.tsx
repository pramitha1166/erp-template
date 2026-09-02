"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormLabel } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  VALUATION_METHOD_OPTIONS,
  type ItemView,
  createItem,
  disableItem,
  enableItem,
  listItemGroups,
  listItems,
  updateItem,
} from "@/lib/api/masterdata-item-api";
import { listUoms } from "@/lib/api/masterdata-uom-api";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const createSchema = z.object({
  code: z.string().min(1, "Code is required"),
  name: z.string().min(1, "Name is required"),
  itemGroupId: z.string().min(1, "Item group is required"),
  stockUomId: z.string().min(1, "Stock UOM is required"),
  valuationMethod: z.enum(["FIFO", "WEIGHTED_AVERAGE", "STANDARD_COST"]),
});
type CreateFormValues = z.infer<typeof createSchema>;
const emptyCreateValues: CreateFormValues = {
  code: "",
  name: "",
  itemGroupId: "",
  stockUomId: "",
  valuationMethod: "FIFO",
};

const editSchema = z.object({
  name: z.string().min(1, "Name is required"),
  itemGroupId: z.string().min(1, "Item group is required"),
  purchaseUomId: z.string(),
  valuationMethod: z.enum(["FIFO", "WEIGHTED_AVERAGE", "STANDARD_COST"]),
  reorderLevel: z.string().regex(/^\d*\.?\d*$/, "Must be a non-negative number"),
  batchTracked: z.boolean(),
  serialTracked: z.boolean(),
  taxCategoryCode: z.string().optional(),
  hsCode: z.string().optional(),
});
type EditFormValues = z.infer<typeof editSchema>;

function toEditValues(item: ItemView): EditFormValues {
  return {
    name: item.name,
    itemGroupId: item.itemGroupId,
    purchaseUomId: item.purchaseUomId ?? "",
    valuationMethod: item.valuationMethod,
    reorderLevel: String(item.reorderLevel),
    batchTracked: item.batchTracked,
    serialTracked: item.serialTracked,
    taxCategoryCode: item.taxCategoryCode ?? "",
    hsCode: item.hsCode ?? "",
  };
}

export interface ItemPanelProps {
  companyId: string;
}

/** F0.6.5 / MDM-6 / MDM-7: item master list + detail form. */
export function ItemPanel({ companyId }: ItemPanelProps) {
  const queryClient = useQueryClient();
  const itemsQueryKey = ["masterdata", "items", companyId];
  const [editingItem, setEditingItem] = useState<ItemView | null>(null);
  const [creating, setCreating] = useState(false);

  const { data: items, isLoading, isError } = useQuery({ queryKey: itemsQueryKey, queryFn: () => listItems(companyId) });
  const { data: itemGroups } = useQuery({
    queryKey: ["masterdata", "item-groups", companyId],
    queryFn: () => listItemGroups(companyId),
  });
  const { data: uoms } = useQuery({ queryKey: ["masterdata", "uoms"], queryFn: listUoms });

  const itemGroupOptions = (itemGroups ?? []).map((g) => ({ value: g.id, label: `${g.code} — ${g.name}` }));
  const uomOptions = (uoms ?? []).filter((u) => !u.disabled).map((u) => ({ value: u.id, label: `${u.code} — ${u.name}` }));
  const optionalUomOptions = [{ value: "", label: "(same as stock UOM)" }, ...uomOptions];

  const createForm = useForm<CreateFormValues>({ resolver: zodResolver(createSchema), defaultValues: emptyCreateValues });
  const editForm = useForm<EditFormValues>({ resolver: zodResolver(editSchema) });

  const createMutation = useMutation({
    mutationFn: (values: CreateFormValues) => createItem(companyId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: itemsQueryKey });
      createForm.reset(emptyCreateValues);
      setCreating(false);
    },
  });

  const editMutation = useMutation({
    mutationFn: (values: EditFormValues) =>
      updateItem(editingItem!.id, companyId, {
        name: values.name,
        itemGroupId: values.itemGroupId,
        purchaseUomId: values.purchaseUomId || undefined,
        valuationMethod: values.valuationMethod,
        reorderLevel: Number(values.reorderLevel || "0"),
        batchTracked: values.batchTracked,
        serialTracked: values.serialTracked,
        taxCategoryCode: values.taxCategoryCode || undefined,
        hsCode: values.hsCode || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: itemsQueryKey });
      setEditingItem(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: ItemView) => (target.disabled ? enableItem(target.id, companyId) : disableItem(target.id, companyId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: itemsQueryKey }),
  });

  function startEditing(item: ItemView) {
    setEditingItem(item);
    editForm.reset(toEditValues(item));
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Items</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading items…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load items.
          </p>
        )}
        {items && items.length === 0 && <p className="text-sm text-muted-foreground">No items yet.</p>}
        {items && items.length > 0 && (
          <ul className="flex flex-col gap-2">
            {items.map((item) => (
              <li
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">
                    {item.code} — {item.name}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {item.valuationMethod} · reorder at {item.reorderLevel}
                    {item.batchTracked ? " · batch-tracked" : ""}
                    {item.serialTracked ? " · serial-tracked" : ""}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={item.disabled} />
                  <Button size="sm" variant="ghost" onClick={() => startEditing(item)}>
                    Edit
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

      {editingItem && (
        <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
          <h3 className="text-sm font-semibold">Edit {editingItem.code}</h3>
          <Form {...editForm}>
            <form onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))} className="flex flex-col gap-3">
              <ValidatedTextField control={editForm.control} name="name" label="Name" />
              <ValidatedSelectField control={editForm.control} name="itemGroupId" label="Item group" options={itemGroupOptions} />
              <ValidatedSelectField
                control={editForm.control}
                name="purchaseUomId"
                label="Purchase UOM"
                options={optionalUomOptions}
              />
              <ValidatedSelectField
                control={editForm.control}
                name="valuationMethod"
                label="Valuation method"
                options={VALUATION_METHOD_OPTIONS}
              />
              <ValidatedTextField control={editForm.control} name="reorderLevel" label="Reorder level" />
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <ValidatedTextField control={editForm.control} name="taxCategoryCode" label="Tax category" />
                <ValidatedTextField control={editForm.control} name="hsCode" label="HS code" />
              </div>
              <FormField
                control={editForm.control}
                name="batchTracked"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2 space-y-0">
                    <FormControl>
                      <input
                        type="checkbox"
                        className="size-4"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    </FormControl>
                    <FormLabel className="!mt-0">Batch-tracked</FormLabel>
                  </FormItem>
                )}
              />
              <FormField
                control={editForm.control}
                name="serialTracked"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2 space-y-0">
                    <FormControl>
                      <input
                        type="checkbox"
                        className="size-4"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    </FormControl>
                    <FormLabel className="!mt-0">Serial-tracked</FormLabel>
                  </FormItem>
                )}
              />

              {editMutation.isError && (
                <p role="alert" className="text-sm text-destructive">
                  {editMutation.error instanceof ApiError ? editMutation.error.message : "Could not save this item."}
                </p>
              )}

              <div className="flex gap-2">
                <Button type="submit" disabled={editMutation.isPending} className="self-start">
                  {editMutation.isPending ? "Saving…" : "Save changes"}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setEditingItem(null)}>
                  Cancel
                </Button>
              </div>
            </form>
          </Form>
        </section>
      )}

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        {!creating ? (
          <Button variant="outline" className="self-start" onClick={() => setCreating(true)}>
            Add an item
          </Button>
        ) : (
          <>
            <h3 className="text-sm font-semibold">Add an item</h3>
            <Form {...createForm}>
              <form
                onSubmit={createForm.handleSubmit((values) => createMutation.mutate(values))}
                className="flex flex-col gap-3"
              >
                <ValidatedTextField control={createForm.control} name="code" label="Code" />
                <ValidatedTextField control={createForm.control} name="name" label="Name" />
                <ValidatedSelectField
                  control={createForm.control}
                  name="itemGroupId"
                  label="Item group"
                  options={itemGroupOptions}
                />
                <ValidatedSelectField control={createForm.control} name="stockUomId" label="Stock UOM" options={uomOptions} />
                <ValidatedSelectField
                  control={createForm.control}
                  name="valuationMethod"
                  label="Valuation method"
                  options={VALUATION_METHOD_OPTIONS}
                />

                {createMutation.isError && (
                  <p role="alert" className="text-sm text-destructive">
                    {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create this item."}
                  </p>
                )}

                <div className="flex gap-2">
                  <Button type="submit" disabled={createMutation.isPending} className="self-start">
                    {createMutation.isPending ? "Creating…" : "Create item"}
                  </Button>
                  <Button type="button" variant="ghost" onClick={() => setCreating(false)}>
                    Cancel
                  </Button>
                </div>
              </form>
            </Form>
          </>
        )}
      </section>
    </div>
  );
}
