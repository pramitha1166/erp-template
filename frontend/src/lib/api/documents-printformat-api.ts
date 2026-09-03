import { apiFetch, authHeaders, baseUrl, parseError } from "./http";

/** Mirrors `PrintFormatController.PrintFormatView` (DOC-2). */
export interface PrintFormatView {
  id: string;
  documentType: string;
  name: string;
  isDefault: boolean;
  templateContent: string;
  disabled: boolean;
}

export interface NewPrintFormatRequest {
  documentType: string;
  name: string;
  templateContent: string;
  makeDefault: boolean;
}

export function listPrintFormats(companyId: string, documentType: string): Promise<PrintFormatView[]> {
  return apiFetch<PrintFormatView[]>("/documents/print-formats", { query: { companyId, documentType } });
}

export function createPrintFormat(companyId: string, request: NewPrintFormatRequest): Promise<PrintFormatView> {
  return apiFetch<PrintFormatView>("/documents/print-formats", { method: "POST", query: { companyId }, body: request });
}

export function renamePrintFormat(printFormatId: string, name: string): Promise<PrintFormatView> {
  return apiFetch<PrintFormatView>(`/documents/print-formats/${printFormatId}`, { method: "PUT", body: { name } });
}

export function updatePrintFormatTemplate(printFormatId: string, templateContent: string): Promise<PrintFormatView> {
  return apiFetch<PrintFormatView>(`/documents/print-formats/${printFormatId}/template`, {
    method: "PUT",
    body: { templateContent },
  });
}

export function setDefaultPrintFormat(printFormatId: string): Promise<PrintFormatView> {
  return apiFetch<PrintFormatView>(`/documents/print-formats/${printFormatId}/default`, { method: "POST" });
}

export function disablePrintFormat(printFormatId: string): Promise<void> {
  return apiFetch<void>(`/documents/print-formats/${printFormatId}/disable`, { method: "POST" });
}

export function enablePrintFormat(printFormatId: string): Promise<void> {
  return apiFetch<void>(`/documents/print-formats/${printFormatId}/enable`, { method: "POST" });
}

/**
 * DOC-3: renders `printFormatId` against `model` (arbitrary Thymeleaf variables the template references) and
 * returns the PDF bytes. A binary response body and a caller-shaped JSON payload, so — like the attachment
 * upload/download calls — this bypasses `apiFetch` and builds the request by hand.
 */
export async function renderPrintFormat(printFormatId: string, model: Record<string, unknown>): Promise<Blob> {
  return postForPdf(`${baseUrl}/documents/print-formats/${printFormatId}/render`, model);
}

/** DOC-3: renders using the enabled default print format for `companyId` + `documentType`. */
export async function renderDefaultPrintFormat(
  companyId: string,
  documentType: string,
  model: Record<string, unknown>,
): Promise<Blob> {
  const url = new URL(`${baseUrl}/documents/print-formats/render-default`);
  url.searchParams.set("companyId", companyId);
  url.searchParams.set("documentType", documentType);
  return postForPdf(url.toString(), model);
}

async function postForPdf(url: string, model: Record<string, unknown>): Promise<Blob> {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(model),
  });
  if (!response.ok) {
    throw await parseError(response);
  }
  return response.blob();
}
