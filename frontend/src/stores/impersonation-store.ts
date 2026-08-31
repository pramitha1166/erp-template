import { create } from "zustand";

import type { SessionUser } from "./session-store";

/**
 * F0.11.5 / ADM-7: tracks an active brand/platform-admin-as-tenant-admin
 * session. Deliberately not persisted (unlike `tenant-store`) — the stashed
 * admin tokens below are as sensitive as the refresh token `tokens.ts`
 * already keeps in localStorage, but doubling that up across a reload adds
 * risk for a 30-minute, already-short-lived feature. A reload while
 * impersonating just ends the impersonated view; the admin signs in again.
 */
interface ImpersonationState {
  active: boolean;
  tenantId: string | null;
  tenantName: string | null;
  sessionId: string | null;
  expiresAt: string | null;
  adminAccessToken: string | null;
  adminRefreshToken: string | null;
  adminUser: SessionUser | null;
  start: (session: {
    tenantId: string;
    tenantName: string;
    sessionId: string;
    expiresAt: string;
    adminAccessToken: string | null;
    adminRefreshToken: string | null;
    adminUser: SessionUser | null;
  }) => void;
  clear: () => void;
}

export const useImpersonationStore = create<ImpersonationState>((set) => ({
  active: false,
  tenantId: null,
  tenantName: null,
  sessionId: null,
  expiresAt: null,
  adminAccessToken: null,
  adminRefreshToken: null,
  adminUser: null,
  start: (session) => set({ active: true, ...session }),
  clear: () =>
    set({
      active: false,
      tenantId: null,
      tenantName: null,
      sessionId: null,
      expiresAt: null,
      adminAccessToken: null,
      adminRefreshToken: null,
      adminUser: null,
    }),
}));
