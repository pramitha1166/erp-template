"use client";

import { use } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";

import type { RoleView } from "@/lib/api/iam-api";
import { RequireCompany } from "@/components/admin/require-company";
import { PermissionsPanel } from "@/components/admin/permissions-panel";
import { FieldPermissionForm } from "@/components/admin/field-permission-form";
import { RoleAssignmentPanel } from "@/components/admin/role-assignment-panel";

interface RoleDetailPageProps {
  params: Promise<{ roleId: string }>;
}

function RoleDetail({ roleId, companyId }: { roleId: string; companyId: string }) {
  const queryClient = useQueryClient();
  const role = queryClient
    .getQueryData<RoleView[]>(["roles", companyId])
    ?.find((candidate) => candidate.id === roleId);
  const roleName = role?.name ?? roleId;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href="/admin/roles" className="text-xs text-muted-foreground hover:underline">
          ← Roles
        </Link>
        <h1 className="text-xl font-semibold">{roleName}</h1>
        {role?.description && <p className="text-sm text-muted-foreground">{role.description}</p>}
      </div>
      <PermissionsPanel roleId={roleId} companyId={companyId} />
      <FieldPermissionForm roleId={roleId} companyId={companyId} />
      <RoleAssignmentPanel roleId={roleId} roleName={roleName} companyId={companyId} />
    </div>
  );
}

/** F0.2.5 / F0.2.7: manage one role's permissions, field permissions, and user assignments. */
export default function RoleDetailPage({ params }: RoleDetailPageProps) {
  const { roleId } = use(params);

  return (
    <RequireCompany>{(companyId) => <RoleDetail roleId={roleId} companyId={companyId} />}</RequireCompany>
  );
}
