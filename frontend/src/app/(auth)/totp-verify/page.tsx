"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { verifyTotp } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { applySession } from "@/lib/auth/session";
import { clearPendingChallenge, getPendingChallenge } from "@/lib/auth/mfa-challenge";
import { safeNext } from "@/lib/auth/safe-redirect";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const codeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code from your authenticator app"),
});

type CodeValues = z.infer<typeof codeSchema>;

/** IAM-2: the second step of login for an account with TOTP 2FA enrolled — verifies the code against the challenge issued by `/auth/login`. */
function TotpVerifyForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = safeNext(searchParams.get("next"));
  const [serverError, setServerError] = useState<string | null>(null);
  const challenge = getPendingChallenge();

  const form = useForm<CodeValues>({
    resolver: zodResolver(codeSchema),
    defaultValues: { code: "" },
  });

  useEffect(() => {
    if (!challenge) {
      router.replace("/login");
    }
    // Only redirect on the very first render if there's no challenge to verify against.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const effectiveNext = next ?? challenge?.defaultNext ?? "/";

  async function onSubmit(values: CodeValues) {
    if (!challenge) {
      return;
    }
    setServerError(null);
    try {
      const result = await verifyTotp({ mfaChallengeToken: challenge.mfaChallengeToken, code: values.code });
      if (result.accessToken && result.refreshToken) {
        applySession(result.accessToken, result.refreshToken, challenge.email);
        clearPendingChallenge();
        if (result.passwordChangeRequired) {
          router.replace(`/account/change-password?required=1&next=${encodeURIComponent(effectiveNext)}`);
          return;
        }
        router.replace(effectiveNext);
      }
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "Verification failed. Try again.");
    }
  }

  if (!challenge) {
    return null;
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Enter your authentication code</h1>
        <p className="text-sm text-muted-foreground">
          Signed in as {challenge.email}. Open your authenticator app and enter the current 6-digit code.
        </p>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <ValidatedTextField
            control={form.control}
            name="code"
            label="Authentication code"
            type="text"
          />
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Verifying…" : "Verify"}
          </Button>
        </form>
      </Form>
    </div>
  );
}

export default function TotpVerifyPage() {
  return (
    <Suspense fallback={null}>
      <TotpVerifyForm />
    </Suspense>
  );
}
