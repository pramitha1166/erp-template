"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { assignRole, listUserRoles, unassignRole } from "@/lib/api/iam-api";
import { ApiError } from "@/lib/api/http";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const assignSchema = z.object({ userId: z.uuid("Enter the user's id (a UUID)") });
type AssignValues = z.infer<typeof assignSchema>;

export interface RoleAssignmentPanelProps {
  roleId: string;
  roleName: string;
  companyId: string;
}

/**
 * F0.2.6 / F0.2.7 / IAM-4 / IAM-7: assigns this role to a user in the
 * active company, and surfaces a Segregation-of-Duties conflict inline
 * right here rather than as a generic toast — `UserRoleAssignmentService`
 * only ever finds out about a conflict *during* the assignment call
 * itself (there's no separate "would this conflict" pre-check endpoint),
 * so the 409 response is the conflict warning.
 *
 * There's no user directory endpoint yet (see `UserController`), so the
 * target user is identified by id, typed in directly.
 */
export function RoleAssignmentPanel({ roleId, roleName, companyId }: RoleAssignmentPanelProps) {
  const queryClient = useQueryClient();
  const [lookupUserId, setLookupUserId] = useState<string | null>(null);
  const [sodWarning, setSodWarning] = useState<string | null>(null);

  const form = useForm<AssignValues>({
    resolver: zodResolver(assignSchema),
    defaultValues: { userId: "" },
  });

  const rolesQueryKey = ["user-roles", lookupUserId, companyId];
  const { data: userRoles } = useQuery({
    queryKey: rolesQueryKey,
    queryFn: () => listUserRoles(lookupUserId as string, companyId),
    enabled: !!lookupUserId,
  });

  const assignMutation = useMutation({
    mutationFn: (userId: string) => assignRole(userId, roleId, companyId),
    onSuccess: (_data, userId) => {
      setSodWarning(null);
      setLookupUserId(userId);
      queryClient.invalidateQueries({ queryKey: ["user-roles", userId, companyId] });
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.status === 409) {
        setSodWarning(error.message);
      } else {
        setSodWarning(error instanceof ApiError ? error.message : "Could not assign this role.");
      }
    },
  });

  const unassignMutation = useMutation({
    mutationFn: (target: { userId: string; roleId: string }) =>
      unassignRole(target.userId, target.roleId, companyId),
    onSuccess: (_data, target) => {
      queryClient.invalidateQueries({ queryKey: ["user-roles", target.userId, companyId] });
    },
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Assign &quot;{roleName}&quot; to a user</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => assignMutation.mutate(values.userId))}
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="userId" label="User ID" />
          </div>
          <Button type="submit" disabled={assignMutation.isPending}>
            {assignMutation.isPending ? "Assigning…" : "Assign role"}
          </Button>
        </form>
      </Form>

      {sodWarning && (
        <p role="alert" className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {sodWarning}
        </p>
      )}

      {lookupUserId && userRoles && (
        <div className="flex flex-col gap-2">
          <h3 className="text-xs font-medium text-muted-foreground">
            Roles this user holds in this company
          </h3>
          {userRoles.length === 0 ? (
            <p className="text-sm text-muted-foreground">No roles assigned.</p>
          ) : (
            <ul className="flex flex-col gap-1">
              {userRoles.map((assignment) => (
                <li
                  key={assignment.roleId}
                  className="flex items-center justify-between rounded-md border px-3 py-1.5 text-sm"
                >
                  <span className="font-mono text-xs">{assignment.roleId}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={unassignMutation.isPending}
                    onClick={() => unassignMutation.mutate({ userId: lookupUserId, roleId: assignment.roleId })}
                  >
                    Unassign
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
