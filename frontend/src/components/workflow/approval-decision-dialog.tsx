"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { approveTask, rejectTask, type TaskView } from "@/lib/api/workflow-api";

/** WF-6: rejection always requires a comment; approval's is optional. */
const decisionSchema = z.object({
  comment: z.string().optional(),
});

type DecisionValues = z.infer<typeof decisionSchema>;

export interface ApprovalDecisionDialogProps {
  task: TaskView;
  companyId: string;
  action: "approve" | "reject";
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDecided: () => void;
}

/** F0.4.4: the approve/reject action screen — a focused dialog rather than a full page, since a decision is one field and one submit. */
export function ApprovalDecisionDialog({
  task,
  companyId,
  action,
  open,
  onOpenChange,
  onDecided,
}: ApprovalDecisionDialogProps) {
  const [submitError, setSubmitError] = useState<string | null>(null);
  const isReject = action === "reject";

  const form = useForm<DecisionValues>({
    resolver: zodResolver(
      isReject
        ? decisionSchema.superRefine((values, ctx) => {
            if (!values.comment || values.comment.trim().length === 0) {
              ctx.addIssue({
                code: z.ZodIssueCode.custom,
                path: ["comment"],
                message: "A comment is required when rejecting.",
              });
            }
          })
        : decisionSchema,
    ),
    defaultValues: { comment: "" },
  });

  async function onSubmit(values: DecisionValues) {
    setSubmitError(null);
    try {
      if (isReject) {
        await rejectTask(task.id, companyId, values.comment!.trim());
      } else {
        await approveTask(task.id, companyId, values.comment?.trim() || undefined);
      }
      form.reset();
      onOpenChange(false);
      onDecided();
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.message : `Could not ${action} this task.`);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          form.reset();
          setSubmitError(null);
        }
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isReject ? "Reject task" : "Approve task"}</DialogTitle>
          <DialogDescription>
            {task.documentType} · document {task.documentId}
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <ValidatedTextField
              control={form.control}
              name="comment"
              label={isReject ? "Reason for rejection" : "Comment (optional)"}
            />
            {submitError && (
              <p role="alert" className="text-sm text-destructive">
                {submitError}
              </p>
            )}
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" variant={isReject ? "destructive" : "default"} disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? "Submitting…" : isReject ? "Reject" : "Approve"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
