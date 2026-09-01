"use client";

import { useState } from "react";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RequireCompany } from "@/components/admin/require-company";
import { ApprovalChainList } from "@/components/workflow/approval-chain-list";

/** F0.4.1 / WF-1: approval chain configuration screen, per document type per company. */
export default function WorkflowChainsAdminPage() {
  const [documentType, setDocumentType] = useState("");

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Approval chains</h1>
        <p className="text-sm text-muted-foreground">
          Configure who approves each document type, in what order, and under which conditions.
        </p>
      </div>

      <RequireCompany>
        {(companyId) => (
          <div className="flex flex-col gap-6">
            <div className="flex max-w-xs flex-col gap-2">
              <Label htmlFor="workflow-document-type">Document type</Label>
              <Input
                id="workflow-document-type"
                placeholder="e.g. PURCHASE_ORDER"
                value={documentType}
                onChange={(event) => setDocumentType(event.target.value)}
              />
            </div>

            {documentType ? (
              <ApprovalChainList companyId={companyId} documentType={documentType} />
            ) : (
              <p className="text-sm text-muted-foreground">
                Enter a document type to view or configure its approval chains.
              </p>
            )}
          </div>
        )}
      </RequireCompany>
    </div>
  );
}
