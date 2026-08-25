"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createRole, type RoleView } from "@/lib/api/iam-api";
import { ApiError } from "@/lib/api/http";
import { RequireCompany } from "@/components/admin/require-company";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const createRoleSchema = z.object({
  name: z.string().min(1, "Name is required"),
  description: z.string().optional(),
});

type CreateRoleValues = z.infer<typeof createRoleSchema>;

function RoleList({ companyId }: { companyId: string }) {
  const queryClient = useQueryClient();
  const queryKey = ["roles", companyId];

  const { data: roles } = useQuery<RoleView[]>({
    queryKey,
    queryFn: () => Promise.resolve([]),
    initialData: [],
    staleTime: Infinity,
  });

  const form = useForm<CreateRoleValues>({
    resolver: zodResolver(createRoleSchema),
    defaultValues: { name: "", description: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: CreateRoleValues) => createRole(companyId, values.name, values.description || undefined),
    onSuccess: (role) => {
      queryClient.setQueryData<RoleView[]>(queryKey, (existing = []) => [...existing, role]);
      form.reset();
    },
  });

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Create a role</h2>
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="name" label="Name" />
            </div>
            <div className="flex-1">
              <ValidatedTextField control={form.control} name="description" label="Description" />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Creating…" : "Create role"}
            </Button>
          </form>
        </Form>
        {createMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {createMutation.error instanceof ApiError ? createMutation.error.message : "Could not create the role."}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Roles created this session</h2>
        <p className="text-xs text-muted-foreground">
          There is no role directory endpoint yet, so this list only shows roles created since you opened this
          screen — reload and previously created roles won&apos;t reappear here (their permissions are still live).
        </p>
        {roles.length === 0 ? (
          <p className="text-sm text-muted-foreground">No roles created yet.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {roles.map((role) => (
              <li key={role.id}>
                <Link
                  href={`/admin/roles/${role.id}`}
                  className="block rounded-md border px-3 py-2 text-sm hover:bg-accent"
                >
                  <span className="font-medium">{role.name}</span>
                  {role.description && (
                    <span className="ml-2 text-muted-foreground">{role.description}</span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

/** F0.2.5 / IAM-3: role administration entry point — create a role, then manage its permissions. */
export default function RolesAdminPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Roles &amp; permissions</h1>
        <p className="text-sm text-muted-foreground">
          Permissions are <code className="text-xs">module:entity:action</code> triples, e.g.{" "}
          <code className="text-xs">finance:journal-entry:submit</code>.
        </p>
      </div>
      <RequireCompany>{(companyId) => <RoleList companyId={companyId} />}</RequireCompany>
    </div>
  );
}
