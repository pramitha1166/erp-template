import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ImpersonationBanner } from "./impersonation-banner";
import { useImpersonationStore } from "@/stores/impersonation-store";

const { mockReplace, mockEndImpersonation } = vi.hoisted(() => ({
  mockReplace: vi.fn(),
  mockEndImpersonation: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

vi.mock("@/lib/auth/impersonation", () => ({
  endImpersonation: mockEndImpersonation,
}));

afterEach(() => {
  useImpersonationStore.getState().clear();
  mockReplace.mockClear();
  mockEndImpersonation.mockReset();
  vi.useRealTimers();
});

describe("ImpersonationBanner", () => {
  it("renders nothing when no session is active", () => {
    render(<ImpersonationBanner />);

    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows the impersonated tenant and a live countdown while active", () => {
    vi.useFakeTimers().setSystemTime(new Date("2026-01-01T00:00:00Z"));
    useImpersonationStore.getState().start({
      tenantId: "tenant-1",
      tenantName: "Acme Corp",
      sessionId: "session-1",
      expiresAt: "2026-01-01T00:05:00Z",
      adminAccessToken: "admin-token",
      adminRefreshToken: "admin-refresh",
      adminUser: { id: "admin-1", email: "admin@example.com", displayName: "admin@example.com" },
    });

    render(<ImpersonationBanner />);

    expect(screen.getByText("Acme Corp", { exact: false })).toBeInTheDocument();
    expect(screen.getByText(/ends in 5:00/)).toBeInTheDocument();
  });

  it("ends the session and redirects home when 'End impersonation' is clicked", async () => {
    const user = userEvent.setup();
    mockEndImpersonation.mockResolvedValue(undefined);
    useImpersonationStore.getState().start({
      tenantId: "tenant-1",
      tenantName: "Acme Corp",
      sessionId: "session-1",
      expiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
      adminAccessToken: "admin-token",
      adminRefreshToken: "admin-refresh",
      adminUser: { id: "admin-1", email: "admin@example.com", displayName: "admin@example.com" },
    });
    render(<ImpersonationBanner />);

    await user.click(screen.getByRole("button", { name: "End impersonation" }));

    expect(mockEndImpersonation).toHaveBeenCalled();
    expect(mockReplace).toHaveBeenCalledWith("/");
  });

  it("auto-ends the session once the countdown reaches zero", async () => {
    vi.useFakeTimers().setSystemTime(new Date("2026-01-01T00:00:00Z"));
    mockEndImpersonation.mockResolvedValue(undefined);
    useImpersonationStore.getState().start({
      tenantId: "tenant-1",
      tenantName: "Acme Corp",
      sessionId: "session-1",
      expiresAt: "2026-01-01T00:00:02Z",
      adminAccessToken: "admin-token",
      adminRefreshToken: "admin-refresh",
      adminUser: { id: "admin-1", email: "admin@example.com", displayName: "admin@example.com" },
    });
    render(<ImpersonationBanner />);

    await vi.advanceTimersByTimeAsync(3000);

    expect(mockEndImpersonation).toHaveBeenCalled();
  });
});
