import * as adminApi from "@/lib/api/admin-api";
import { decodeAccessToken } from "@/lib/api/jwt";
import * as tokens from "@/lib/api/tokens";
import { useImpersonationStore } from "@/stores/impersonation-store";
import { useSessionStore } from "@/stores/session-store";

/**
 * F0.11.5 / ADM-7: swaps the live session (in-memory access token +
 * localStorage refresh token, per `tokens.ts`) for a time-boxed
 * impersonation token, stashing the admin's own tokens so `endImpersonation`
 * can restore them. The impersonation token is deliberately given no
 * refresh token of its own: if it silently refreshed on 401, actions taken
 * after expiry would run as the admin while the UI still showed the
 * impersonation banner, which is exactly the mislabeled-actor problem
 * ADM-7's mandatory audit tagging exists to prevent.
 */
export async function startImpersonation(tenantId: string, tenantName: string, reason: string): Promise<void> {
  const adminAccessToken = tokens.getAccessToken();
  const adminRefreshToken = tokens.getRefreshToken();
  const adminUser = useSessionStore.getState().user;

  const started = await adminApi.startImpersonationSession(tenantId, reason);

  useImpersonationStore.getState().start({
    tenantId,
    tenantName,
    sessionId: started.sessionId,
    expiresAt: started.expiresAt,
    adminAccessToken,
    adminRefreshToken,
    adminUser,
  });

  tokens.setAccessToken(started.token);
  tokens.setRefreshToken(null);

  const decoded = decodeAccessToken(started.token);
  useSessionStore.getState().setUser({
    id: decoded?.userId ?? "",
    email: tenantName,
    displayName: `${tenantName} (impersonated)`,
  });
}

/**
 * Ends the active impersonation session and restores the admin's own
 * session. Safe to call more than once (e.g. the banner's expiry timer
 * racing a manual "End" click) — it no-ops once the store is already clear.
 */
export async function endImpersonation(): Promise<void> {
  const state = useImpersonationStore.getState();
  if (!state.active || !state.tenantId || !state.sessionId) {
    return;
  }

  // The end endpoint requires the admin's own permission, not the
  // impersonated tenant admin's — restore the admin token first.
  tokens.setAccessToken(state.adminAccessToken);
  tokens.setRefreshToken(state.adminRefreshToken);

  try {
    await adminApi.endImpersonationSession(state.tenantId, state.sessionId);
  } finally {
    if (state.adminUser) {
      useSessionStore.getState().setUser(state.adminUser);
    }
    useImpersonationStore.getState().clear();
  }
}
