"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  type CompanyView,
  createCompany,
  disableCompany,
  enableCompany,
  listCompanies,
  updateCompany,
} from "@/lib/api/masterdata-company-api";
import { useTenantStore } from "@/stores/tenant-store";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const editSchema = z.object({
  legalName: z.string().min(1, "Legal name is required"),
  address: z.string().optional(),
  logoUrl: z.string().optional(),
});
type EditFormValues = z.infer<typeof editSchema>;

const createSchema = z.object({
  legalName: z.string().min(1, "Legal name is required"),
  registrationNo: z.string().optional(),
  vatNo: z.string().optional(),
  address: z.string().optional(),
  baseCurrency: z
    .string()
    .length(3, "Use a 3-letter ISO currency code, e.g. LKR")
    .regex(/^[A-Z]+$/, "Use upper-case letters"),
  fiscalYearStartMonth: z
    .string()
    .regex(/^\d+$/, "Must be a whole number")
    .refine((value) => Number(value) >= 1 && Number(value) <= 12, "Must be between 1 and 12"),
});
type CreateFormValues = z.infer<typeof createSchema>;

const emptyCreateValues: CreateFormValues = {
  legalName: "",
  registrationNo: "",
  vatNo: "",
  address: "",
  baseCurrency: "LKR",
  fiscalYearStartMonth: "1",
};

/**
 * F0.6.1 / F0.6.9 / MDM-1 / MDM-2: lists every company in the tenant, edits the amendable fields of one, adds a
 * further company (MDM-2 — a tenant may hold several), and disables/enables. This is also what actually populates
 * `useTenantStore`'s `availableCompanies` — `CompanySwitcher` previously had nothing to switch between.
 */
export function CompanyPanel() {
  const queryClient = useQueryClient();
  const queryKey = ["masterdata", "companies"];
  const activeCompany = useTenantStore((state) => state.activeCompany);
  const setActiveCompany = useTenantStore((state) => state.setActiveCompany);
  const [editingCompany, setEditingCompany] = useState<CompanyView | null>(null);
  const [addingCompany, setAddingCompany] = useState(false);

  // Same React Query cache key `CompanySwitcher` uses to keep `useTenantStore`'s `availableCompanies` in sync —
  // sharing the key means visiting this screen doesn't trigger a second network round trip.
  const {
    data: companies,
    isLoading,
    isError,
  } = useQuery({ queryKey, queryFn: listCompanies });

  const editForm = useForm<EditFormValues>({ resolver: zodResolver(editSchema), defaultValues: { legalName: "" } });
  const createForm = useForm<CreateFormValues>({ resolver: zodResolver(createSchema), defaultValues: emptyCreateValues });

  const editMutation = useMutation({
    mutationFn: (values: EditFormValues) => updateCompany(editingCompany!.id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setEditingCompany(null);
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: CreateFormValues) =>
      createCompany(activeCompany!.id, { ...values, fiscalYearStartMonth: Number(values.fiscalYearStartMonth) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      createForm.reset(emptyCreateValues);
      setAddingCompany(false);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: CompanyView) => (target.disabled ? enableCompany(target.id) : disableCompany(target.id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEditing(company: CompanyView) {
    setEditingCompany(company);
    editForm.reset({ legalName: company.legalName, address: company.address ?? "", logoUrl: company.logoUrl ?? "" });
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Companies</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading companies…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load companies.
          </p>
        )}
        {companies && companies.length === 0 && <p className="text-sm text-muted-foreground">No companies yet.</p>}
        {companies && companies.length > 0 && (
          <ul className="flex flex-col gap-2">
            {companies.map((company) => (
              <li
                key={company.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="flex items-center gap-1.5 font-medium">
                    {company.id === activeCompany?.id && <Check className="size-4 text-primary" aria-hidden="true" />}
                    {company.legalName}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {company.baseCurrency} · fiscal year starts month {company.fiscalYearStartMonth}
                    {company.registrationNo ? ` · ${company.registrationNo}` : ""}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={company.disabled} />
                  {company.id !== activeCompany?.id && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setActiveCompany({ id: company.id, name: company.legalName })}
                    >
                      Make active
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => startEditing(company)}>
                    Edit
                  </Button>
                  <DisableToggleButton
                    disabled={company.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(company)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {editingCompany && (
        <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
          <h3 className="text-sm font-semibold">Edit {editingCompany.legalName}</h3>
          <Form {...editForm}>
            <form
              onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
              className="flex flex-col gap-3"
            >
              <ValidatedTextField control={editForm.control} name="legalName" label="Legal name" />
              <ValidatedTextField control={editForm.control} name="address" label="Address" />
              <ValidatedTextField control={editForm.control} name="logoUrl" label="Logo URL" />

              {editMutation.isError && (
                <p role="alert" className="text-sm text-destructive">
                  {editMutation.error instanceof ApiError ? editMutation.error.message : "Could not save this company."}
                </p>
              )}

              <div className="flex gap-2">
                <Button type="submit" disabled={editMutation.isPending} className="self-start">
                  {editMutation.isPending ? "Saving…" : "Save changes"}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setEditingCompany(null)}>
                  Cancel
                </Button>
              </div>
            </form>
          </Form>
        </section>
      )}

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        {!addingCompany ? (
          <Button
            variant="outline"
            className="self-start"
            disabled={!activeCompany}
            onClick={() => setAddingCompany(true)}
          >
            Add another company
          </Button>
        ) : (
          <>
            <h3 className="text-sm font-semibold">Add another company</h3>
            <Form {...createForm}>
              <form
                onSubmit={createForm.handleSubmit((values) => createMutation.mutate(values))}
                className="flex flex-col gap-3"
              >
                <ValidatedTextField control={createForm.control} name="legalName" label="Legal name" />
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <ValidatedTextField control={createForm.control} name="registrationNo" label="Registration no." />
                  <ValidatedTextField control={createForm.control} name="vatNo" label="VAT no." />
                </div>
                <ValidatedTextField control={createForm.control} name="address" label="Address" />
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <ValidatedTextField control={createForm.control} name="baseCurrency" label="Base currency" />
                  <ValidatedTextField
                    control={createForm.control}
                    name="fiscalYearStartMonth"
                    label="Fiscal-year start month (1-12)"
                    type="number"
                  />
                </div>

                {createMutation.isError && (
                  <p role="alert" className="text-sm text-destructive">
                    {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create the company."}
                  </p>
                )}

                <div className="flex gap-2">
                  <Button type="submit" disabled={createMutation.isPending} className="self-start">
                    {createMutation.isPending ? "Creating…" : "Create company"}
                  </Button>
                  <Button type="button" variant="ghost" onClick={() => setAddingCompany(false)}>
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
