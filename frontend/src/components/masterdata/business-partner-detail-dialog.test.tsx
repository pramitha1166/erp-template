import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { BusinessPartnerDetailDialog } from "./business-partner-detail-dialog";

const { mockListContacts, mockUpdatePartner, mockAddContact } = vi.hoisted(() => ({
  mockListContacts: vi.fn(),
  mockUpdatePartner: vi.fn(),
  mockAddContact: vi.fn(),
}));

vi.mock("@/lib/api/masterdata-partner-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-partner-api")>("@/lib/api/masterdata-partner-api");
  return { ...actual, listContacts: mockListContacts, updatePartner: mockUpdatePartner, addContact: mockAddContact };
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
  creditLimit: 0,
  creditTermsDays: 0,
  defaultAccountId: null,
  bankName: null,
  bankBranch: null,
  bankAccountNo: null,
  bankSwiftCode: null,
  disabled: false,
};

describe("BusinessPartnerDetailDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("saves credit terms and bank details", async () => {
    const user = userEvent.setup();
    mockListContacts.mockResolvedValue([]);
    mockUpdatePartner.mockResolvedValue({ ...acme, creditTermsDays: 45 });

    renderWithClient(<BusinessPartnerDetailDialog companyId="co-1" partner={acme} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getByLabelText("Credit terms (days)")).toBeInTheDocument());
    await user.clear(screen.getByLabelText("Credit terms (days)"));
    await user.type(screen.getByLabelText("Credit terms (days)"), "45");
    await user.type(screen.getByLabelText("Bank name"), "BOC");
    await user.click(screen.getByRole("button", { name: "Save details" }));

    await waitFor(() =>
      expect(mockUpdatePartner).toHaveBeenCalledWith(
        "bp-1",
        "co-1",
        expect.objectContaining({ creditTermsDays: 45, bankName: "BOC" }),
      ),
    );
  });

  it("marks the first added contact as primary", async () => {
    const user = userEvent.setup();
    mockListContacts.mockResolvedValue([]);
    mockAddContact.mockResolvedValue({ id: "c-1", name: "Jane Silva", designation: null, phone: null, email: null, primaryContact: true });

    renderWithClient(<BusinessPartnerDetailDialog companyId="co-1" partner={acme} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getByLabelText("Contact name")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Contact name"), "Jane Silva");
    await user.click(screen.getByRole("button", { name: "Add contact" }));

    await waitFor(() =>
      expect(mockAddContact).toHaveBeenCalledWith(
        "bp-1",
        "co-1",
        expect.objectContaining({ name: "Jane Silva", primaryContact: true }),
      ),
    );
  });

  it("lists existing contacts", async () => {
    mockListContacts.mockResolvedValue([
      { id: "c-1", name: "Jane Silva", designation: "Finance Manager", phone: "0771234567", email: null, primaryContact: true },
    ]);

    renderWithClient(<BusinessPartnerDetailDialog companyId="co-1" partner={acme} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getByText("Jane Silva")).toBeInTheDocument());
    expect(screen.getByText("(primary)")).toBeInTheDocument();
    expect(screen.getByText(/Finance Manager/)).toBeInTheDocument();
  });
});
