"use client";

import { use } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import {
  getBrand,
  getTenantUsage,
  listBrandTenants,
  reactivateTenant,
  suspendTenant,
  type TenantView,
} from "@/lib/api/admin-api";

interface BrandConsolePageProps {
  params: Promise<{ brandId: string }>;
}

function TenantUsageBadge({ brandId, tenantId }: { brandId: string; tenantId: string }) {
  const { data: usage } = useQuery({
    queryKey: ["tenant-usage", brandId, tenantId],
    queryFn: () => getTenantUsage(brandId, tenantId),
  });

  if (!usage) {
    return null;
  }
  return (
    <span className="text-xs text-muted-foreground">{usage.activeUserCount} active users</span>
  );
}

function TenantRow({ brandId, tenant }: { brandId: string; tenant: TenantView }) {
  const queryClient = useQueryClient();
  const queryKey = ["brand-tenants", brandId];

  const toggleMutation = useMutation({
    mutationFn: () =>
      tenant.status === "ACTIVE" ? suspendTenant(brandId, tenant.id) : reactivateTenant(brandId, tenant.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm">
      <div className="flex flex-col">
        <Link href={`/admin/brands/${brandId}/tenants/${tenant.id}`} className="font-medium hover:underline">
          {tenant.name}
        </Link>
        <TenantUsageBadge brandId={brandId} tenantId={tenant.id} />
      </div>
      <div className="flex items-center gap-2">
        <Badge variant={tenant.status === "ACTIVE" ? "outline" : "destructive"}>{tenant.status}</Badge>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={toggleMutation.isPending}
          onClick={() => toggleMutation.mutate()}
        >
          {tenant.status === "ACTIVE" ? "Suspend" : "Reactivate"}
        </Button>
      </div>
    </li>
  );
}

/** F0.11.4 / ADM-5, BRD-14, BRD-15: brand-partner console — tenants within this Brand only. */
function BrandConsole({ brandId }: { brandId: string }) {
  const { data: brand } = useQuery({
    queryKey: ["brand", brandId],
    queryFn: () => getBrand(brandId),
  });

  const {
    data: tenants,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["brand-tenants", brandId],
    queryFn: () => listBrandTenants(brandId),
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-col gap-1">
          <h1 className="text-xl font-semibold">{brand?.name ?? "Brand console"}</h1>
          <p className="text-sm text-muted-foreground">
            Tenants onboarded under this Brand. Suspending a Brand-owned Tenant blocks new logins and
            postings but keeps its data and reports available.
          </p>
        </div>
        <Button asChild>
          <Link href={`/admin/brands/${brandId}/tenants/new`}>Onboard a tenant</Link>
        </Button>
      </div>

      {isError && (
        <p role="alert" className="text-sm text-destructive">
          {error instanceof ApiError ? error.message : "Could not load tenants."}
        </p>
      )}
      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {tenants && tenants.length === 0 && (
        <p className="text-sm text-muted-foreground">No tenants onboarded yet.</p>
      )}
      {tenants && tenants.length > 0 && (
        <ul className="flex flex-col gap-2">
          {tenants.map((tenant) => (
            <TenantRow key={tenant.id} brandId={brandId} tenant={tenant} />
          ))}
        </ul>
      )}
    </div>
  );
}

export default function BrandConsolePage({ params }: BrandConsolePageProps) {
  const { brandId } = use(params);
  return <BrandConsole brandId={brandId} />;
}
