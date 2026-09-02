import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ItemPanel } from "./item-panel";

const { mockListItems, mockCreateItem, mockUpdateItem, mockDisableItem, mockListItemGroups, mockListUoms } = vi.hoisted(
  () => ({
    mockListItems: vi.fn(),
    mockCreateItem: vi.fn(),
    mockUpdateItem: vi.fn(),
    mockDisableItem: vi.fn(),
    mockListItemGroups: vi.fn(),
    mockListUoms: vi.fn(),
  }),
);

vi.mock("@/lib/api/masterdata-item-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-item-api")>("@/lib/api/masterdata-item-api");
  return {
    ...actual,
    listItems: mockListItems,
    createItem: mockCreateItem,
    updateItem: mockUpdateItem,
    disableItem: mockDisableItem,
    listItemGroups: mockListItemGroups,
  };
});

vi.mock("@/lib/api/masterdata-uom-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-uom-api")>("@/lib/api/masterdata-uom-api");
  return { ...actual, listUoms: mockListUoms };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const raw = { id: "ig-1", code: "RAW", name: "Raw Materials", parentId: null, disabled: false };
const nos = { id: "uom-1", code: "NOS", name: "Numbers", disabled: false };
const box = { id: "uom-2", code: "BOX", name: "Box", disabled: false };

const widget = {
  id: "item-1",
  code: "ITEM-001",
  name: "Widget",
  itemGroupId: "ig-1",
  stockUomId: "uom-1",
  purchaseUomId: null,
  valuationMethod: "FIFO" as const,
  reorderLevel: 10,
  batchTracked: false,
  serialTracked: false,
  taxCategoryCode: null,
  hsCode: null,
  disabled: false,
};

describe("ItemPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListItemGroups.mockResolvedValue([raw]);
    mockListUoms.mockResolvedValue([nos, box]);
  });

  it("lists items with their valuation method and reorder level", async () => {
    mockListItems.mockResolvedValue([widget]);

    renderWithClient(<ItemPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("ITEM-001 — Widget")).toBeInTheDocument());
    expect(screen.getByText(/reorder at 10/)).toBeInTheDocument();
  });

  it("creates an item with a stock UOM and item group", async () => {
    const user = userEvent.setup();
    mockListItems.mockResolvedValue([]);
    mockCreateItem.mockResolvedValue(widget);

    renderWithClient(<ItemPanel companyId="co-1" />);

    await user.click(await screen.findByRole("button", { name: "Add an item" }));
    await user.type(screen.getByLabelText("Code"), "ITEM-002");
    await user.type(screen.getByLabelText("Name"), "Bolt");
    await user.selectOptions(screen.getByLabelText("Item group"), "ig-1");
    await user.selectOptions(screen.getByLabelText("Stock UOM"), "uom-1");
    await user.click(screen.getByRole("button", { name: "Create item" }));

    await waitFor(() =>
      expect(mockCreateItem).toHaveBeenCalledWith("co-1", {
        code: "ITEM-002",
        name: "Bolt",
        itemGroupId: "ig-1",
        stockUomId: "uom-1",
        valuationMethod: "FIFO",
      }),
    );
  });

  it("edits an item, including a distinct purchase UOM", async () => {
    const user = userEvent.setup();
    mockListItems.mockResolvedValue([widget]);
    mockUpdateItem.mockResolvedValue({ ...widget, purchaseUomId: "uom-2" });

    renderWithClient(<ItemPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("ITEM-001 — Widget")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Edit" }));
    await user.selectOptions(screen.getByLabelText("Purchase UOM"), "uom-2");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() =>
      expect(mockUpdateItem).toHaveBeenCalledWith(
        "item-1",
        "co-1",
        expect.objectContaining({ purchaseUomId: "uom-2" }),
      ),
    );
  });

  it("toggles an item between active and disabled", async () => {
    const user = userEvent.setup();
    mockListItems.mockResolvedValue([widget]);
    mockDisableItem.mockResolvedValue(undefined);

    renderWithClient(<ItemPanel companyId="co-1" />);

    await waitFor(() => expect(screen.getByText("ITEM-001 — Widget")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisableItem).toHaveBeenCalledWith("item-1", "co-1"));
  });
});
