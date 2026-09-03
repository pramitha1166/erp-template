import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { PrintFormatPanel } from "./print-format-panel";

const {
  mockList,
  mockCreate,
  mockRename,
  mockUpdateTemplate,
  mockSetDefault,
  mockDisable,
  mockEnable,
  mockRender,
  mockOpenBlobInNewTab,
} = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockCreate: vi.fn(),
  mockRename: vi.fn(),
  mockUpdateTemplate: vi.fn(),
  mockSetDefault: vi.fn(),
  mockDisable: vi.fn(),
  mockEnable: vi.fn(),
  mockRender: vi.fn(),
  mockOpenBlobInNewTab: vi.fn(),
}));

vi.mock("@/lib/api/documents-printformat-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/documents-printformat-api")>(
    "@/lib/api/documents-printformat-api",
  );
  return {
    ...actual,
    listPrintFormats: mockList,
    createPrintFormat: mockCreate,
    renamePrintFormat: mockRename,
    updatePrintFormatTemplate: mockUpdateTemplate,
    setDefaultPrintFormat: mockSetDefault,
    disablePrintFormat: mockDisable,
    enablePrintFormat: mockEnable,
    renderPrintFormat: mockRender,
  };
});

vi.mock("@/lib/documents/browser-file", async () => {
  const actual = await vi.importActual<typeof import("@/lib/documents/browser-file")>("@/lib/documents/browser-file");
  return { ...actual, openBlobInNewTab: mockOpenBlobInNewTab };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const existingFormat = {
  id: "pf1",
  documentType: "sales:invoice",
  name: "Standard",
  isDefault: true,
  templateContent: "<html/>",
  disabled: false,
};

describe("PrintFormatPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lists print formats and flags the default one", async () => {
    mockList.mockResolvedValue([existingFormat]);

    renderWithClient(<PrintFormatPanel companyId="c1" documentType="sales:invoice" />);

    await waitFor(() => expect(screen.getByText("Standard")).toBeInTheDocument());
    expect(screen.getByText("Default")).toBeInTheDocument();
  });

  it("creates a new print format", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([]);
    mockCreate.mockResolvedValue({ ...existingFormat, id: "pf2", name: "Alternate", isDefault: false });

    renderWithClient(<PrintFormatPanel companyId="c1" documentType="sales:invoice" />);

    await waitFor(() => expect(screen.getByLabelText("Name")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Name"), "Alternate");
    await user.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() =>
      expect(mockCreate).toHaveBeenCalledWith(
        "c1",
        expect.objectContaining({ documentType: "sales:invoice", name: "Alternate", makeDefault: false }),
      ),
    );
  });

  it("toggles an existing format between enabled and disabled", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([existingFormat]);
    mockDisable.mockResolvedValue(undefined);

    renderWithClient(<PrintFormatPanel companyId="c1" documentType="sales:invoice" />);

    await waitFor(() => expect(screen.getByText("Standard")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(mockDisable).toHaveBeenCalledWith("pf1"));
  });

  it("saves the draft and opens a rendered PDF preview", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([]);
    mockCreate.mockResolvedValue({ ...existingFormat, id: "pf3", name: "Preview me" });
    const pdfBlob = new Blob(["%PDF-"], { type: "application/pdf" });
    mockRender.mockResolvedValue(pdfBlob);

    renderWithClient(<PrintFormatPanel companyId="c1" documentType="sales:invoice" />);

    await waitFor(() => expect(screen.getByLabelText("Name")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Name"), "Preview me");
    await user.click(screen.getByRole("button", { name: "Save & preview PDF" }));

    await waitFor(() => expect(mockRender).toHaveBeenCalledWith("pf3", {}));
    expect(mockOpenBlobInNewTab).toHaveBeenCalledWith(pdfBlob);
  });

  it("rejects invalid JSON sample data without calling render", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([]);
    mockCreate.mockResolvedValue({ ...existingFormat, id: "pf4", name: "Bad sample" });

    renderWithClient(<PrintFormatPanel companyId="c1" documentType="sales:invoice" />);

    await waitFor(() => expect(screen.getByLabelText("Name")).toBeInTheDocument());
    await user.type(screen.getByLabelText("Name"), "Bad sample");
    await user.clear(screen.getByLabelText(/Sample data/));
    await user.type(screen.getByLabelText(/Sample data/), "{{not json");
    await user.click(screen.getByRole("button", { name: "Save & preview PDF" }));

    expect(await screen.findByText("Sample data must be valid JSON.")).toBeInTheDocument();
    expect(mockRender).not.toHaveBeenCalled();
  });
});
