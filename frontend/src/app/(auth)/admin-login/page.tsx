"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import Link from "next/link";

import { adminLogin } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { applySession } from "@/lib/auth/session";
import { safeNext } from "@/lib/auth/safe-redirect";
import { setPendingChallenge } from "@/lib/auth/mfa-challenge";
import { ADMIN_DEFAULT_PATH } from "@/lib/auth/admin-realm";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const adminLoginSchema = z.object({
  email: z.email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});

type AdminLoginValues = z.infer<typeof adminLoginSchema>;

/**
 * ADM-1 / ADM-5 / F0.11.7: the admin realm's own sign-in screen for
 * platform and brand admin staff — genuinely separate from `/login`
 * (F0.2.1), not that page with a hidden or sentinel tenant field (see the
 * design note on this task and `AdminAuthController`'s javadoc on the
 * backend for why). There is no tenant field: admin staff have no tenant
 * of their own to supply.
 */
function AdminLoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = safeNext(searchParams.get("next"));
  const [serverError, setServerError] = useState<string | null>(null);

  const form = useForm<AdminLoginValues>({
    resolver: zodResolver(adminLoginSchema),
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit(values: AdminLoginValues) {
    setServerError(null);
    try {
      const result = await adminLogin(values);

      if (result.mfaRequired && result.mfaChallengeToken) {
        setPendingChallenge({
          mfaChallengeToken: result.mfaChallengeToken,
          email: values.email,
          defaultNext: ADMIN_DEFAULT_PATH,
        });
        router.push(`/totp-verify${next ? `?next=${encodeURIComponent(next)}` : ""}`);
        return;
      }

      if (result.accessToken && result.refreshToken) {
        applySession(result.accessToken, result.refreshToken, values.email);
        const effectiveNext = next ?? ADMIN_DEFAULT_PATH;
        if (result.passwordChangeRequired) {
          router.replace(`/account/change-password?required=1&next=${encodeURIComponent(effectiveNext)}`);
          return;
        }
        router.replace(effectiveNext);
      }
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "Sign in failed. Try again.");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Admin sign in</h1>
        <p className="text-sm text-muted-foreground">
          For platform and brand admin staff. Tenant users should use the regular sign-in page.
        </p>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <ValidatedTextField control={form.control} name="email" label="Email" type="email" />
          <ValidatedTextField control={form.control} name="password" label="Password" type="password" />
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </Form>
      <Link
        href="/login"
        className="text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
      >
        Not an admin? Sign in here
      </Link>
    </div>
  );
}

export default function AdminLoginPage() {
  return (
    <Suspense fallback={null}>
      <AdminLoginForm />
    </Suspense>
  );
}
