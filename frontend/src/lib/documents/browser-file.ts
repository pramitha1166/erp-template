/** F0.7.1/F0.7.4: turns an in-memory `Blob` (an attachment download, a rendered PDF) into a browser action. */

const UNITS = ["B", "KB", "MB", "GB"] as const;

/** F0.7.1: a human-readable file size, e.g. `1.4 MB`. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < UNITS.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(1)} ${UNITS[unitIndex]}`;
}

export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/** Opens a PDF in a new tab — lets the browser's own viewer handle printing, rather than forcing a download. */
export function openBlobInNewTab(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, "_blank", "noopener,noreferrer");
  // Revoked after a tick so the new tab has time to start loading it.
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
