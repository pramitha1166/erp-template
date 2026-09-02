import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { FiscalYearPanel } from "./fiscal-year-panel";

const {
  mockListFiscalYears,
  mockCloseFiscalYear,
  mockReopenFiscalYear,
  mockListAccountingPeriods,
  mockCloseAccountingPeriod,
} = vi.hoisted(() => ({
  mockListFiscalYears: vi.fn(),
  mockCloseFiscalYear: vi.fn(),
  mockReopenFiscalYear: vi.fn(),
  mockListAccountingPeriods: vi.fn(),
  mockCloseAccountingPeriod: vi.fn(),
}));

vi.mock("@/lib/api/masterdata-fiscalyear-api", async () => {
  const actual =
    await vi.importActual<typeof import("@/lib/api/masterdata-fiscalyear-api")>("@/lib/api/masterdata-fiscalyear-api");
  return {
    ...actual,
    listFiscalYears: mockListFiscalYears,
    closeFiscalYear: mockCloseFiscalYear,
    reopenFiscalYear: mockReopenFiscalYear,
    listAccountingPeriods: mockListAccountingPeriods,
    closeAccountingPeriod: mockCloseAccountingPeriod,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const fy2026 = { id: "fy-1", name: "FY 2026", startDate: "2026-01-01", endDate: "2026-12-31", status: "OPEN" as const };
const january = { id: "p-1", fiscalYearId: "fy-1", name: "January 2026", startDate: "2026-01-01", endDate: "2026-01-31", status: "OPEN" as const };

describe("FiscalYearPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("lists fiscal years", async () => {
    mockListFiscalYears.mockResolvedValue([fy2026]);

    renderWithClient(<FiscalYearPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("FY 2026")).toBeInTheDocument());
  });

  it("shows the backend's guard message when closing fails with an open period", async () => {
    const user = userEvent.setup();
    mockListFiscalYears.mockResolvedValue([fy2026]);
    mockCloseFiscalYear.mockRejectedValue(
      new (await import("@/lib/api/http")).ApiError(409, "Cannot close a fiscal year while it still has an open accounting period"),
    );

    renderWithClient(<FiscalYearPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("FY 2026")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Close" }));

    expect(await screen.findByText("Cannot close a fiscal year while it still has an open accounting period")).toBeInTheDocument();
  });

  it("closes a fiscal year once no period blocks it", async () => {
    const user = userEvent.setup();
    mockListFiscalYears.mockResolvedValue([fy2026]);
    mockCloseFiscalYear.mockResolvedValue(undefined);

    renderWithClient(<FiscalYearPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("FY 2026")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Close" }));

    await waitFor(() => expect(mockCloseFiscalYear).toHaveBeenCalledWith("fy-1", "co-1"));
  });

  it("expands to show accounting periods and closes one", async () => {
    const user = userEvent.setup();
    mockListFiscalYears.mockResolvedValue([fy2026]);
    mockListAccountingPeriods.mockResolvedValue([january]);
    mockCloseAccountingPeriod.mockResolvedValue(undefined);

    renderWithClient(<FiscalYearPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("FY 2026")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "View periods" }));

    const periodRow = (await screen.findByText("January 2026")).closest("li")!;
    await user.click(within(periodRow).getByRole("button", { name: "Close" }));

    await waitFor(() => expect(mockCloseAccountingPeriod).toHaveBeenCalledWith("p-1", "co-1"));
  });
});
