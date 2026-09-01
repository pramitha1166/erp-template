import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PendingApprovalsList } from "./pending-approvals-list";
import type { TaskView } from "@/lib/api/workflow-api";

const { mockMyPendingTasks } = vi.hoisted(() => ({
  mockMyPendingTasks: vi.fn(),
}));

vi.mock("@/lib/api/workflow-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/workflow-api")>("@/lib/api/workflow-api");
  return { ...actual, myPendingTasks: mockMyPendingTasks };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const tasks: TaskView[] = [
  { id: "t1", instanceId: "i1", documentType: "PURCHASE_ORDER", documentId: "PO-1", status: "PENDING", dueAt: null },
  {
    id: "t2",
    instanceId: "i2",
    documentType: "PURCHASE_ORDER",
    documentId: "PO-2",
    status: "ESCALATED",
    dueAt: "2026-01-01T00:00:00Z",
  },
  { id: "t3", instanceId: "i3", documentType: "PURCHASE_ORDER", documentId: "PO-3", status: "APPROVED", dueAt: null },
];

describe("PendingApprovalsList", () => {
  it("shows an empty message when nothing is pending", async () => {
    mockMyPendingTasks.mockResolvedValue([]);

    renderWithClient(<PendingApprovalsList companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("Nothing waiting on your approval.")).toBeInTheDocument());
  });

  it("only shows PENDING and ESCALATED tasks, with the escalation indicator (F0.4.5)", async () => {
    mockMyPendingTasks.mockResolvedValue(tasks);

    renderWithClient(<PendingApprovalsList companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("PO-1")).toBeInTheDocument());
    expect(screen.getByText("PO-2")).toBeInTheDocument();
    expect(screen.getByText("Escalated")).toBeInTheDocument();
    expect(screen.queryByText("PO-3")).not.toBeInTheDocument();
  });

  it("caps rows to the given limit and shows a view-all link with the actionable count", async () => {
    mockMyPendingTasks.mockResolvedValue(tasks);

    renderWithClient(<PendingApprovalsList companyId="co-1" limit={1} showViewAllLink />);

    await waitFor(() => expect(screen.getByText("PO-1")).toBeInTheDocument());
    expect(screen.queryByText("PO-2")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View all (2)" })).toHaveAttribute("href", "/approvals");
  });
});
