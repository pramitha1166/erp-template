/**
 * F0.2.1: there is no "current user" endpoint (see `UserController`'s
 * comment — user profile lookups are deferred past this epic), so the only
 * source of the caller's own id is the `sub` claim already inside the
 * access token `AuthController` hands back. This reads that claim only —
 * it does not verify the signature, which is fine here: the token is only
 * ever used to talk to a backend that verifies it independently, so a
 * tampered claim would just fail every subsequent request as an invalid
 * token, never grant anything client-side.
 */
export interface DecodedAccessToken {
  userId: string;
  tenantId: string;
}

export function decodeAccessToken(token: string): DecodedAccessToken | null {
  const segments = token.split(".");
  if (segments.length !== 3) {
    return null;
  }
  try {
    const payload = JSON.parse(base64UrlDecode(segments[1])) as Record<string, unknown>;
    if (typeof payload.sub !== "string" || typeof payload.tid !== "string") {
      return null;
    }
    return { userId: payload.sub, tenantId: payload.tid };
  } catch {
    return null;
  }
}

function base64UrlDecode(segment: string): string {
  const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return atob(padded);
}
