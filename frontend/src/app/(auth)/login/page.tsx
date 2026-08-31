"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { login } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { getLastTenantId, setLastTenantId } from "@/lib/api/tokens";
import { applySession } from "@/lib/auth/session";
import { safeNext } from "@/lib/auth/safe-redirect";
import { setPendingChallenge } from "@/lib/auth/mfa-challenge";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import Link from "next/link";

const loginSchema = z.object({
  tenantId: z.uuid("Enter your tenant id (a UUID)"),
  email: z.email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
});

type LoginValues = z.infer<typeof loginSchema>;

/** IAM-1: email/password login, handing off to `/totp-verify` when the account has 2FA enrolled (IAM-2). */
function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = safeNext(searchParams.get("next"));
  const [serverError, setServerError] = useState<string | null>(null);

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      tenantId: getLastTenantId() ?? "",
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginValues) {
    setServerError(null);
    try {
      const result = await login(values);
      setLastTenantId(values.tenantId);

      if (result.mfaRequired && result.mfaChallengeToken) {
        setPendingChallenge({ mfaChallengeToken: result.mfaChallengeToken, email: values.email, defaultNext: "/" });
        router.push(`/totp-verify${next ? `?next=${encodeURIComponent(next)}` : ""}`);
        return;
      }

      if (result.accessToken && result.refreshToken) {
        applySession(result.accessToken, result.refreshToken, values.email);
        if (result.passwordChangeRequired) {
          const suffix = next ? `&next=${encodeURIComponent(next)}` : "";
          router.replace(`/account/change-password?required=1${suffix}`);
          return;
        }
        router.replace(next ?? "/");
      }
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "Sign in failed. Try again.");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Sign in</h1>
        <p className="text-sm text-muted-foreground">
          Enter your tenant, email, and password to continue.
        </p>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <ValidatedTextField control={form.control} name="tenantId" label="Tenant ID" />
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
        href="/forgot-password"
        className="text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
      >
        Forgot password?
      </Link>
      <Link
        href="/admin-login"
        className="text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
      >
        Platform or brand admin? Sign in here
      </Link>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
