import * as authApi from "@/lib/api/auth-api";
import { decodeAccessToken } from "@/lib/api/jwt";
import * as tokens from "@/lib/api/tokens";
import { useSessionStore } from "@/stores/session-store";

/**
 * F0.2.1: the one place that turns a token pair into the session-store's
 * notion of "who is logged in". `email` is whatever the caller already
 * knows (typed at login, or recalled from `tokens.getLastEmail`) — the
 * access token itself only carries a user id (see `jwt.ts`).
 */
export function applySession(accessToken: string, refreshToken: string, email: string): void {
  tokens.setAccessToken(accessToken);
  tokens.setRefreshToken(refreshToken);
  tokens.setLastEmail(email);
  const decoded = decodeAccessToken(accessToken);
  useSessionStore.getState().setUser({
    id: decoded?.userId ?? "",
    email,
    displayName: email,
  });
}

/**
 * Called once on app boot (see `AuthGuard`): if a refresh token survived
 * from a previous visit, spend it immediately to mint a fresh access token
 * rather than waiting for the first API call to 401. Clears everything on
 * any failure (expired, revoked, or reuse-detected — `SessionService`
 * revokes the whole session family in that last case) so a stale token
 * doesn't linger.
 */
export async function restoreSession(): Promise<boolean> {
  const refreshToken = tokens.getRefreshToken();
  if (!refreshToken) {
    return false;
  }
  try {
    const pair = await authApi.refresh(refreshToken);
    applySession(pair.accessToken, pair.refreshToken, tokens.getLastEmail() ?? "");
    return true;
  } catch {
    tokens.clearTokens();
    return false;
  }
}

/** IAM-1: logout — best-effort revoke server-side, but the client-side session always clears. */
export async function endSession(): Promise<void> {
  const refreshToken = tokens.getRefreshToken();
  useSessionStore.getState().clearSession();
  tokens.clearTokens();
  if (refreshToken) {
    try {
      await authApi.logout(refreshToken);
    } catch {
      // Session is already cleared client-side; a failed revoke here just
      // means the server-side row outlives it until it expires naturally.
    }
  }
}
