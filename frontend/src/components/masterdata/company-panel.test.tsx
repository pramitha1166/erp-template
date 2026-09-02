import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { useTenantStore } from "@/stores/tenant-store";
import { CompanyPanel } from "./company-panel";

const { mockListCompanies, mockCreateCompany, mockUpdateCompany, mockDisableCompany, mockEnableCompany } = vi.hoisted(
  () => ({
    mockListCompanies: vi.fn(),
    mockCreateCompany: vi.fn(),
    mockUpdateCompany: vi.fn(),
    mockDisableCompany: vi.fn(),
    mockEnableCompany: vi.fn(),
  }),
);

vi.mock("@/lib/api/masterdata-company-api", () => ({
  listCompanies: mockListCompanies,
  createCompany: mockCreateCompany,
  updateCompany: mockUpdateCompany,
  disableCompany: mockDisableCompany,
  enableCompany: mockEnableCompany,
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const acme = {
  id: "co-1",
  legalName: "Acme (Pvt) Ltd",
  registrationNo: "REG-1",
  vatNo: "VAT-1",
  address: "Colombo",
  baseCurrency: "LKR",
  fiscalYearStartMonth: 1,
  logoUrl: null,
  disabled: false,
};

describe("CompanyPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    useTenantStore.getState().clearTenantContext();
  });

  it("lists companies in the tenant", async () => {
    mockListCompanies.mockResolvedValue([acme]);

    renderWithClient(<CompanyPanel />);

    await waitFor(() => expect(screen.getByText("Acme (Pvt) Ltd")).toBeInTheDocument());
    expect(screen.getByText(/LKR/)).toBeInTheDocument();
  });

  it("disables adding another company until one is active", async () => {
    mockListCompanies.mockResolvedValue([acme]);

    renderWithClient(<CompanyPanel />);

    await waitFor(() => expect(screen.getByText("Acme (Pvt) Ltd")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Add another company" })).toBeDisabled();
  });

  it("creates another company anchored on the active company", async () => {
    const user = userEvent.setup();
    useTenantStore.getState().setActiveCompany({ id: "co-1", name: "Acme (Pvt) Ltd" });
    mockListCompanies.mockResolvedValue([acme]);
    mockCreateCompany.mockResolvedValue({ ...acme, id: "co-2", legalName: "Acme Lanka" });

    renderWithClient(<CompanyPanel />);

    await waitFor(() => expect(screen.getByRole("button", { name: "Add another company" })).toBeEnabled());
    await user.click(screen.getByRole("button", { name: "Add another company" }));

    await user.type(screen.getByLabelText("Legal name"), "Acme Lanka");
    await user.clear(screen.getByLabelText("Base currency"));
    await user.type(screen.getByLabelText("Base currency"), "USD");
    await user.click(screen.getByRole("button", { name: "Create company" }));

    await waitFor(() =>
      expect(mockCreateCompany).toHaveBeenCalledWith(
        "co-1",
        expect.objectContaining({ legalName: "Acme Lanka", baseCurrency: "USD", fiscalYearStartMonth: 1 }),
      ),
    );
  });

  it("edits a company's amendable fields", async () => {
    const user = userEvent.setup();
    mockListCompanies.mockResolvedValue([acme]);
    mockUpdateCompany.mockResolvedValue({ ...acme, legalName: "Acme Holdings" });

    renderWithClient(<CompanyPanel />);

    await waitFor(() => expect(screen.getByText("Acme (Pvt) Ltd")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Edit" }));
    await user.clear(screen.getByLabelText("Legal name"));
    await user.type(screen.getByLabelText("Legal name"), "Acme Holdings");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() =>
      expect(mockUpdateCompany).toHaveBeenCalledWith("co-1", { legalName: "Acme Holdings", address: "Colombo", logoUrl: "" }),
    );
  });

  it("toggles a company between active and disabled", async () => {
    const user = userEvent.setup();
    mockListCompanies.mockResolvedValue([acme]);
    mockDisableCompany.mockResolvedValue(undefined);

    renderWithClient(<CompanyPanel />);

    await waitFor(() => expect(screen.getByText("Acme (Pvt) Ltd")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisableCompany).toHaveBeenCalledWith("co-1"));
  });
});
