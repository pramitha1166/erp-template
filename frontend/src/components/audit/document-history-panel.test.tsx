import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DocumentHistoryPanel } from "./document-history-panel";

const { mockGetDocumentHistory } = vi.hoisted(() => ({
  mockGetDocumentHistory: vi.fn(),
}));

vi.mock("@/lib/api/audit-api", () => ({
  getDocumentHistory: mockGetDocumentHistory,
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("DocumentHistoryPanel", () => {
  it("shows an empty message when there is no history", async () => {
    mockGetDocumentHistory.mockResolvedValue([]);

    renderWithClient(<DocumentHistoryPanel entityType="sales.invoice" entityId="INV-1" companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("No version history yet.")).toBeInTheDocument());
    expect(mockGetDocumentHistory).toHaveBeenCalledWith("sales.invoice", "INV-1", "co-1");
  });

  it("renders each entry with its action, actor, and timestamp", async () => {
    mockGetDocumentHistory.mockResolvedValue([
      {
        id: "e1",
        action: "INSERT",
        actor: "alice@example.com",
        occurredAt: "2026-01-01T00:00:00Z",
        oldValues: {},
        newValues: { status: "DRAFT" },
      },
    ]);

    renderWithClient(<DocumentHistoryPanel entityType="sales.invoice" entityId="INV-1" companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("Created")).toBeInTheDocument());
    expect(screen.getByText("alice@example.com")).toBeInTheDocument();
  });

  it("shows only the fields that actually changed between old and new values", async () => {
    mockGetDocumentHistory.mockResolvedValue([
      {
        id: "e2",
        action: "UPDATE",
        actor: "bob@example.com",
        occurredAt: "2026-01-02T00:00:00Z",
        oldValues: { status: "DRAFT", amount: "100.0000" },
        newValues: { status: "SUBMITTED", amount: "100.0000" },
      },
    ]);

    renderWithClient(<DocumentHistoryPanel entityType="sales.invoice" entityId="INV-1" companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("status")).toBeInTheDocument());
    expect(screen.getByText("DRAFT")).toBeInTheDocument();
    expect(screen.getByText("SUBMITTED")).toBeInTheDocument();
    expect(screen.queryByText("amount")).not.toBeInTheDocument();
  });

  it("shows an error message when the request fails", async () => {
    mockGetDocumentHistory.mockRejectedValue(new Error("boom"));

    renderWithClient(<DocumentHistoryPanel entityType="sales.invoice" entityId="INV-1" companyId="co-1" />);

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Could not load version history."));
  });
});
