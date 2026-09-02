import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { NumberingSeriesPanel } from "./numbering-series-panel";

const { mockListSeries, mockConfigureSeries, mockActivateSeries, mockDeactivateSeries } = vi.hoisted(() => ({
  mockListSeries: vi.fn(),
  mockConfigureSeries: vi.fn(),
  mockActivateSeries: vi.fn(),
  mockDeactivateSeries: vi.fn(),
}));

vi.mock("@/lib/api/numbering-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/numbering-api")>("@/lib/api/numbering-api");
  return {
    ...actual,
    listSeries: mockListSeries,
    configureSeries: mockConfigureSeries,
    activateSeries: mockActivateSeries,
    deactivateSeries: mockDeactivateSeries,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const existingSeries = {
  id: "s1",
  companyId: "c1",
  docType: "SALES_INVOICE",
  prefix: "SINV-",
  counterWidth: 5,
  resetPolicy: "NEVER" as const,
  fiscalYearStartMonth: 1,
  active: true,
  nextCounter: 7,
};

describe("NumberingSeriesPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lists configured series and shows their next counter", async () => {
    mockListSeries.mockResolvedValue([existingSeries]);

    renderWithClient(<NumberingSeriesPanel companyId="c1" />);

    await waitFor(() => expect(screen.getByText("SALES_INVOICE")).toBeInTheDocument());
    expect(screen.getByText(/next #7/)).toBeInTheDocument();
  });

  it("creates a new series with the entered template", async () => {
    const user = userEvent.setup();
    mockListSeries.mockResolvedValue([]);
    mockConfigureSeries.mockResolvedValue({ ...existingSeries, docType: "PAYMENT_VOUCHER", prefix: "PAY-" });

    renderWithClient(<NumberingSeriesPanel companyId="c1" />);

    await waitFor(() => expect(screen.getByText("No naming series configured yet for this company.")).toBeInTheDocument());

    await user.type(screen.getByLabelText("Document type"), "PAYMENT_VOUCHER");
    await user.clear(screen.getByLabelText("Prefix template"));
    await user.type(screen.getByLabelText("Prefix template"), "PAY-");
    await user.click(screen.getByRole("button", { name: "Add series" }));

    await waitFor(() =>
      expect(mockConfigureSeries).toHaveBeenCalledWith("c1", {
        docType: "PAYMENT_VOUCHER",
        prefix: "PAY-",
        counterWidth: 5,
        resetPolicy: "NEVER",
        fiscalYearStartMonth: 1,
      }),
    );
  });

  it("requires a fiscal-year start month when the reset policy is annual", async () => {
    const user = userEvent.setup();
    mockListSeries.mockResolvedValue([]);

    renderWithClient(<NumberingSeriesPanel companyId="c1" />);

    await waitFor(() => expect(screen.getByLabelText("Document type")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Document type"), "GOODS_RECEIPT_NOTE");
    await user.type(screen.getByLabelText("Prefix template"), "GRN-{FY}-");
    await user.selectOptions(screen.getByLabelText("Reset policy"), "ANNUAL");
    await user.clear(screen.getByLabelText("Fiscal-year start month (1-12)"));
    await user.click(screen.getByRole("button", { name: "Add series" }));

    expect(
      await screen.findByText("Fiscal-year start month (1-12) is required when the reset policy is annual"),
    ).toBeInTheDocument();
    expect(mockConfigureSeries).not.toHaveBeenCalled();
  });

  it("toggles an existing series between active and inactive", async () => {
    const user = userEvent.setup();
    mockListSeries.mockResolvedValue([existingSeries]);
    mockDeactivateSeries.mockResolvedValue(undefined);

    renderWithClient(<NumberingSeriesPanel companyId="c1" />);

    await waitFor(() => expect(screen.getByText("SALES_INVOICE")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Deactivate" }));

    await waitFor(() => expect(mockDeactivateSeries).toHaveBeenCalledWith("s1", "c1"));
  });

  it("shows a live preview of the next number as the template is edited", async () => {
    const user = userEvent.setup();
    mockListSeries.mockResolvedValue([]);

    renderWithClient(<NumberingSeriesPanel companyId="c1" />);

    await waitFor(() => expect(screen.getByLabelText("Prefix template")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Prefix template"), "SINV-");

    expect(await screen.findByText("SINV-00001")).toBeInTheDocument();
  });
});
