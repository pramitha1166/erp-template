import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { UomPanel } from "./uom-panel";

const { mockListUoms, mockCreateUom, mockDisableUom, mockConfigureConversion, mockConversionsFrom } = vi.hoisted(() => ({
  mockListUoms: vi.fn(),
  mockCreateUom: vi.fn(),
  mockDisableUom: vi.fn(),
  mockConfigureConversion: vi.fn(),
  mockConversionsFrom: vi.fn(),
}));

vi.mock("@/lib/api/masterdata-uom-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-uom-api")>("@/lib/api/masterdata-uom-api");
  return {
    ...actual,
    listUoms: mockListUoms,
    createUom: mockCreateUom,
    disableUom: mockDisableUom,
    configureConversion: mockConfigureConversion,
    conversionsFrom: mockConversionsFrom,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const nos = { id: "uom-1", code: "NOS", name: "Numbers", disabled: false };
const box = { id: "uom-2", code: "BOX", name: "Box", disabled: false };

describe("UomPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockConversionsFrom.mockResolvedValue([]);
  });

  it("lists units of measure", async () => {
    mockListUoms.mockResolvedValue([nos, box]);

    renderWithClient(<UomPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("NOS — Numbers")).toBeInTheDocument();
    expect(within(listEl).getByText("BOX — Box")).toBeInTheDocument();
  });

  it("creates a new UOM anchored on the active company", async () => {
    const user = userEvent.setup();
    mockListUoms.mockResolvedValue([]);
    mockCreateUom.mockResolvedValue({ id: "uom-3", code: "KG", name: "Kilogram", disabled: false });

    renderWithClient(<UomPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByPlaceholderText("e.g. KG")).toBeInTheDocument());
    await user.type(screen.getByPlaceholderText("e.g. KG"), "KG");
    await user.type(screen.getByPlaceholderText("e.g. Kilogram"), "Kilogram");
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => expect(mockCreateUom).toHaveBeenCalledWith("co-1", "KG", "Kilogram"));
  });

  it("toggles a UOM between active and disabled", async () => {
    const user = userEvent.setup();
    mockListUoms.mockResolvedValue([nos]);
    mockDisableUom.mockResolvedValue(undefined);

    renderWithClient(<UomPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("NOS — Numbers")).toBeInTheDocument();
    await user.click(within(listEl).getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisableUom).toHaveBeenCalledWith("uom-1", "co-1"));
  });

  it("configures a conversion factor between two UOMs", async () => {
    const user = userEvent.setup();
    mockListUoms.mockResolvedValue([nos, box]);
    mockConfigureConversion.mockResolvedValue({ id: "conv-1", fromUomId: "uom-2", toUomId: "uom-1", conversionFactor: 12 });

    renderWithClient(<UomPanel companyId="co-1" />);

    await waitFor(() =>
      expect(within(screen.getByLabelText("From (e.g. purchase UOM)")).getAllByRole("option").length).toBeGreaterThan(1),
    );
    await user.selectOptions(screen.getByLabelText("From (e.g. purchase UOM)"), "uom-2");
    await user.selectOptions(screen.getByLabelText("To (e.g. stock UOM)"), "uom-1");
    await user.type(screen.getByLabelText("Factor"), "12");
    await user.click(screen.getByRole("button", { name: "Save conversion" }));

    await waitFor(() => expect(mockConfigureConversion).toHaveBeenCalledWith("co-1", "uom-2", "uom-1", 12));
  });

  it("shows existing conversions once a viewer UOM is selected", async () => {
    const user = userEvent.setup();
    mockListUoms.mockResolvedValue([nos, box]);
    mockConversionsFrom.mockResolvedValue([{ id: "conv-1", fromUomId: "uom-2", toUomId: "uom-1", conversionFactor: 12 }]);

    renderWithClient(<UomPanel companyId="co-1" />);

    await waitFor(() =>
      expect(within(screen.getByLabelText("View conversions from")).getAllByRole("option").length).toBeGreaterThan(1),
    );
    await user.selectOptions(screen.getByLabelText("View conversions from"), "uom-2");

    expect(await screen.findByText("1 BOX = 12 NOS")).toBeInTheDocument();
  });
});
