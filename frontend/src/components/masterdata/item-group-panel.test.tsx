import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ItemGroupPanel } from "./item-group-panel";

const { mockListItemGroups } = vi.hoisted(() => ({ mockListItemGroups: vi.fn() }));

vi.mock("@/lib/api/masterdata-item-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/masterdata-item-api")>("@/lib/api/masterdata-item-api");
  return { ...actual, listItemGroups: mockListItemGroups };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ItemGroupPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("wires the item-group API into the generic hierarchical panel", async () => {
    mockListItemGroups.mockResolvedValue([{ id: "ig-1", code: "RAW", name: "Raw Materials", parentId: null, disabled: false }]);

    renderWithClient(<ItemGroupPanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("RAW — Raw Materials")).toBeInTheDocument();
    expect(mockListItemGroups).toHaveBeenCalledWith("co-1");
  });
});
