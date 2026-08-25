"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { setFieldPermission, type FieldAccess } from "@/lib/api/iam-api";
import { ApiError } from "@/lib/api/http";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ValidatedSelectField } from "@/components/form/validated-select-field";

const fieldPermissionSchema = z.object({
  entityCode: z.string().min(1, "Entity code is required (e.g. payroll:employee)"),
  fieldName: z.string().min(1, "Field name is required (e.g. salary)"),
  access: z.enum(["NONE", "READ", "WRITE"]),
});

type FieldPermissionValues = z.infer<typeof fieldPermissionSchema>;

export interface FieldPermissionFormProps {
  roleId: string;
  companyId: string;
}

/**
 * F0.2.5 / F0.2.8 / IAM-5: sets a role's access to one field on an entity
 * (e.g. `payroll:employee` / `salary` → `READ`) — the data half of the
 * field-level permission pattern the `FieldAccessGate` component (F0.2.8)
 * renders against. There's no read endpoint for a role's existing field
 * permissions (`RoleController` only exposes `POST`), so this shows what
 * was set this session rather than the role's full current state.
 */
export function FieldPermissionForm({ roleId, companyId }: FieldPermissionFormProps) {
  const [recent, setRecent] = useState<FieldPermissionValues[]>([]);

  const form = useForm<FieldPermissionValues>({
    resolver: zodResolver(fieldPermissionSchema),
    defaultValues: { entityCode: "", fieldName: "", access: "READ" },
  });

  const mutation = useMutation({
    mutationFn: (values: FieldPermissionValues) =>
      setFieldPermission(roleId, companyId, values.entityCode, values.fieldName, values.access as FieldAccess),
    onSuccess: (_data, values) => {
      setRecent((existing) => [values, ...existing.filter((entry) => entry.fieldName !== values.fieldName || entry.entityCode !== values.entityCode)]);
    },
  });

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Field-level permissions</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="entityCode" label="Entity code" placeholder="payroll:employee" />
          </div>
          <div className="flex-1">
            <ValidatedTextField control={form.control} name="fieldName" label="Field name" placeholder="salary" />
          </div>
          <div className="w-32">
            <ValidatedSelectField
              control={form.control}
              name="access"
              label="Access"
              options={[
                { value: "NONE", label: "Hidden" },
                { value: "READ", label: "Read-only" },
                { value: "WRITE", label: "Editable" },
              ]}
            />
          </div>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Saving…" : "Set access"}
          </Button>
        </form>
      </Form>
      {mutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {mutation.error instanceof ApiError ? mutation.error.message : "Could not set that field permission."}
        </p>
      )}
      {recent.length > 0 && (
        <ul className="flex flex-col gap-1">
          {recent.map((entry) => (
            <li key={`${entry.entityCode}.${entry.fieldName}`} className="text-sm">
              <code className="text-xs">
                {entry.entityCode}.{entry.fieldName}
              </code>{" "}
              → {entry.access}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
