import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AuditArchiveStatusIndicator } from "./audit-archive-status-indicator";

const { mockGetAuditArchiveStatus } = vi.hoisted(() => ({
  mockGetAuditArchiveStatus: vi.fn(),
}));

vi.mock("@/lib/api/audit-api", () => ({
  getAuditArchiveStatus: mockGetAuditArchiveStatus,
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("AuditArchiveStatusIndicator", () => {
  it("shows the retention window and that archival has not run yet", async () => {
    mockGetAuditArchiveStatus.mockResolvedValue({
      archivalEnabled: false,
      archivedThrough: null,
      lastObjectKey: null,
      coldStorageAfterYears: 2,
      minimumRetentionYears: 7,
    });

    renderWithClient(<AuditArchiveStatusIndicator companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("Archival not configured")).toBeInTheDocument());
    expect(screen.getByText(/Cold storage after 2 years/)).toBeInTheDocument();
    expect(screen.getByText(/minimum retention 7 years/)).toBeInTheDocument();
    expect(screen.getByText(/not yet archived/)).toBeInTheDocument();
  });

  it("shows the last archived-through timestamp once archival has run", async () => {
    mockGetAuditArchiveStatus.mockResolvedValue({
      archivalEnabled: true,
      archivedThrough: "2024-06-01T00:00:00Z",
      lastObjectKey: "tenant/object.json",
      coldStorageAfterYears: 2,
      minimumRetentionYears: 7,
    });

    renderWithClient(<AuditArchiveStatusIndicator companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("Archival enabled")).toBeInTheDocument());
    expect(screen.queryByText(/not yet archived/)).not.toBeInTheDocument();
    expect(screen.getByText(/Archived through:/)).toBeInTheDocument();
  });
});
