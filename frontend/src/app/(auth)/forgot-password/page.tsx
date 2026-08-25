import Link from "next/link";

/**
 * F0.2.3 / IAM-9: self-service, unauthenticated password reset needs a
 * backend capability that doesn't exist yet — `/auth/password/change`
 * (see `AuthController`) requires the *current* password, and there is no
 * reset-token issuance/email endpoint anywhere in the IAM module. Building
 * that here would mean adding a new backend flow (a migration, a token
 * table, an outbound email dependency) from a frontend-only epic, which is
 * exactly the "don't build ahead of the phase gate" case in the root
 * CLAUDE.md. Until that lands, this page only tells an operator how reset
 * actually happens today; a signed-in user's own reset is `/account/change-password`.
 */
export default function ForgotPasswordPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Forgot your password?</h1>
      <p className="text-sm text-muted-foreground">
        Self-service password reset by email isn&apos;t available yet. Ask your
        tenant administrator to help you regain access, or, if you can still
        sign in, change your password from your account settings instead.
      </p>
      <Link
        href="/login"
        className="text-sm text-primary underline-offset-4 hover:underline"
      >
        Back to sign in
      </Link>
    </div>
  );
}
