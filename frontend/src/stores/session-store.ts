import { create } from "zustand";

/**
 * Client-side session state only — never the tokens themselves. Rotating
 * refresh handling lives in the API client (F0.2.1); this store just
 * mirrors "am I logged in, and as whom" for UI decisions (nav, guards).
 */
export interface SessionUser {
  id: string;
  displayName: string;
  email: string;
}

interface SessionState {
  user: SessionUser | null;
  isAuthenticated: boolean;
  setUser: (user: SessionUser) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>((set) => ({
  user: null,
  isAuthenticated: false,
  setUser: (user) => set({ user, isAuthenticated: true }),
  clearSession: () => set({ user: null, isAuthenticated: false }),
}));
