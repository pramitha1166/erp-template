import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { DelegationPanel } from "./delegation-panel";

const { mockMyDelegations, mockCreateDelegation, mockRevokeDelegation } = vi.hoisted(() => ({
  mockMyDelegations: vi.fn(),
  mockCreateDelegation: vi.fn(),
  mockRevokeDelegation: vi.fn(),
}));

vi.mock("@/lib/api/workflow-api", () => ({
  myDelegations: mockMyDelegations,
  createDelegation: mockCreateDelegation,
  revokeDelegation: mockRevokeDelegation,
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const validUuid = "123e4567-e89b-12d3-a456-426614174000";

describe("DelegationPanel", () => {
  it("rejects an end date before the start date", async () => {
    const user = userEvent.setup();
    mockMyDelegations.mockResolvedValue([]);

    renderWithClient(<DelegationPanel />);

    await user.type(screen.getByLabelText("Delegate user id"), validUuid);
    await user.type(screen.getByLabelText("Start date"), "2026-02-10");
    await user.type(screen.getByLabelText("End date"), "2026-02-01");
    await user.click(screen.getByRole("button", { name: "Delegate" }));

    expect(await screen.findByText("End date must be on or after the start date")).toBeInTheDocument();
    expect(mockCreateDelegation).not.toHaveBeenCalled();
  });

  it("creates a delegation for a valid date range", async () => {
    const user = userEvent.setup();
    mockMyDelegations.mockResolvedValue([]);
    mockCreateDelegation.mockResolvedValue({
      id: "d1",
      delegatorUserId: "me",
      delegateUserId: validUuid,
      startDate: "2026-02-01",
      endDate: "2026-02-10",
      reason: null,
      revoked: false,
    });

    renderWithClient(<DelegationPanel />);

    await user.type(screen.getByLabelText("Delegate user id"), validUuid);
    await user.type(screen.getByLabelText("Start date"), "2026-02-01");
    await user.type(screen.getByLabelText("End date"), "2026-02-10");
    await user.click(screen.getByRole("button", { name: "Delegate" }));

    await waitFor(() =>
      expect(mockCreateDelegation).toHaveBeenCalledWith(validUuid, "2026-02-01", "2026-02-10", undefined),
    );
  });

  it("lists active delegations and revokes one", async () => {
    const user = userEvent.setup();
    mockMyDelegations.mockResolvedValue([
      {
        id: "d1",
        delegatorUserId: "me",
        delegateUserId: validUuid,
        startDate: "2026-02-01",
        endDate: "2026-02-10",
        reason: "On leave",
        revoked: false,
      },
    ]);
    mockRevokeDelegation.mockResolvedValue(undefined);

    renderWithClient(<DelegationPanel />);

    await waitFor(() => expect(screen.getByText("On leave", { exact: false })).toBeInTheDocument());

    await user.click(screen.getByRole("button", { name: "Revoke" }));

    await waitFor(() => expect(mockRevokeDelegation).toHaveBeenCalledWith("d1"));
  });
});
