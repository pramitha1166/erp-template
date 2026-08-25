"use client";

import { Suspense, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { changePassword } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { getSecurityPolicy, type SecurityPolicy } from "@/lib/api/iam-api";
import { passwordPolicyChecklist } from "@/lib/auth/password-policy";
import { safeNext } from "@/lib/auth/safe-redirect";
import { PasswordPolicyChecklist } from "@/components/auth/password-policy-checklist";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

/**
 * The policy is fetched async, so its rules can't be baked into a static
 * Zod schema up front. Reading it through `getPolicy()` inside
 * `superRefine` — rather than swapping the schema itself per fetch — keeps
 * the inferred form type fixed and lets `getPolicy` return fresher data
 * (via a ref) without recreating the `useForm` hook.
 */
function buildSchema(getPolicy: () => SecurityPolicy | undefined) {
  return z
    .object({
      currentPassword: z.string().min(1, "Current password is required"),
      newPassword: z.string().min(1, "New password is required"),
      confirmPassword: z.string().min(1, "Confirm your new password"),
    })
    .superRefine((values, ctx) => {
      if (values.newPassword !== values.confirmPassword) {
        ctx.addIssue({ code: "custom", path: ["confirmPassword"], message: "Passwords do not match" });
      }
      const policy = getPolicy();
      if (!policy) {
        return;
      }
      for (const rule of passwordPolicyChecklist(policy, values.newPassword)) {
        if (!rule.met) {
          ctx.addIssue({ code: "custom", path: ["newPassword"], message: rule.label });
        }
      }
    });
}

type ChangePasswordValues = z.infer<ReturnType<typeof buildSchema>>;

function ChangePasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const required = searchParams.get("required") === "1";
  const next = safeNext(searchParams.get("next"));
  const [serverError, setServerError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const { data: policy } = useQuery({
    queryKey: ["security-policy"],
    queryFn: getSecurityPolicy,
    staleTime: 60_000,
  });

  const policyRef = useRef(policy);
  policyRef.current = policy;
  const schema = useMemo(() => buildSchema(() => policyRef.current), []);

  const form = useForm<ChangePasswordValues>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const newPassword = useWatch({ control: form.control, name: "newPassword" }) ?? "";

  async function onSubmit(values: ChangePasswordValues) {
    setServerError(null);
    try {
      await changePassword(values.currentPassword, values.newPassword);
      setSuccess(true);
      form.reset();
      setTimeout(() => router.replace(next ?? "/"), 1200);
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError([error.message, ...error.details].filter(Boolean).join(" — "));
      } else {
        setServerError("Could not change your password. Try again.");
      }
    }
  }

  return (
    <div className="flex max-w-sm flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Change password</h1>
        {required && (
          <p className="text-sm text-destructive">
            Your password has expired. Set a new one to continue.
          </p>
        )}
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <ValidatedTextField
            control={form.control}
            name="currentPassword"
            label="Current password"
            type="password"
          />
          <ValidatedTextField
            control={form.control}
            name="newPassword"
            label="New password"
            type="password"
          />
          {policy && <PasswordPolicyChecklist policy={policy} value={newPassword} />}
          <ValidatedTextField
            control={form.control}
            name="confirmPassword"
            label="Confirm new password"
            type="password"
          />
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          {success && (
            <p role="status" className="text-sm text-primary">
              Password changed.
            </p>
          )}
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Saving…" : "Change password"}
          </Button>
        </form>
      </Form>
    </div>
  );
}

export default function ChangePasswordPage() {
  return (
    <Suspense fallback={null}>
      <ChangePasswordForm />
    </Suspense>
  );
}
