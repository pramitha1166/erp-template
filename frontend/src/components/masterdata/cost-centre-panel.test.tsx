import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { CostCentrePanel } from "./cost-centre-panel";

const { mockListCostCentres } = vi.hoisted(() => ({ mockListCostCentres: vi.fn() }));

vi.mock("@/lib/api/masterdata-costcentre-api", async () => {
  const actual =
    await vi.importActual<typeof import("@/lib/api/masterdata-costcentre-api")>("@/lib/api/masterdata-costcentre-api");
  return { ...actual, listCostCentres: mockListCostCentres };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("CostCentrePanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("wires the cost-centre API into the generic hierarchical panel", async () => {
    mockListCostCentres.mockResolvedValue([{ id: "cc-1", code: "HO", name: "Head Office", parentId: null, disabled: false }]);

    renderWithClient(<CostCentrePanel companyId="co-1" />);

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("HO — Head Office")).toBeInTheDocument();
    expect(mockListCostCentres).toHaveBeenCalledWith("co-1");
    expect(screen.getByText("cost centre tree")).toBeInTheDocument();
  });
});
