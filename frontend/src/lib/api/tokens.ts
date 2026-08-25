/**
 * F0.2.1 / IAM-1: storage for the rotating-refresh session pair.
 *
 * The access token lives in memory only — it never touches storage, so a
 * successful XSS read of localStorage can't lift a live 15-minute bearer
 * token. The refresh token has to survive a reload (there is no httpOnly
 * cookie in this API — `AuthController` returns both tokens in the JSON
 * body), so it's kept in localStorage; `http.ts` treats it as opaque and
 * only ever sends it to `/auth/refresh` or `/auth/logout`.
 */

const REFRESH_TOKEN_KEY = "erp.refreshToken";
const LAST_TENANT_KEY = "erp.lastTenantId";
const LAST_EMAIL_KEY = "erp.lastEmail";

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string | null): void {
  if (typeof window === "undefined") {
    return;
  }
  if (token) {
    window.localStorage.setItem(REFRESH_TOKEN_KEY, token);
  } else {
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

export function clearTokens(): void {
  setAccessToken(null);
  setRefreshToken(null);
}

/** Convenience prefill for the login form only — never trusted as an auth decision. */
export function getLastTenantId(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(LAST_TENANT_KEY);
}

export function setLastTenantId(tenantId: string): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(LAST_TENANT_KEY, tenantId);
}

/**
 * There is no "current user" endpoint (see `jwt.ts`), so the email typed
 * at login is the only source of display text across a page reload. Purely
 * cosmetic — never used for an auth decision.
 */
export function getLastEmail(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(LAST_EMAIL_KEY);
}

export function setLastEmail(email: string): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(LAST_EMAIL_KEY, email);
}
