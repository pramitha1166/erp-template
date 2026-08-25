import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { DocumentActionBar } from "./document-action-bar";

describe("DocumentActionBar", () => {
  it("shows only Submit for a DRAFT document", () => {
    render(
      <DocumentActionBar
        status="DRAFT"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        onAmend={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Submit" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancel" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Amend" })).not.toBeInTheDocument();
  });

  it("shows Cancel and Amend, but not Submit, for a SUBMITTED document", () => {
    render(
      <DocumentActionBar
        status="SUBMITTED"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        onAmend={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Amend" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Submit" })).not.toBeInTheDocument();
  });

  it("renders nothing for a terminal CANCELLED document", () => {
    const { container } = render(
      <DocumentActionBar
        status="CANCELLED"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        onAmend={vi.fn()}
      />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("omits an action the screen didn't wire up, even if the state machine allows it", () => {
    render(<DocumentActionBar status="SUBMITTED" onCancel={vi.fn()} />);

    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Amend" })).not.toBeInTheDocument();
  });

  it("calls the handler on click", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<DocumentActionBar status="DRAFT" onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "Submit" }));

    expect(onSubmit).toHaveBeenCalledOnce();
  });

  it("disables and relabels a pending action", () => {
    render(
      <DocumentActionBar
        status="DRAFT"
        onSubmit={vi.fn()}
        pending={{ submit: true }}
      />,
    );

    const button = screen.getByRole("button", { name: "Submitting…" });
    expect(button).toBeDisabled();
  });
});
