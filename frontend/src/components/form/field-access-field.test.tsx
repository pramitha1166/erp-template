import { render, screen } from "@testing-library/react";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";

import { Form } from "@/components/ui/form";
import { FieldAccessField } from "./field-access-field";
import type { FieldAccess } from "@/lib/api/iam-api";

interface EmployeeValues {
  salary: string;
}

function TestForm({ access }: { access: FieldAccess }) {
  const form = useForm<EmployeeValues>({ defaultValues: { salary: "50000" } });

  return (
    <Form {...form}>
      <form>
        <FieldAccessField control={form.control} name="salary" label="Salary" access={access} />
      </form>
    </Form>
  );
}

describe("FieldAccessField", () => {
  it("renders nothing for NONE access", () => {
    render(<TestForm access="NONE" />);

    expect(screen.queryByText("Salary")).not.toBeInTheDocument();
    expect(screen.queryByText("50000")).not.toBeInTheDocument();
  });

  it("renders the value as read-only text for READ access", () => {
    render(<TestForm access="READ" />);

    expect(screen.getByText("Salary")).toBeInTheDocument();
    expect(screen.getByText("50000")).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });

  it("renders an editable input for WRITE access", () => {
    render(<TestForm access="WRITE" />);

    const input = screen.getByLabelText("Salary");
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue("50000");
  });
});
