import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { HierarchicalCodeNamePanel, type HierarchicalCodeNameItem } from "./hierarchical-code-name-panel";

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const assets: HierarchicalCodeNameItem = { id: "cc-1", code: "HO", name: "Head Office", parentId: null, disabled: false };
const branch: HierarchicalCodeNameItem = { id: "cc-2", code: "BR1", name: "Branch 1", parentId: "cc-1", disabled: false };

describe("HierarchicalCodeNamePanel", () => {
  let list: ReturnType<typeof vi.fn<(companyId: string) => Promise<HierarchicalCodeNameItem[]>>>;
  let create: ReturnType<
    typeof vi.fn<
      (companyId: string, request: { code: string; name: string; parentId: string | null }) => Promise<HierarchicalCodeNameItem>
    >
  >;
  let rename: ReturnType<typeof vi.fn<(id: string, companyId: string, name: string) => Promise<HierarchicalCodeNameItem>>>;
  let disable: ReturnType<typeof vi.fn<(id: string, companyId: string) => Promise<void>>>;
  let enable: ReturnType<typeof vi.fn<(id: string, companyId: string) => Promise<void>>>;

  beforeEach(() => {
    list = vi.fn();
    create = vi.fn();
    rename = vi.fn();
    disable = vi.fn();
    enable = vi.fn();
  });

  function renderPanel() {
    return renderWithClient(
      <HierarchicalCodeNamePanel
        companyId="co-1"
        queryKey={["masterdata", "cost-centres", "co-1"]}
        itemLabel="cost centre"
        emptyMessage="No cost centres yet."
        list={list}
        create={create}
        rename={rename}
        disable={disable}
        enable={enable}
      />,
    );
  }

  it("renders children indented under their parent", async () => {
    list.mockResolvedValue([branch, assets]);

    renderPanel();

    const listEl = await screen.findByRole("list");
    const parentRow = within(listEl).getByText("HO — Head Office").closest("li");
    const childRow = within(listEl).getByText("BR1 — Branch 1").closest("li");
    expect(Number(childRow?.style.marginLeft.replace("px", ""))).toBeGreaterThan(
      Number(parentRow?.style.marginLeft.replace("px", "") || 0),
    );
  });

  it("creates a new node under the chosen parent", async () => {
    const user = userEvent.setup();
    list.mockResolvedValue([assets]);
    create.mockResolvedValue({ id: "cc-3", code: "BR2", name: "Branch 2", parentId: "cc-1", disabled: false });

    renderPanel();

    await waitFor(() => expect(screen.getByLabelText("Code")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Code"), "BR2");
    await user.type(screen.getByLabelText("Name"), "Branch 2");
    await user.selectOptions(screen.getByLabelText("Parent"), "cc-1");
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => expect(create).toHaveBeenCalledWith("co-1", { code: "BR2", name: "Branch 2", parentId: "cc-1" }));
  });

  it("toggles a node between active and disabled", async () => {
    const user = userEvent.setup();
    list.mockResolvedValue([assets]);
    disable.mockResolvedValue(undefined);

    renderPanel();

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("HO — Head Office")).toBeInTheDocument();
    await user.click(within(listEl).getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(disable).toHaveBeenCalledWith("cc-1", "co-1"));
  });

  it("renames without offering to change the parent", async () => {
    const user = userEvent.setup();
    list.mockResolvedValue([assets]);
    rename.mockResolvedValue({ ...assets, name: "Head Office (Renamed)" });

    renderPanel();

    const listEl = await screen.findByRole("list");
    expect(within(listEl).getByText("HO — Head Office")).toBeInTheDocument();
    await user.click(within(listEl).getByRole("button", { name: "Rename" }));

    expect(screen.queryByLabelText("Parent")).not.toBeInTheDocument();
    await user.clear(screen.getByLabelText("Name"));
    await user.type(screen.getByLabelText("Name"), "Head Office (Renamed)");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => expect(rename).toHaveBeenCalledWith("cc-1", "co-1", "Head Office (Renamed)"));
  });
});
