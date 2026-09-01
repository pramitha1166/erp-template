import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ApprovalDecisionDialog } from "./approval-decision-dialog";
import type { TaskView } from "@/lib/api/workflow-api";

const { mockApproveTask, mockRejectTask } = vi.hoisted(() => ({
  mockApproveTask: vi.fn(),
  mockRejectTask: vi.fn(),
}));

vi.mock("@/lib/api/workflow-api", () => ({
  approveTask: mockApproveTask,
  rejectTask: mockRejectTask,
}));

const task: TaskView = {
  id: "task-1",
  instanceId: "inst-1",
  documentType: "PURCHASE_ORDER",
  documentId: "PO-1",
  status: "PENDING",
  dueAt: null,
};

describe("ApprovalDecisionDialog", () => {
  it("blocks rejection without a comment (WF-6)", async () => {
    const user = userEvent.setup();
    const onDecided = vi.fn();

    render(
      <ApprovalDecisionDialog
        task={task}
        companyId="co-1"
        action="reject"
        open={true}
        onOpenChange={() => {}}
        onDecided={onDecided}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Reject" }));

    expect(await screen.findByText("A comment is required when rejecting.")).toBeInTheDocument();
    expect(mockRejectTask).not.toHaveBeenCalled();
    expect(onDecided).not.toHaveBeenCalled();
  });

  it("submits the rejection once a comment is entered", async () => {
    const user = userEvent.setup();
    mockRejectTask.mockResolvedValue(undefined);
    const onDecided = vi.fn();
    const onOpenChange = vi.fn();

    render(
      <ApprovalDecisionDialog
        task={task}
        companyId="co-1"
        action="reject"
        open={true}
        onOpenChange={onOpenChange}
        onDecided={onDecided}
      />,
    );

    await user.type(screen.getByLabelText("Reason for rejection"), "Missing budget sign-off");
    await user.click(screen.getByRole("button", { name: "Reject" }));

    await vi.waitFor(() => expect(mockRejectTask).toHaveBeenCalledWith("task-1", "co-1", "Missing budget sign-off"));
    expect(onDecided).toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("submits an approval without requiring a comment", async () => {
    const user = userEvent.setup();
    mockApproveTask.mockResolvedValue(undefined);
    const onDecided = vi.fn();

    render(
      <ApprovalDecisionDialog
        task={task}
        companyId="co-1"
        action="approve"
        open={true}
        onOpenChange={() => {}}
        onDecided={onDecided}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Approve" }));

    await vi.waitFor(() => expect(mockApproveTask).toHaveBeenCalledWith("task-1", "co-1", undefined));
    expect(onDecided).toHaveBeenCalled();
  });
});
