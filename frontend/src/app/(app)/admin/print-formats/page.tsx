"use client";

import { useState } from "react";

import { RequireCompany } from "@/components/admin/require-company";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PrintFormatPanel } from "@/components/documents/print-format-panel";

/** F0.7.2 / F0.7.3: print-format template administration, one document type at a time. */
export default function PrintFormatsAdminPage() {
  const [documentType, setDocumentType] = useState("");

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Print formats</h1>
        <p className="text-sm text-muted-foreground">
          Configure the print-format templates each document type renders to PDF from (DOC-2/DOC-3).
        </p>
      </div>

      <div className="flex flex-col gap-1 max-w-sm">
        <Label htmlFor="document-type">Document type</Label>
        <Input
          id="document-type"
          placeholder="e.g. sales:invoice"
          value={documentType}
          onChange={(event) => setDocumentType(event.target.value)}
        />
        <p className="text-xs text-muted-foreground">
          The <code>module:entity</code> a business module registers its documents under.
        </p>
      </div>

      <RequireCompany>
        {(companyId) =>
          documentType.trim() ? (
            <PrintFormatPanel companyId={companyId} documentType={documentType.trim()} />
          ) : (
            <p className="text-sm text-muted-foreground">Enter a document type above to manage its print formats.</p>
          )
        }
      </RequireCompany>
    </div>
  );
}
