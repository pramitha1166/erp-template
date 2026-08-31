"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { PlatformAdminNav } from "@/components/admin/platform-admin-nav";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import {
  createBrand,
  listBrands,
  reactivateBrand,
  suspendBrand,
  type BrandView,
} from "@/lib/api/admin-api";

const createBrandSchema = z.object({
  name: z.string().min(1, "Name is required"),
  legalName: z.string().optional(),
  supportEmail: z.union([z.literal(""), z.string().email("Enter a valid email")]).optional(),
});

type CreateBrandValues = z.infer<typeof createBrandSchema>;

function BrandRow({ brand }: { brand: BrandView }) {
  const queryClient = useQueryClient();
  const queryKey = ["brands"];

  const toggleMutation = useMutation({
    mutationFn: () =>
      brand.status === "ACTIVE" ? suspendBrand(brand.id) : reactivateBrand(brand.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm">
      <div className="flex flex-col">
        <Link href={`/admin/brands/${brand.id}`} className="font-medium hover:underline">
          {brand.name}
        </Link>
        {brand.legalName && <span className="text-xs text-muted-foreground">{brand.legalName}</span>}
      </div>
      <div className="flex items-center gap-2">
        <Badge variant={brand.status === "ACTIVE" ? "outline" : "destructive"}>{brand.status}</Badge>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={toggleMutation.isPending}
          onClick={() => toggleMutation.mutate()}
        >
          {brand.status === "ACTIVE" ? "Suspend" : "Reactivate"}
        </Button>
      </div>
    </li>
  );
}

/** F0.11.1 / ADM-1: platform-operator console — create, suspend, and reactivate Brands. */
export default function PlatformBrandsPage() {
  const queryClient = useQueryClient();
  const queryKey = ["brands"];

  const { data: brands, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: listBrands,
  });

  const form = useForm<CreateBrandValues>({
    resolver: zodResolver(createBrandSchema),
    defaultValues: { name: "", legalName: "", supportEmail: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: CreateBrandValues) =>
      createBrand(values.name, values.legalName || undefined, values.supportEmail || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Platform Admin Console</h1>
        <p className="text-sm text-muted-foreground">
          Create Brands and manage which ones can onboard Tenants. Suspending a Brand suspends every Tenant
          under it.
        </p>
      </div>
      <PlatformAdminNav />

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Create a brand</h2>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="name" label="Name" />
            </div>
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="legalName" label="Legal name" />
            </div>
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="supportEmail" label="Support email" type="email" />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Creating…" : "Create brand"}
            </Button>
          </form>
        </Form>
        {createMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create the brand."}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Brands</h2>
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            {error instanceof ApiError ? error.message : "Could not load brands."}
          </p>
        )}
        {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {brands && brands.length === 0 && <p className="text-sm text-muted-foreground">No brands yet.</p>}
        {brands && brands.length > 0 && (
          <ul className="flex flex-col gap-2">
            {brands.map((brand) => (
              <BrandRow key={brand.id} brand={brand} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
