"use client";

import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/http";
import {
  deleteAttachment,
  downloadAttachment,
  listAttachments,
  uploadAttachment,
  type AttachmentView,
} from "@/lib/api/documents-attachment-api";
import { downloadBlob, formatFileSize } from "@/lib/documents/browser-file";
import { AttachmentScanStatusBadge } from "./attachment-scan-status-badge";

export interface AttachmentPanelProps {
  companyId: string;
  /** `module:entity` — the document this panel's files belong to, e.g. `sales:invoice` (DOC-5). */
  documentType: string;
  documentId: string;
}

/**
 * F0.7.1 / F0.7.5: a generic file-attachment panel — upload (drag-and-drop or browse), list, download, delete —
 * meant to be dropped onto any document detail view once a business module has one. Every action here goes through
 * the ordinary REST calls in `documents-attachment-api.ts`; permission is enforced server-side against the parent
 * document's own permission codes (DOC-5), so there is no separate client-side permission check to duplicate.
 */
export function AttachmentPanel({ companyId, documentType, documentId }: AttachmentPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["documents", "attachments", companyId, documentType, documentId];
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDraggingOver, setIsDraggingOver] = useState(false);

  const {
    data: attachments,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: () => listAttachments(companyId, documentType, documentId),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadAttachment(companyId, documentType, documentId, file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const deleteMutation = useMutation({
    mutationFn: (attachmentId: string) => deleteAttachment(attachmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  async function handleDownload(attachment: AttachmentView) {
    setDownloadError(null);
    setDownloadingId(attachment.id);
    try {
      const blob = await downloadAttachment(attachment.id);
      downloadBlob(blob, attachment.fileName);
    } catch (error) {
      setDownloadError(error instanceof ApiError ? error.message : "Could not download the attachment.");
    } finally {
      setDownloadingId(null);
    }
  }

  function handleFiles(files: FileList | null) {
    const file = files?.[0];
    if (file) {
      uploadMutation.mutate(file);
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-4">
      <h2 className="text-sm font-semibold">Attachments</h2>

      <div
        className={`flex flex-col items-center gap-2 rounded-md border border-dashed p-6 text-center text-sm transition-colors ${
          isDraggingOver ? "border-primary bg-primary/5" : "border-muted-foreground/30"
        }`}
        onDragOver={(event) => {
          event.preventDefault();
          setIsDraggingOver(true);
        }}
        onDragLeave={() => setIsDraggingOver(false)}
        onDrop={(event) => {
          event.preventDefault();
          setIsDraggingOver(false);
          handleFiles(event.dataTransfer.files);
        }}
      >
        <p className="text-muted-foreground">Drag and drop a file here, or</p>
        <Button
          type="button"
          size="sm"
          variant="outline"
          disabled={uploadMutation.isPending}
          onClick={() => fileInputRef.current?.click()}
        >
          {uploadMutation.isPending ? "Uploading…" : "Browse files"}
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          aria-label="Upload attachment"
          className="hidden"
          onChange={(event) => {
            handleFiles(event.target.files);
            event.target.value = "";
          }}
        />
      </div>

      {uploadMutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {uploadMutation.error instanceof ApiError ? uploadMutation.error.message : "Could not upload the file."}
        </p>
      )}
      {downloadError && (
        <p role="alert" className="text-sm text-destructive">
          {downloadError}
        </p>
      )}

      {isLoading && <p className="text-sm text-muted-foreground">Loading attachments…</p>}
      {isError && (
        <p role="alert" className="text-sm text-destructive">
          Could not load attachments.
        </p>
      )}
      {attachments && attachments.length === 0 && (
        <p className="text-sm text-muted-foreground">No attachments yet.</p>
      )}
      {attachments && attachments.length > 0 && (
        <ul className="flex flex-col gap-2">
          {attachments.map((attachment) => (
            <li
              key={attachment.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
            >
              <div className="flex flex-col">
                <span className="font-medium">{attachment.fileName}</span>
                <span className="text-xs text-muted-foreground">
                  {formatFileSize(attachment.sizeBytes)} · {attachment.contentType}
                  {attachment.uploadedBy ? ` · uploaded by ${attachment.uploadedBy}` : ""}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <AttachmentScanStatusBadge status={attachment.scanStatus} />
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={downloadingId === attachment.id}
                  onClick={() => handleDownload(attachment)}
                >
                  {downloadingId === attachment.id ? "Downloading…" : "Download"}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate(attachment.id)}
                >
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
