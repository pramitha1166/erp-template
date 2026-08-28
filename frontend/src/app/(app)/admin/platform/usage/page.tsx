"use client";

import { useQuery } from "@tanstack/react-query";

import { PlatformAdminNav } from "@/components/admin/platform-admin-nav";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/api/http";
import { getPlatformUsage } from "@/lib/api/admin-api";

/** F0.11.1 / ADM-9: cross-brand tenant/usage rollup plus basic system health. */
export default function PlatformUsagePage() {
  const { data: usage, isLoading, isError, error } = useQuery({
    queryKey: ["platform-usage"],
    queryFn: getPlatformUsage,
    refetchInterval: 30_000,
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Platform Admin Console</h1>
        <p className="text-sm text-muted-foreground">
          Tenant count, active users, and system health, rolled up by Brand.
        </p>
      </div>
      <PlatformAdminNav />

      {isError && (
        <p role="alert" className="text-sm text-destructive">
          {error instanceof ApiError ? error.message : "Could not load platform usage."}
        </p>
      )}
      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}

      {usage && (
        <>
          <div className="flex flex-wrap gap-4">
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Total tenants</p>
              <p className="text-2xl font-semibold tabular-nums">{usage.totalTenants}</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Total active users</p>
              <p className="text-2xl font-semibold tabular-nums">{usage.totalActiveUsers}</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">System health</p>
              <Badge
                className="mt-1"
                variant={usage.systemHealth === "UP" ? "outline" : "destructive"}
              >
                {usage.systemHealth}
              </Badge>
            </div>
          </div>

          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Brand</TableHead>
                  <TableHead>Tenants</TableHead>
                  <TableHead>Active tenants</TableHead>
                  <TableHead>Active users</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {usage.byBrand.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      No brands yet.
                    </TableCell>
                  </TableRow>
                ) : (
                  usage.byBrand.map((brand) => (
                    <TableRow key={brand.brandId}>
                      <TableCell>{brand.brandName}</TableCell>
                      <TableCell className="tabular-nums">{brand.tenantCount}</TableCell>
                      <TableCell className="tabular-nums">{brand.activeTenantCount}</TableCell>
                      <TableCell className="tabular-nums">{brand.activeUserCount}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </>
      )}
    </div>
  );
}
