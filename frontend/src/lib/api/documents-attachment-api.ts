import { apiFetch, authHeaders, baseUrl, parseError } from "./http";

/** Mirrors `ScanStatus` (DOC-4). */
export type ScanStatus = "PENDING" | "CLEAN" | "INFECTED" | "FAILED";

/** Mirrors `AttachmentController.AttachmentView` (DOC-1). */
export interface AttachmentView {
  id: string;
  documentType: string;
  documentId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  checksumSha256: string;
  scanStatus: ScanStatus;
  uploadedBy: string | null;
  uploadedAt: string | null;
}

export function listAttachments(
  companyId: string,
  documentType: string,
  documentId: string,
): Promise<AttachmentView[]> {
  return apiFetch<AttachmentView[]>("/documents/attachments", { query: { companyId, documentType, documentId } });
}

/**
 * DOC-1: uploads `file` against a document. A multipart request body, so this bypasses `apiFetch` (which always
 * JSON-encodes) and builds the request by hand, same auth/error-parsing rules as `apiFetch` reused from `http.ts`.
 */
export async function uploadAttachment(
  companyId: string,
  documentType: string,
  documentId: string,
  file: File,
): Promise<AttachmentView> {
  const url = new URL(`${baseUrl}/documents/attachments`);
  url.searchParams.set("companyId", companyId);
  url.searchParams.set("documentType", documentType);
  url.searchParams.set("documentId", documentId);

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(url.toString(), {
    method: "POST",
    headers: authHeaders(),
    body: formData,
  });
  if (!response.ok) {
    throw await parseError(response);
  }
  return (await response.json()) as AttachmentView;
}

export function deleteAttachment(attachmentId: string): Promise<void> {
  return apiFetch<void>(`/documents/attachments/${attachmentId}`, { method: "DELETE" });
}

/** DOC-1: fetches the raw file bytes. A binary response body, so this bypasses `apiFetch` the same way upload does. */
export async function downloadAttachment(attachmentId: string): Promise<Blob> {
  const response = await fetch(`${baseUrl}/documents/attachments/${attachmentId}/content`, {
    headers: authHeaders(),
  });
  if (!response.ok) {
    throw await parseError(response);
  }
  return response.blob();
}
