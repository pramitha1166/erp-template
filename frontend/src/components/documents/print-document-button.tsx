"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import { renderDefaultPrintFormat } from "@/lib/api/documents-printformat-api";
import { openBlobInNewTab } from "@/lib/documents/browser-file";

export interface PrintDocumentButtonProps {
  companyId: string;
  /** `module:entity`, e.g. `sales:invoice` (DOC-5). */
  documentType: string;
  /** The data the document type's default print format template expects (its Thymeleaf variables). */
  model: Record<string, unknown>;
  className?: string;
}

/**
 * F0.7.4: renders `documentType`'s enabled default print format against `model` and opens the result in a new tab —
 * the browser's own PDF viewer then handles printing/saving, so this is both the "Print" and "Download PDF" action
 * from a single control.
 */
export function PrintDocumentButton({ companyId, documentType, model, className }: PrintDocumentButtonProps) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleClick() {
    setError(null);
    setIsPending(true);
    try {
      const pdf = await renderDefaultPrintFormat(companyId, documentType, model);
      openBlobInNewTab(pdf);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.status === 404
            ? "No default print format is configured for this document yet."
            : caught.message
          : "Could not render the document.",
      );
    } finally {
      setIsPending(false);
    }
  }

  return (
    <div className={className}>
      <Button type="button" variant="outline" disabled={isPending} onClick={handleClick}>
        {isPending ? "Rendering…" : "Print / Download PDF"}
      </Button>
      {error && (
        <p role="alert" className="mt-1 text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}
