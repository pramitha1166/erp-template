"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { canTransition, type DocStatus } from "@/lib/documents/status";

export interface DocumentActionBarProps {
  status: DocStatus;
  onSubmit?: () => void;
  onCancel?: () => void;
  onAmend?: () => void;
  /** Per-action pending flags — disables that action and swaps its label. */
  pending?: Partial<Record<"submit" | "cancel" | "amend", boolean>>;
  className?: string;
}

/**
 * F0.1.1 / ARCH-4: renders only the actions the ARCH-4 state machine
 * allows from the document's current status, and only when the screen
 * supplied a handler for that action — a screen that never lets a
 * document be amended simply doesn't pass `onAmend`.
 */
export function DocumentActionBar({
  status,
  onSubmit,
  onCancel,
  onAmend,
  pending = {},
  className,
}: DocumentActionBarProps) {
  const actions: Array<{
    action: "submit" | "cancel" | "amend";
    label: string;
    pendingLabel: string;
    handler?: () => void;
    variant?: "default" | "outline" | "destructive";
  }> = [
    {
      action: "submit",
      label: "Submit",
      pendingLabel: "Submitting…",
      handler: onSubmit,
    },
    {
      action: "amend",
      label: "Amend",
      pendingLabel: "Amending…",
      handler: onAmend,
      variant: "outline",
    },
    {
      action: "cancel",
      label: "Cancel",
      pendingLabel: "Cancelling…",
      handler: onCancel,
      variant: "destructive",
    },
  ];

  const visible = actions.filter(
    ({ action, handler }) => handler && canTransition(status, action),
  );

  if (visible.length === 0) {
    return null;
  }

  return (
    <div className={cn("flex items-center gap-2", className)}>
      {visible.map(({ action, label, pendingLabel, handler, variant }) => (
        <Button
          key={action}
          type="button"
          variant={variant}
          disabled={pending[action]}
          onClick={handler}
        >
          {pending[action] ? pendingLabel : label}
        </Button>
      ))}
    </div>
  );
}
