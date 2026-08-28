"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { acceptInvite } from "@/lib/api/admin-api";
import { ApiError } from "@/lib/api/http";
import { setLastTenantId } from "@/lib/api/tokens";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";

const acceptSchema = z
  .object({
    password: z.string().min(12, "At least 12 characters"),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

type AcceptValues = z.infer<typeof acceptSchema>;

/** F0.11.4 / ADM-5: the recipient side of the tenant-admin invite — sets a password against the public `/invites/accept` endpoint. */
function InviteAcceptForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tenantId = searchParams.get("tenantId");
  const token = searchParams.get("token");
  const [serverError, setServerError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const form = useForm<AcceptValues>({
    resolver: zodResolver(acceptSchema),
    defaultValues: { password: "", confirmPassword: "" },
  });

  async function onSubmit(values: AcceptValues) {
    if (!tenantId || !token) {
      return;
    }
    setServerError(null);
    try {
      await acceptInvite(tenantId, token, values.password);
      setLastTenantId(tenantId);
      setDone(true);
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "Could not accept this invite.");
    }
  }

  if (!tenantId || !token) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-xl font-semibold">Invite link incomplete</h1>
        <p className="text-sm text-muted-foreground">
          This link is missing its tenant or token. Ask whoever invited you to resend it.
        </p>
        <Link href="/login" className="text-sm text-primary underline-offset-4 hover:underline">
          Back to sign in
        </Link>
      </div>
    );
  }

  if (done) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-xl font-semibold">You&apos;re all set</h1>
        <p className="text-sm text-muted-foreground">Your password is set. Sign in to continue.</p>
        <Button onClick={() => router.push("/login")}>Go to sign in</Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Set your password</h1>
        <p className="text-sm text-muted-foreground">Finish accepting your tenant administrator invite.</p>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <ValidatedTextField control={form.control} name="password" label="Password" type="password" />
          <ValidatedTextField control={form.control} name="confirmPassword" label="Confirm password" type="password" />
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          <Button type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Setting password…" : "Set password"}
          </Button>
        </form>
      </Form>
    </div>
  );
}

export default function InviteAcceptPage() {
  return (
    <Suspense fallback={null}>
      <InviteAcceptForm />
    </Suspense>
  );
}
