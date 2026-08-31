/**
 * F0.11.7 / ADM-1 / ADM-5: the admin realm's own login route and default
 * post-login landing page — distinct from the tenant realm's `/login` and
 * `/` (see `AdminAuthController`'s javadoc on the backend for why platform
 * and brand admin staff never go through the tenant login contract).
 * Centralised here so `AuthGuard`, the admin login page, and the shared
 * `/totp-verify` step all agree on the same values instead of each
 * hardcoding its own copy.
 */
export const ADMIN_LOGIN_PATH = "/admin-login";

/**
 * There's no "current admin" lookup endpoint yet to route a brand admin
 * straight to their own brand (see the comment on `NAV_ITEMS`'s "Platform
 * Admin" entry) — Brands is the closest thing this console has to a home
 * screen today, and is the one page every admin realm's nav already points
 * at first.
 */
export const ADMIN_DEFAULT_PATH = "/admin/platform/brands";

export function isAdminRoute(pathname: string): boolean {
  return pathname === "/admin" || pathname.startsWith("/admin/");
}
