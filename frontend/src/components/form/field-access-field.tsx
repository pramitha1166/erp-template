"use client";

import type { HTMLInputTypeAttribute } from "react";
import type { Control, FieldPath, FieldValues } from "react-hook-form";

import { FormField, FormItem, FormLabel } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import type { FieldAccess } from "@/lib/api/iam-api";

export interface FieldAccessFieldProps<TFieldValues extends FieldValues> {
  /**
   * What the caller's effective roles grant on this field (IAM-5's
   * `FieldAccess`: `NONE` | `READ` | `WRITE`). There is no REST endpoint
   * yet that resolves this for the signed-in user against a real entity
   * (`FieldPermissionApi.resolveAccess` is a Spring Modulith-internal call
   * other backend modules make, not something exposed over HTTP) — Phase 1
   * module screens are what will eventually compute this value and pass it
   * in, once real entities with restricted fields exist.
   */
  access: FieldAccess;
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  type?: HTMLInputTypeAttribute;
  placeholder?: string;
}

/**
 * F0.2.8 / IAM-5: the field-level permission pattern — e.g. `salary`,
 * restricted to HR roles. `NONE` renders nothing at all (the field doesn't
 * exist for this viewer, not just disabled); `READ` renders the value as
 * plain text with no input control (visible, not editable); `WRITE` is the
 * normal editable field. Module screens compose this instead of a bare
 * `ValidatedTextField` for any column a role's field permissions can
 * restrict.
 */
export function FieldAccessField<TFieldValues extends FieldValues>({
  access,
  control,
  name,
  label,
  type = "text",
  placeholder,
}: FieldAccessFieldProps<TFieldValues>) {
  if (access === "NONE") {
    return null;
  }

  if (access === "READ") {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field }) => (
          <FormItem>
            <FormLabel>{label}</FormLabel>
            <p className="text-sm text-foreground">{field.value || "—"}</p>
          </FormItem>
        )}
      />
    );
  }

  return (
    <ValidatedTextField control={control} name={name} label={label} type={type} placeholder={placeholder} />
  );
}
