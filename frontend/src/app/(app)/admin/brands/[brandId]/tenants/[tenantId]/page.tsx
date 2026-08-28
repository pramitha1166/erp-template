"use client";

import { use } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ImpersonateTenantButton } from "@/components/admin/impersonate-tenant-button";
import { TenantEntitlementsForm } from "@/components/admin/tenant-entitlements-form";
import { TenantInvitesPanel } from "@/components/admin/tenant-invites-panel";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import {
  getTenantUsage,
  listBrandTenants,
  reactivateTenant,
  suspendTenant,
} from "@/lib/api/admin-api";

interface TenantDetailPageProps {
  params: Promise<{ brandId: string; tenantId: string }>;
}

/** F0.11.4 / F0.11.5 / F0.11.6: one tenant's admin console — usage, entitlements, invites, impersonation, data requests. */
function TenantDetail({ brandId, tenantId }: { brandId: string; tenantId: string }) {
  const queryClient = useQueryClient();
  const tenantsQueryKey = ["brand-tenants", brandId];

  const { data: tenants } = useQuery({ queryKey: tenantsQueryKey, queryFn: () => listBrandTenants(brandId) });
  const tenant = tenants?.find((candidate) => candidate.id === tenantId);

  const { data: usage } = useQuery({
    queryKey: ["tenant-usage", brandId, tenantId],
    queryFn: () => getTenantUsage(brandId, tenantId),
  });

  const toggleMutation = useMutation({
    mutationFn: () =>
      tenant?.status === "ACTIVE" ? suspendTenant(brandId, tenantId) : reactivateTenant(brandId, tenantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tenantsQueryKey }),
  });

  const tenantName = tenant?.name ?? usage?.tenantName ?? tenantId;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href={`/admin/brands/${brandId}`} className="text-xs text-muted-foreground hover:underline">
          ← Brand console
        </Link>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h1 className="text-xl font-semibold">{tenantName}</h1>
          <div className="flex items-center gap-2">
            <ImpersonateTenantButton tenantId={tenantId} tenantName={tenantName} />
            {tenant && (
              <>
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
              </>
            )}
          </div>
        </div>
        {toggleMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {toggleMutation.error instanceof ApiError ? toggleMutation.error.message : "Could not update the tenant."}
          </p>
        )}
        {usage && <p className="text-sm text-muted-foreground">{usage.activeUserCount} active users</p>}
      </div>

      <TenantEntitlementsForm brandId={brandId} tenantId={tenantId} />
      <TenantInvitesPanel tenantId={tenantId} />

      <section className="flex flex-col gap-2 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Data requests</h2>
        <p className="text-sm text-muted-foreground">Export or erasure requests for this tenant (PDPA).</p>
        <Button asChild variant="outline" className="w-fit">
          <Link href={`/admin/brands/${brandId}/tenants/${tenantId}/data-requests`}>View data requests</Link>
        </Button>
      </section>
    </div>
  );
}

export default function TenantDetailPage({ params }: TenantDetailPageProps) {
  const { brandId, tenantId } = use(params);
  return <TenantDetail brandId={brandId} tenantId={tenantId} />;
}
