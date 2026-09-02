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
import {
  PARTNER_TYPE_OPTIONS,
  type BusinessPartnerType,
  type PartnerView,
  createPartner,
  disablePartner,
  enablePartner,
  listPartners,
} from "@/lib/api/masterdata-partner-api";
import { BusinessPartnerDetailDialog } from "./business-partner-detail-dialog";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const createSchema = z.object({
  partnerType: z.enum(["CUSTOMER", "SUPPLIER", "BOTH"]),
  code: z.string().min(1, "Code is required"),
  name: z.string().min(1, "Name is required"),
});
type CreateFormValues = z.infer<typeof createSchema>;

const emptyCreateValues: CreateFormValues = { partnerType: "CUSTOMER", code: "", name: "" };

const FILTER_OPTIONS: { value: string; label: string }[] = [{ value: "", label: "All" }, ...PARTNER_TYPE_OPTIONS];

export interface BusinessPartnerPanelProps {
  companyId: string;
}

/** F0.6.4 / MDM-5: customer/supplier master list, plus the detail dialog for contacts, credit terms, and bank details. */
export function BusinessPartnerPanel({ companyId }: BusinessPartnerPanelProps) {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<BusinessPartnerType | "">("");
  const [managingPartner, setManagingPartner] = useState<PartnerView | null>(null);
  const queryKey = ["masterdata", "business-partners", companyId, filter];

  const {
    data: partners,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: () => listPartners(companyId, filter || undefined),
  });

  const form = useForm<CreateFormValues>({ resolver: zodResolver(createSchema), defaultValues: emptyCreateValues });

  const createMutation = useMutation({
    mutationFn: (values: CreateFormValues) => createPartner(companyId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["masterdata", "business-partners", companyId] });
      form.reset(emptyCreateValues);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: PartnerView) =>
      target.disabled ? enablePartner(target.id, companyId) : disablePartner(target.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["masterdata", "business-partners", companyId] }),
  });

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold">Customers &amp; suppliers</h2>
          <select
            aria-label="Filter by type"
            className="h-8 rounded-md border bg-transparent px-2 text-sm"
            value={filter}
            onChange={(e) => setFilter(e.target.value as BusinessPartnerType | "")}
          >
            {FILTER_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load customers/suppliers.
          </p>
        )}
        {partners && partners.length === 0 && <p className="text-sm text-muted-foreground">No records yet.</p>}
        {partners && partners.length > 0 && (
          <ul className="flex flex-col gap-2">
            {partners.map((partner) => (
              <li
                key={partner.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">
                    {partner.code} — {partner.name}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {partner.partnerType} · {partner.creditTermsDays}-day terms · credit limit {partner.creditLimit}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={partner.disabled} />
                  <Button size="sm" variant="ghost" onClick={() => setManagingPartner(partner)}>
                    Manage
                  </Button>
                  <DisableToggleButton
                    disabled={partner.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(partner)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">Add a customer/supplier</h3>
        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => createMutation.mutate(values))} className="flex flex-col gap-3">
            <ValidatedSelectField control={form.control} name="partnerType" label="Type" options={PARTNER_TYPE_OPTIONS} />
            <ValidatedTextField control={form.control} name="code" label="Code" />
            <ValidatedTextField control={form.control} name="name" label="Name" />

            {createMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create this record."}
              </p>
            )}

            <Button type="submit" disabled={createMutation.isPending} className="self-start">
              {createMutation.isPending ? "Creating…" : "Add"}
            </Button>
          </form>
        </Form>
      </section>

      {managingPartner && (
        <BusinessPartnerDetailDialog companyId={companyId} partner={managingPartner} onClose={() => setManagingPartner(null)} />
      )}
    </div>
  );
}
