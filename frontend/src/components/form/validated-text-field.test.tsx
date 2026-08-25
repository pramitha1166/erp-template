import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "./validated-text-field";

const schema = z.object({
  docNumber: z.string().min(1, "Document number is required"),
});

type FormValues = z.infer<typeof schema>;

function TestForm({ onSubmit }: { onSubmit: (values: FormValues) => void }) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { docNumber: "" },
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)}>
        <ValidatedTextField control={form.control} name="docNumber" label="Document number" />
        <Button type="submit">Save</Button>
      </form>
    </Form>
  );
}

describe("ValidatedTextField", () => {
  it("surfaces the field's own Zod error inline, next to the field, on submit", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<TestForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: "Save" }));

    const input = screen.getByLabelText("Document number");
    const error = await screen.findByText("Document number is required");

    expect(onSubmit).not.toHaveBeenCalled();
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input.getAttribute("aria-describedby")).toContain(error.id);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("submits and clears the error once the field is valid", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<TestForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText("Document number"), "INV-0001");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).toHaveBeenCalledWith(
      { docNumber: "INV-0001" },
      expect.anything(),
    );
    expect(screen.queryByText("Document number is required")).not.toBeInTheDocument();
  });
});
