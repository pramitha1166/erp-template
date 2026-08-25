import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AuthGuard } from "./auth-guard";
import { useSessionStore } from "@/stores/session-store";

const { mockReplace, mockRestoreSession } = vi.hoisted(() => ({
  mockReplace: vi.fn(),
  mockRestoreSession: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }),
  usePathname: () => "/admin/roles",
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

  it("redirects to /login with the current path as `next` when restoration fails", async () => {
    mockRestoreSession.mockResolvedValue(false);

    render(
      <AuthGuard>
        <p>secret</p>
      </AuthGuard>,
    );

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("/login?next=%2Fadmin%2Froles"));
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });
});
