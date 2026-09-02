import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { vi } from "vitest";

import { ApprovalHistoryTimeline } from "./approval-history-timeline";

const { mockGetApprovalHistory } = vi.hoisted(() => ({
  mockGetApprovalHistory: vi.fn(),
}));

vi.mock("@/lib/api/workflow-api", () => ({
  getApprovalHistory: mockGetApprovalHistory,
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ApprovalHistoryTimeline", () => {
  it("shows an empty message when there is no history", async () => {
    mockGetApprovalHistory.mockResolvedValue([]);

    renderWithClient(<ApprovalHistoryTimeline companyId="co-1" documentType="PURCHASE_ORDER" documentId="PO-1" />);

    await waitFor(() => expect(screen.getByText("No approval activity yet.")).toBeInTheDocument());
    expect(mockGetApprovalHistory).toHaveBeenCalledWith("co-1", "PURCHASE_ORDER", "PO-1");
  });

  it("renders each entry with its action, actor, and comment", async () => {
    mockGetApprovalHistory.mockResolvedValue([
      {
        instanceId: "inst-1",
        action: "REJECTED",
        actorUserId: "user-1",
        comment: "Missing budget approval",
        occurredAt: "2026-01-01T00:00:00Z",
      },
    ]);

    renderWithClient(<ApprovalHistoryTimeline companyId="co-1" documentType="PURCHASE_ORDER" documentId="PO-1" />);

    await waitFor(() => expect(screen.getByText("Rejected")).toBeInTheDocument());
    expect(screen.getByText("user-1")).toBeInTheDocument();
    expect(screen.getByText("Missing budget approval")).toBeInTheDocument();
  });

  it("shows an error message when the request fails", async () => {
    mockGetApprovalHistory.mockRejectedValue(new Error("boom"));

    renderWithClient(<ApprovalHistoryTimeline companyId="co-1" documentType="PURCHASE_ORDER" documentId="PO-1" />);

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Could not load approval history."));
  });
});
