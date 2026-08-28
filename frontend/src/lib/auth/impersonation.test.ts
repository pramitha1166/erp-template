import { afterEach, describe, expect, it, vi } from "vitest";

import { endImpersonation, startImpersonation } from "./impersonation";
import * as tokens from "@/lib/api/tokens";
import { useImpersonationStore } from "@/stores/impersonation-store";
import { useSessionStore } from "@/stores/session-store";

const { mockStartSession, mockEndSession } = vi.hoisted(() => ({
  mockStartSession: vi.fn(),
  mockEndSession: vi.fn(),
}));

vi.mock("@/lib/api/admin-api", () => ({
  startImpersonationSession: mockStartSession,
  endImpersonationSession: mockEndSession,
}));

function fakeToken(payload: Record<string, unknown>): string {
  const base64Url = (value: string) =>
    Buffer.from(value).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = base64Url(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

const adminUser = { id: "admin-1", email: "admin@example.com", displayName: "admin@example.com" };
const impersonationToken = fakeToken({ sub: "tenant-admin-1", tid: "tenant-1" });

afterEach(() => {
  tokens.clearTokens();
  useImpersonationStore.getState().clear();
  useSessionStore.getState().clearSession();
  mockStartSession.mockReset();
  mockEndSession.mockReset();
});

describe("startImpersonation", () => {
  it("stashes the admin's tokens and identity, then swaps in the impersonation token", async () => {
    tokens.setAccessToken("admin-access-token");
    tokens.setRefreshToken("admin-refresh-token");
    useSessionStore.getState().setUser(adminUser);
    mockStartSession.mockResolvedValue({
      sessionId: "session-1",
      token: impersonationToken,
      expiresAt: "2026-01-01T00:30:00Z",
    });

    await startImpersonation("tenant-1", "Acme Corp", "support ticket #1");

    expect(mockStartSession).toHaveBeenCalledWith("tenant-1", "support ticket #1");

    const state = useImpersonationStore.getState();
    expect(state.active).toBe(true);
    expect(state.tenantId).toBe("tenant-1");
    expect(state.sessionId).toBe("session-1");
    expect(state.adminAccessToken).toBe("admin-access-token");
    expect(state.adminRefreshToken).toBe("admin-refresh-token");
    expect(state.adminUser).toEqual(adminUser);

    // Swapped to the impersonation token, with no refresh token — a 401
    // after expiry must not silently refresh back to the admin's identity.
    expect(tokens.getAccessToken()).toBe(impersonationToken);
    expect(tokens.getRefreshToken()).toBeNull();

    expect(useSessionStore.getState().user).toEqual({
      id: "tenant-admin-1",
      email: "Acme Corp",
      displayName: "Acme Corp (impersonated)",
    });
  });
});

describe("endImpersonation", () => {
  it("restores the admin's tokens to call the end endpoint, then restores their session", async () => {
    tokens.setAccessToken("admin-access-token");
    tokens.setRefreshToken("admin-refresh-token");
    useSessionStore.getState().setUser(adminUser);
    mockStartSession.mockResolvedValue({
      sessionId: "session-1",
      token: impersonationToken,
      expiresAt: "2026-01-01T00:30:00Z",
    });
    await startImpersonation("tenant-1", "Acme Corp", "support ticket #1");
    mockEndSession.mockResolvedValue(undefined);

    await endImpersonation();

    expect(mockEndSession).toHaveBeenCalledWith("tenant-1", "session-1");
    expect(tokens.getAccessToken()).toBe("admin-access-token");
    expect(tokens.getRefreshToken()).toBe("admin-refresh-token");
    expect(useSessionStore.getState().user).toEqual(adminUser);
    expect(useImpersonationStore.getState().active).toBe(false);
  });

  it("restores the admin session even if the end call fails", async () => {
    tokens.setAccessToken("admin-access-token");
    tokens.setRefreshToken("admin-refresh-token");
    useSessionStore.getState().setUser(adminUser);
    mockStartSession.mockResolvedValue({
      sessionId: "session-1",
      token: impersonationToken,
      expiresAt: "2026-01-01T00:30:00Z",
    });
    await startImpersonation("tenant-1", "Acme Corp", "support ticket #1");
    mockEndSession.mockRejectedValue(new Error("network error"));

    await expect(endImpersonation()).rejects.toThrow("network error");

    expect(useSessionStore.getState().user).toEqual(adminUser);
    expect(useImpersonationStore.getState().active).toBe(false);
  });

  it("is a no-op when no session is active", async () => {
    await endImpersonation();

    expect(mockEndSession).not.toHaveBeenCalled();
  });
});
