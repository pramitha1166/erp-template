"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { grantPermission, listRolePermissions, revokePermission } from "@/lib/api/iam-api";
import { ApiError } from "@/lib/api/http";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const SEGMENT = /^[a-z][a-z0-9-]*$/;
const segmentSchema = (label: string) =>
  z.string().regex(SEGMENT, `${label} must be lowercase, starting with a letter (e.g. "journal-entry")`);

const grantSchema = z.object({
  module: segmentSchema("Module"),
  entity: segmentSchema("Entity"),
  action: segmentSchema("Action"),
});

type GrantValues = z.infer<typeof grantSchema>;

export interface PermissionsPanelProps {
  roleId: string;
  companyId: string;
}

/** F0.2.5 / IAM-3: grant/revoke `module:entity:action` permissions on a role. */
export function PermissionsPanel({ roleId, companyId }: PermissionsPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["role-permissions", roleId];

  const { data: permissions, isLoading } = useQuery({
    queryKey,
    queryFn: () => listRolePermissions(roleId),
  });

  const form = useForm<GrantValues>({
    resolver: zodResolver(grantSchema),
    defaultValues: { module: "", entity: "", action: "" },
  });

  const grantMutation = useMutation({
    mutationFn: (values: GrantValues) =>
      grantPermission(roleId, companyId, `${values.module}:${values.entity}:${values.action}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset();
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (permissionCode: string) => revokePermission(roleId, companyId, permissionCode),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Permissions</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => grantMutation.mutate(values))}
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="module" label="Module" placeholder="finance" />
          </div>
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="entity" label="Entity" placeholder="journal-entry" />
          </div>
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="action" label="Action" placeholder="submit" />
          </div>
          <Button type="submit" disabled={grantMutation.isPending}>
            {grantMutation.isPending ? "Granting…" : "Grant"}
          </Button>
        </form>
      </Form>
      {grantMutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {grantMutation.error instanceof ApiError ? grantMutation.error.message : "Could not grant that permission."}
        </p>
      )}

      {isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {permissions && permissions.length === 0 && (
        <p className="text-sm text-muted-foreground">No permissions granted yet.</p>
      )}
      {permissions && permissions.length > 0 && (
        <ul className="flex flex-col gap-1">
          {permissions.map((permission) => (
            <li
              key={permission.permissionCode}
              className="flex items-center justify-between rounded-md border px-3 py-1.5 text-sm"
            >
              <code className="text-xs">{permission.permissionCode}</code>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                disabled={revokeMutation.isPending}
                onClick={() => revokeMutation.mutate(permission.permissionCode)}
              >
                Revoke
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
