"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { disableTotp, enableTotp, setupTotp } from "@/lib/api/auth-api";
import { ApiError } from "@/lib/api/http";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { Separator } from "@/components/ui/separator";

const codeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "Enter the 6-digit code from your authenticator app"),
});

type CodeValues = z.infer<typeof codeSchema>;

/**
 * F0.2.2 / IAM-2: TOTP enrollment and disable. There's no "is 2FA enabled"
 * read endpoint on this user (see `AuthController` — only login's
 * `mfaRequired` reveals it, and only at login time), so this page can't
 * show current status up front; both actions are simply always offered.
 */
export default function TwoFactorPage() {
  const [pendingSecret, setPendingSecret] = useState<{ secret: string; otpAuthUri: string } | null>(null);
  const [setupError, setSetupError] = useState<string | null>(null);
  const [enrolled, setEnrolled] = useState(false);
  const [disabling, setDisabling] = useState(false);
  const [disableMessage, setDisableMessage] = useState<string | null>(null);

  const form = useForm<CodeValues>({
    resolver: zodResolver(codeSchema),
    defaultValues: { code: "" },
  });

  async function handleStartSetup() {
    setSetupError(null);
    setEnrolled(false);
    try {
      const start = await setupTotp();
      setPendingSecret(start);
    } catch (error) {
      setSetupError(error instanceof ApiError ? error.message : "Could not start 2FA setup.");
    }
  }

  async function onConfirm(values: CodeValues) {
    setSetupError(null);
    try {
      await enableTotp(values.code);
      setEnrolled(true);
      setPendingSecret(null);
      form.reset();
    } catch (error) {
      setSetupError(error instanceof ApiError ? error.message : "That code didn't match. Try again.");
    }
  }

  async function handleDisable() {
    setDisabling(true);
    setDisableMessage(null);
    try {
      await disableTotp();
      setDisableMessage("Two-factor authentication is now disabled.");
      setEnrolled(false);
    } catch (error) {
      setDisableMessage(error instanceof ApiError ? error.message : "Could not disable 2FA.");
    } finally {
      setDisabling(false);
    }
  }

  return (
    <div className="flex max-w-md flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Two-factor authentication</h1>
        <p className="text-sm text-muted-foreground">
          IAM-2: mandatory for any role holding an approval permission.
        </p>
      </div>

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Set up an authenticator app</h2>
        {!pendingSecret && !enrolled && (
          <Button type="button" variant="outline" onClick={handleStartSetup} className="self-start">
            Start setup
          </Button>
        )}
        {enrolled && (
          <p role="status" className="text-sm text-primary">
            Two-factor authentication is enabled.
          </p>
        )}
        {pendingSecret && (
          <div className="flex flex-col gap-3">
            <p className="text-sm text-muted-foreground">
              Add this key to your authenticator app, or use the setup link if your app supports it:
            </p>
            <code className="break-all rounded-md bg-muted px-2 py-1 text-xs">{pendingSecret.secret}</code>
            <a
              href={pendingSecret.otpAuthUri}
              className="text-xs text-primary underline-offset-4 hover:underline"
            >
              Open in authenticator app
            </a>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onConfirm)} className="flex flex-col gap-3">
                <ValidatedTextField control={form.control} name="code" label="Confirmation code" />
                {setupError && (
                  <p role="alert" className="text-sm text-destructive">
                    {setupError}
                  </p>
                )}
                <Button type="submit" disabled={form.formState.isSubmitting} className="self-start">
                  {form.formState.isSubmitting ? "Confirming…" : "Confirm and enable"}
                </Button>
              </form>
            </Form>
          </div>
        )}
      </section>

      <Separator />

      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Disable two-factor authentication</h2>
        <p className="text-sm text-muted-foreground">
          Turns off the code prompt at sign-in for this account.
        </p>
        <Button type="button" variant="destructive" onClick={handleDisable} disabled={disabling} className="self-start">
          {disabling ? "Disabling…" : "Disable 2FA"}
        </Button>
        {disableMessage && (
          <p role="status" className="text-sm text-muted-foreground">
            {disableMessage}
          </p>
        )}
      </section>
    </div>
  );
}
