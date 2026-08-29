import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AuthGuard } from "./auth-guard";
import { useSessionStore } from "@/stores/session-store";

const { mockReplace, mockRestoreSession, mockPathname } = vi.hoisted(() => ({
  mockReplace: vi.fn(),
  mockRestoreSession: vi.fn(),
  mockPathname: vi.fn(() => "/admin/roles"),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }),
  usePathname: mockPathname,
}));

vi.mock("@/lib/auth/session", () => ({
  restoreSession: mockRestoreSession,
}));

afterEach(() => {
  // Unmount before clearing the store: AuthGuard subscribes to
  // isAuthenticated, so resetting it first would re-render (and re-run
  // effects on) a component that's still mounted, racing the global
  // cleanup() from vitest.setup.ts.
  cleanup();
  useSessionStore.getState().clearSession();
  mockReplace.mockClear();
  mockRestoreSession.mockReset();
  mockPathname.mockReturnValue("/admin/roles");
});

describe("AuthGuard", () => {
  it("renders children immediately when already authenticated", () => {
    useSessionStore.getState().setUser({ id: "u1", email: "a@b.com", displayName: "A" });

    render(
      <AuthGuard>
        <p>secret</p>
      </AuthGuard>,
    );

    expect(screen.getByText("secret")).toBeInTheDocument();
    expect(mockRestoreSession).not.toHaveBeenCalled();
  });

  it("F0.2.1: restores the session from a stored refresh token on a hard reload", async () => {
    mockRestoreSession.mockImplementation(async () => {
      useSessionStore.getState().setUser({ id: "u1", email: "a@b.com", displayName: "A" });
      return true;
    });

    render(
      <AuthGuard>
        <p>secret</p>
      </AuthGuard>,
    );

    await waitFor(() => expect(screen.getByText("secret")).toBeInTheDocument());
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it("F0.11.7: redirects to /admin-login with the current path as `next` when an admin route's restoration fails", async () => {
    mockRestoreSession.mockResolvedValue(false);

    render(
      <AuthGuard>
        <p>secret</p>
      </AuthGuard>,
    );

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("/admin-login?next=%2Fadmin%2Froles"));
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });

  it("redirects to /login with the current path as `next` when a tenant route's restoration fails", async () => {
    mockPathname.mockReturnValue("/dashboard");
    mockRestoreSession.mockResolvedValue(false);

    render(
      <AuthGuard>
        <p>secret</p>
      </AuthGuard>,
    );

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("/login?next=%2Fdashboard"));
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });
});
