import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ChartOfAccountsPanel } from "./chart-of-accounts-panel";

const { mockListAccounts, mockCreateAccount, mockRenameAccount, mockActivateAccount, mockDeactivateAccount } = vi.hoisted(
  () => ({
    mockListAccounts: vi.fn(),
    mockCreateAccount: vi.fn(),
    mockRenameAccount: vi.fn(),
    mockActivateAccount: vi.fn(),
    mockDeactivateAccount: vi.fn(),
  }),
);

vi.mock("@/lib/api/masterdata-coa-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-coa-api")>("@/lib/api/masterdata-coa-api");
  return {
    ...actual,
    listAccounts: mockListAccounts,
    createAccount: mockCreateAccount,
    renameAccount: mockRenameAccount,
    activateAccount: mockActivateAccount,
    deactivateAccount: mockDeactivateAccount,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const assets = { id: "a-1", code: "1000", name: "Assets", accountType: "ASSET" as const, parentId: null, group: true, active: true };
const cash = { id: "a-2", code: "1100", name: "Cash", accountType: "ASSET" as const, parentId: "a-1", group: false, active: true };

describe("ChartOfAccountsPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("only offers group accounts as a parent", async () => {
    mockListAccounts.mockResolvedValue([assets, cash]);

    renderWithClient(<ChartOfAccountsPanel companyId="co-1" />);

    await waitFor(() =>
      expect(within(screen.getByLabelText("Parent")).getAllByRole("option").length).toBeGreaterThan(1),
    );
    const options = within(screen.getByLabelText("Parent")).getAllByRole("option").map((o) => o.textContent);
    expect(options).toContain("1000 — Assets");
    expect(options).not.toContain("1100 — Cash");
  });

  it("locks the account type to the selected parent's type", async () => {
    const user = userEvent.setup();
    mockListAccounts.mockResolvedValue([assets]);

    renderWithClient(<ChartOfAccountsPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByLabelText("Parent")).toBeInTheDocument());
    await user.selectOptions(screen.getByLabelText("Account type"), "LIABILITY");
    await user.selectOptions(screen.getByLabelText("Parent"), "a-1");

    expect(screen.getByLabelText("Account type")).toHaveValue("ASSET");
  });

  it("creates a ledger account under a group parent", async () => {
    const user = userEvent.setup();
    mockListAccounts.mockResolvedValue([assets]);
    mockCreateAccount.mockResolvedValue({ ...cash, id: "a-3" });

    renderWithClient(<ChartOfAccountsPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByLabelText("Code")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Code"), "1100");
    await user.type(screen.getByLabelText("Name"), "Cash");
    await user.selectOptions(screen.getByLabelText("Parent"), "a-1");
    await user.click(screen.getByLabelText("Group account (can have children)"));
    await user.click(screen.getByRole("button", { name: "Add account" }));

    await waitFor(() =>
      expect(mockCreateAccount).toHaveBeenCalledWith("co-1", {
        code: "1100",
        name: "Cash",
        accountType: "ASSET",
        parentId: "a-1",
        group: false,
      }),
    );
  });

  it("toggles an account between active and inactive", async () => {
    const user = userEvent.setup();
    mockListAccounts.mockResolvedValue([assets]);
    mockDeactivateAccount.mockResolvedValue(undefined);

    renderWithClient(<ChartOfAccountsPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    await user.click(within(listEl).getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDeactivateAccount).toHaveBeenCalledWith("a-1", "co-1"));
  });
});
