import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { CurrencyPanel } from "./currency-panel";

const { mockListCurrencies, mockCreateCurrency, mockDisableCurrency, mockRecordRate, mockRateHistory } = vi.hoisted(() => ({
  mockListCurrencies: vi.fn(),
  mockCreateCurrency: vi.fn(),
  mockDisableCurrency: vi.fn(),
  mockRecordRate: vi.fn(),
  mockRateHistory: vi.fn(),
}));

vi.mock("@/lib/api/masterdata-currency-api", async () => {
  const actual =
    await vi.importActual<typeof import("@/lib/api/masterdata-currency-api")>("@/lib/api/masterdata-currency-api");
  return {
    ...actual,
    listCurrencies: mockListCurrencies,
    createCurrency: mockCreateCurrency,
    disableCurrency: mockDisableCurrency,
    recordRate: mockRecordRate,
    rateHistory: mockRateHistory,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const lkr = { id: "cur-1", code: "LKR", name: "Sri Lankan Rupee", symbol: "Rs.", decimalPlaces: 2, disabled: false };
const usd = { id: "cur-2", code: "USD", name: "US Dollar", symbol: "$", decimalPlaces: 2, disabled: false };

describe("CurrencyPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRateHistory.mockResolvedValue([]);
  });

  it("lists enabled currencies", async () => {
    mockListCurrencies.mockResolvedValue([lkr, usd]);

    renderWithClient(<CurrencyPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("LKR — Sri Lankan Rupee (Rs.)")).toBeInTheDocument();
  });

  it("creates a currency anchored on the active company", async () => {
    const user = userEvent.setup();
    mockListCurrencies.mockResolvedValue([]);
    mockCreateCurrency.mockResolvedValue({ ...usd });

    renderWithClient(<CurrencyPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByPlaceholderText("e.g. USD")).toBeInTheDocument());
    await user.type(screen.getByPlaceholderText("e.g. USD"), "USD");
    await user.type(screen.getByPlaceholderText("e.g. US Dollar"), "US Dollar");
    await user.type(screen.getByPlaceholderText("e.g. $"), "$");
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() =>
      expect(mockCreateCurrency).toHaveBeenCalledWith("co-1", { code: "USD", name: "US Dollar", symbol: "$", decimalPlaces: 2 }),
    );
  });

  it("toggles a currency between active and disabled", async () => {
    const user = userEvent.setup();
    mockListCurrencies.mockResolvedValue([lkr]);
    mockDisableCurrency.mockResolvedValue(undefined);

    renderWithClient(<CurrencyPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    await user.click(within(listEl).getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisableCurrency).toHaveBeenCalledWith("cur-1", "co-1"));
  });

  it("records an exchange rate", async () => {
    const user = userEvent.setup();
    mockListCurrencies.mockResolvedValue([usd]);
    mockRecordRate.mockResolvedValue({ id: "rate-1", currencyCode: "USD", rateDate: "2026-01-01", rateToBase: 300, source: "MANUAL" });

    renderWithClient(<CurrencyPanel companyId="co-1" />);

    await waitFor(() => expect(within(screen.getByLabelText("Currency")).getAllByRole("option").length).toBeGreaterThan(1));
    await user.selectOptions(screen.getByLabelText("Currency"), "USD");
    await user.type(screen.getByLabelText("Effective date"), "2026-01-01");
    await user.type(screen.getByLabelText("Rate to base currency"), "300");
    await user.click(screen.getByRole("button", { name: "Record rate" }));

    await waitFor(() =>
      expect(mockRecordRate).toHaveBeenCalledWith("co-1", { currencyCode: "USD", rateDate: "2026-01-01", rateToBase: 300 }),
    );
  });

  it("shows rate history once a currency is selected", async () => {
    const user = userEvent.setup();
    mockListCurrencies.mockResolvedValue([usd]);
    mockRateHistory.mockResolvedValue([{ id: "rate-1", currencyCode: "USD", rateDate: "2026-01-01", rateToBase: 300, source: "CBSL" }]);

    renderWithClient(<CurrencyPanel companyId="co-1" />);

    await waitFor(() =>
      expect(within(screen.getByLabelText("View rate history for")).getAllByRole("option").length).toBeGreaterThan(1),
    );
    await user.selectOptions(screen.getByLabelText("View rate history for"), "USD");

    expect(await screen.findByText(/1 USD = 300/)).toBeInTheDocument();
    expect(screen.getByText("(CBSL import)")).toBeInTheDocument();
  });
});
