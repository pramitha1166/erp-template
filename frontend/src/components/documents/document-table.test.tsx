import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { DocumentTable, type DocumentTableColumn } from "./document-table";

interface Invoice {
  id: string;
  docNumber: string;
  amount: string;
}

const columns: DocumentTableColumn<Invoice>[] = [
  { id: "docNumber", header: "Document #", cell: (row) => row.docNumber, sortable: true },
  { id: "amount", header: "Amount", cell: (row) => row.amount },
];

const rows: Invoice[] = [
  { id: "1", docNumber: "INV-0001", amount: "1,000.00" },
  { id: "2", docNumber: "INV-0002", amount: "2,500.00" },
];

describe("DocumentTable", () => {
  it("renders one page of rows with the given columns", () => {
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={2}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
      />,
    );

    expect(screen.getByText("INV-0001")).toBeInTheDocument();
    expect(screen.getByText("2,500.00")).toBeInTheDocument();
    expect(screen.getByText("1–2 of 2")).toBeInTheDocument();
  });

  it("shows the empty message when there are no rows", () => {
    render(
      <DocumentTable
        columns={columns}
        rows={[]}
        rowKey={(row) => row.id}
        totalCount={0}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
        emptyMessage="Nothing here yet."
      />,
    );

    expect(screen.getByText("Nothing here yet.")).toBeInTheDocument();
  });

  it("disables Previous on the first page and Next on the last page", () => {
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={2}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  it("calls onPageChange with the next page", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={40}
        page={0}
        pageSize={20}
        onPageChange={onPageChange}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Next" }));

    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it("cycles a sortable column header through ascending and descending", async () => {
    const user = userEvent.setup();
    const onSortChange = vi.fn();
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={2}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
        onSortChange={onSortChange}
      />,
    );

    await user.click(screen.getByRole("button", { name: /Document #/ }));

    expect(onSortChange).toHaveBeenCalledWith({ columnId: "docNumber", direction: "asc" });
  });

  it("flips sort direction when the same column is clicked again", async () => {
    const user = userEvent.setup();
    const onSortChange = vi.fn();
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={2}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
        sort={{ columnId: "docNumber", direction: "asc" }}
        onSortChange={onSortChange}
      />,
    );

    await user.click(screen.getByRole("button", { name: /Document #/ }));

    expect(onSortChange).toHaveBeenCalledWith({ columnId: "docNumber", direction: "desc" });
  });

  it("reports a status filter change", async () => {
    const user = userEvent.setup();
    const onStatusFilterChange = vi.fn();
    render(
      <DocumentTable
        columns={columns}
        rows={rows}
        rowKey={(row) => row.id}
        totalCount={2}
        page={0}
        pageSize={20}
        onPageChange={vi.fn()}
        statusFilter="ALL"
        onStatusFilterChange={onStatusFilterChange}
      />,
    );

    await user.selectOptions(screen.getByLabelText("Status"), "SUBMITTED");

    expect(onStatusFilterChange).toHaveBeenCalledWith("SUBMITTED");
  });
});
