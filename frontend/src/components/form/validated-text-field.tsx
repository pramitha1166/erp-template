"use client";

import type { HTMLInputTypeAttribute } from "react";
import type { Control, FieldPath, FieldValues } from "react-hook-form";

import {
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";

export interface ValidatedTextFieldProps<TFieldValues extends FieldValues> {
  control: Control<TFieldValues>;
  name: FieldPath<TFieldValues>;
  label: string;
  type?: HTMLInputTypeAttribute;
  placeholder?: string;
}

/**
 * F0.1.4 / NFR-U4: the reusable react-hook-form + Zod pattern — a field's
 * own validation issue renders next to that field, not as a generic
 * failure toast. Every module screen's form fields should be built from
 * this (or the same `FormField`/`FormMessage` primitives directly for
 * non-text inputs) rather than re-deriving error display per screen.
 */
export function ValidatedTextField<TFieldValues extends FieldValues>({
  control,
  name,
  label,
  type = "text",
  placeholder,
}: ValidatedTextFieldProps<TFieldValues>) {
  return (
    <FormField
      control={control}
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input type={type} placeholder={placeholder} {...field} />
          </FormControl>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}
