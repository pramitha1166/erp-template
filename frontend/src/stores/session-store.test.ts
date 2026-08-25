import { afterEach, describe, expect, it } from "vitest";

import { useSessionStore } from "./session-store";

afterEach(() => {
  useSessionStore.getState().clearSession();
});

describe("useSessionStore", () => {
  it("starts unauthenticated", () => {
    expect(useSessionStore.getState().isAuthenticated).toBe(false);
    expect(useSessionStore.getState().user).toBeNull();
  });

  it("sets the authenticated user", () => {
    const user = { id: "u-1", displayName: "Nimal Perera", email: "nimal@example.com" };
    useSessionStore.getState().setUser(user);

    expect(useSessionStore.getState().user).toEqual(user);
    expect(useSessionStore.getState().isAuthenticated).toBe(true);
  });

  it("clears the session", () => {
    useSessionStore.getState().setUser({
      id: "u-1",
      displayName: "Nimal Perera",
      email: "nimal@example.com",
    });

    useSessionStore.getState().clearSession();

    expect(useSessionStore.getState().user).toBeNull();
    expect(useSessionStore.getState().isAuthenticated).toBe(false);
  });
});
