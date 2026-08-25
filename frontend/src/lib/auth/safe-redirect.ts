/**
 * `next` is a client-supplied query param (from `AuthGuard`'s redirect to
 * `/login?next=...`) — accepting it verbatim as a redirect target would be
 * an open-redirect hole, so only an in-app relative path is honored.
 */
export function safeNext(next: string | null | undefined): string | null {
  if (!next) {
    return null;
  }
  if (!next.startsWith("/") || next.startsWith("//")) {
    return null;
  }
  return next;
}
