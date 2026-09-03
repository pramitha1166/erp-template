import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { AttachmentPanel } from "./attachment-panel";

const { mockList, mockUpload, mockDelete, mockDownload, mockDownloadBlob } = vi.hoisted(() => ({
  mockList: vi.fn(),
  mockUpload: vi.fn(),
  mockDelete: vi.fn(),
  mockDownload: vi.fn(),
  mockDownloadBlob: vi.fn(),
}));

vi.mock("@/lib/api/documents-attachment-api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/documents-attachment-api")>(
    "@/lib/api/documents-attachment-api",
  );
  return {
    ...actual,
    listAttachments: mockList,
    uploadAttachment: mockUpload,
    deleteAttachment: mockDelete,
    downloadAttachment: mockDownload,
  };
});

vi.mock("@/lib/documents/browser-file", async () => {
  const actual = await vi.importActual<typeof import("@/lib/documents/browser-file")>("@/lib/documents/browser-file");
  return { ...actual, downloadBlob: mockDownloadBlob };
});

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

const existingAttachment = {
  id: "a1",
  documentType: "sales:invoice",
  documentId: "d1",
  fileName: "invoice.pdf",
  contentType: "application/pdf",
  sizeBytes: 2048,
  checksumSha256: "abc",
  scanStatus: "CLEAN" as const,
  uploadedBy: "jane@example.com",
  uploadedAt: "2026-09-01T00:00:00Z",
};

describe("AttachmentPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lists attachments with their scan status", async () => {
    mockList.mockResolvedValue([existingAttachment]);

    renderWithClient(<AttachmentPanel companyId="c1" documentType="sales:invoice" documentId="d1" />);

    await waitFor(() => expect(screen.getByText("invoice.pdf")).toBeInTheDocument());
    expect(screen.getByText("Clean")).toBeInTheDocument();
    expect(screen.getByText(/uploaded by jane@example.com/)).toBeInTheDocument();
  });

  it("shows an empty message when there are no attachments", async () => {
    mockList.mockResolvedValue([]);

    renderWithClient(<AttachmentPanel companyId="c1" documentType="sales:invoice" documentId="d1" />);

    await waitFor(() => expect(screen.getByText("No attachments yet.")).toBeInTheDocument());
  });

  it("uploads a selected file", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([]);
    mockUpload.mockResolvedValue({ ...existingAttachment, id: "a2" });
    const file = new File(["hello"], "hello.txt", { type: "text/plain" });

    renderWithClient(<AttachmentPanel companyId="c1" documentType="sales:invoice" documentId="d1" />);

    await waitFor(() => expect(screen.getByLabelText("Upload attachment")).toBeInTheDocument());
    await user.upload(screen.getByLabelText("Upload attachment"), file);

    await waitFor(() => expect(mockUpload).toHaveBeenCalledWith("c1", "sales:invoice", "d1", file));
  });

  it("deletes an attachment", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([existingAttachment]);
    mockDelete.mockResolvedValue(undefined);

    renderWithClient(<AttachmentPanel companyId="c1" documentType="sales:invoice" documentId="d1" />);

    await waitFor(() => expect(screen.getByText("invoice.pdf")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(mockDelete).toHaveBeenCalledWith("a1"));
  });

  it("downloads an attachment", async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue([existingAttachment]);
    const blob = new Blob(["content"]);
    mockDownload.mockResolvedValue(blob);

    renderWithClient(<AttachmentPanel companyId="c1" documentType="sales:invoice" documentId="d1" />);

    await waitFor(() => expect(screen.getByText("invoice.pdf")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Download" }));

    await waitFor(() => expect(mockDownload).toHaveBeenCalledWith("a1"));
    expect(mockDownloadBlob).toHaveBeenCalledWith(blob, "invoice.pdf");
  });
});
