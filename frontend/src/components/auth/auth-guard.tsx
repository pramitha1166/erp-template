"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { ADMIN_LOGIN_PATH, isAdminRoute } from "@/lib/auth/admin-realm";
import { restoreSession } from "@/lib/auth/session";
import { useSessionStore } from "@/stores/session-store";

/**
 * F0.2.1: gates every route under the `(app)` group. A hard reload has no
 * access token in memory (see `tokens.ts`), so this always spends one
 * `restoreSession()` attempt against the stored refresh token before
 * deciding there's really no session — otherwise every reload would bounce
 * an already-logged-in user out to `/login`.
 *
 * F0.11.7: an unauthenticated visit under `/admin/**` bounces to the admin
 * realm's own login instead — the tenant `/login` form asks for a
 * `tenantId` platform/brand admin staff don't have (see
 * `AdminAuthController`'s javadoc on the backend).
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useSessionStore((state) => state.isAuthenticated);
  const [checking, setChecking] = useState(!isAuthenticated);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (isAuthenticated) {
      setChecking(false);
      return;
    }
    let cancelled = false;
    restoreSession().then((restored) => {
      if (cancelled) {
        return;
      }
      setChecking(false);
      if (!restored) {
        const next = encodeURIComponent(pathname);
        const loginPath = isAdminRoute(pathname) ? ADMIN_LOGIN_PATH : "/login";
        router.replace(`${loginPath}?next=${next}`);
      }
    });
    return () => {
      cancelled = true;
    };
    // Only re-run this check when the auth flag itself flips (e.g. logout).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  if (checking) {
    return (
      <div
        role="status"
        aria-label="Loading"
        className="flex min-h-dvh items-center justify-center"
      >
        <div className="size-6 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
