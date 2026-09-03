import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api/http";
import { PrintDocumentButton } from "./print-document-button";

const { mockRenderDefault, mockOpenBlobInNewTab } = vi.hoisted(() => ({
  mockRenderDefault: vi.fn(),
  mockOpenBlobInNewTab: vi.fn(),
}));

vi.mock("@/lib/api/documents-printformat-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/documents-printformat-api")>(
    "@/lib/api/documents-printformat-api",
  );
  return { ...actual, renderDefaultPrintFormat: mockRenderDefault };
});

vi.mock("@/lib/documents/browser-file", async () => {
  const actual = await vi.importActual<typeof import("@/lib/documents/browser-file")>("@/lib/documents/browser-file");
  return { ...actual, openBlobInNewTab: mockOpenBlobInNewTab };
});

describe("PrintDocumentButton", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the default print format and opens it", async () => {
    const user = userEvent.setup();
    const pdfBlob = new Blob(["%PDF-"], { type: "application/pdf" });
    mockRenderDefault.mockResolvedValue(pdfBlob);

    render(<PrintDocumentButton companyId="c1" documentType="sales:invoice" model={{ invoiceNumber: "INV-1" }} />);

    await user.click(screen.getByRole("button", { name: "Print / Download PDF" }));

    await waitFor(() =>
      expect(mockRenderDefault).toHaveBeenCalledWith("c1", "sales:invoice", { invoiceNumber: "INV-1" }),
    );
    expect(mockOpenBlobInNewTab).toHaveBeenCalledWith(pdfBlob);
  });

  it("shows a helpful message when no default print format is configured", async () => {
    const user = userEvent.setup();
    mockRenderDefault.mockRejectedValue(new ApiError(404, "No default print format configured"));

    render(<PrintDocumentButton companyId="c1" documentType="sales:invoice" model={{}} />);

    await user.click(screen.getByRole("button", { name: "Print / Download PDF" }));

    expect(await screen.findByText("No default print format is configured for this document yet.")).toBeInTheDocument();
    expect(mockOpenBlobInNewTab).not.toHaveBeenCalled();
  });
});
