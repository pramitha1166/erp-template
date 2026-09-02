import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { BusinessPartnerPanel } from "./business-partner-panel";

const { mockListPartners, mockCreatePartner, mockDisablePartner, mockListContacts } = vi.hoisted(() => ({
  mockListPartners: vi.fn(),
  mockCreatePartner: vi.fn(),
  mockDisablePartner: vi.fn(),
  mockListContacts: vi.fn(),
}));

vi.mock("@/lib/api/masterdata-partner-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-partner-api")>("@/lib/api/masterdata-partner-api");
  return {
    ...actual,
    listPartners: mockListPartners,
    createPartner: mockCreatePartner,
    disablePartner: mockDisablePartner,
    listContacts: mockListContacts,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const acme: import("@/lib/api/masterdata-partner-api").PartnerView = {
  id: "bp-1",
  partnerType: "CUSTOMER",
  code: "CUST-001",
  name: "Acme Traders",
  taxRegistrationNo: null,
  creditLimit: 50000,
  creditTermsDays: 30,
  defaultAccountId: null,
  bankName: null,
  bankBranch: null,
  bankAccountNo: null,
  bankSwiftCode: null,
  disabled: false,
};

describe("BusinessPartnerPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListContacts.mockResolvedValue([]);
  });

  it("lists customers and suppliers", async () => {
    mockListPartners.mockResolvedValue([acme]);

    renderWithClient(<BusinessPartnerPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("CUST-001 — Acme Traders")).toBeInTheDocument());
    expect(screen.getByText(/30-day terms/)).toBeInTheDocument();
  });

  it("creates a new partner", async () => {
    const user = userEvent.setup();
    mockListPartners.mockResolvedValue([]);
    mockCreatePartner.mockResolvedValue({ ...acme, id: "bp-2", code: "SUP-001" });

    renderWithClient(<BusinessPartnerPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByLabelText("Code")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Code"), "SUP-001");
    await user.type(screen.getByLabelText("Name"), "Ceylon Supplies");
    await user.selectOptions(screen.getByLabelText("Type"), "SUPPLIER");
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() =>
      expect(mockCreatePartner).toHaveBeenCalledWith("co-1", {
        partnerType: "SUPPLIER",
        code: "SUP-001",
        name: "Ceylon Supplies",
      }),
    );
  });

  it("toggles a partner between active and disabled", async () => {
    const user = userEvent.setup();
    mockListPartners.mockResolvedValue([acme]);
    mockDisablePartner.mockResolvedValue(undefined);

    renderWithClient(<BusinessPartnerPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("CUST-001 — Acme Traders")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisablePartner).toHaveBeenCalledWith("bp-1", "co-1"));
  });

  it("opens the detail dialog for a partner", async () => {
    const user = userEvent.setup();
    mockListPartners.mockResolvedValue([acme]);

    renderWithClient(<BusinessPartnerPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("CUST-001 — Acme Traders")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Manage" }));

    expect(await screen.findByRole("heading", { name: "CUST-001 — Acme Traders" })).toBeInTheDocument();
  });

  it("re-fetches with the type filter applied", async () => {
    const user = userEvent.setup();
    mockListPartners.mockResolvedValue([acme]);

    renderWithClient(<BusinessPartnerPanel companyId="co-1" />);

    await waitFor(() => expect(mockListPartners).toHaveBeenCalledWith("co-1", undefined));
    await user.selectOptions(screen.getByLabelText("Filter by type"), "SUPPLIER");

    await waitFor(() => expect(mockListPartners).toHaveBeenCalledWith("co-1", "SUPPLIER"));
  });
});
