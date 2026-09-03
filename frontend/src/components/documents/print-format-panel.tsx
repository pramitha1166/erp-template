"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormLabel } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ValidatedTextareaField } from "@/components/form/validated-textarea-field";
import { DisableToggleButton } from "@/components/masterdata/disable-toggle-button";
import { ApiError } from "@/lib/api/http";
import {
  createPrintFormat,
  disablePrintFormat,
  enablePrintFormat,
  listPrintFormats,
  renamePrintFormat,
  renderPrintFormat,
  setDefaultPrintFormat,
  updatePrintFormatTemplate,
  type PrintFormatView,
} from "@/lib/api/documents-printformat-api";
import { openBlobInNewTab } from "@/lib/documents/browser-file";

const printFormatSchema = z.object({
  name: z.string().min(1, "Name is required"),
  templateContent: z.string().min(1, "Template content is required"),
  makeDefault: z.boolean(),
});

type PrintFormatFormValues = z.infer<typeof printFormatSchema>;

const emptyFormValues: PrintFormatFormValues = {
  name: "",
  templateContent: "",
  makeDefault: false,
};

const SAMPLE_TEMPLATE = `<html xmlns:th="http://www.thymeleaf.org">
  <body>
    <h1>Invoice <span th:text="\${invoiceNumber}">INV-0001</span></h1>
  </body>
</html>`;

function toFormValues(printFormat: PrintFormatView): PrintFormatFormValues {
  return { name: printFormat.name, templateContent: printFormat.templateContent, makeDefault: printFormat.isDefault };
}

export interface PrintFormatPanelProps {
  companyId: string;
  documentType: string;
}

/**
 * F0.7.2 / F0.7.3: print-format template administration for one document type — create/rename/set-default/
 * enable-disable, plus a "Save & preview" action that renders the current draft (against caller-supplied sample
 * data) to a real PDF and opens it in a new tab, so an edit can be checked without leaving the page.
 */
export function PrintFormatPanel({ companyId, documentType }: PrintFormatPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["documents", "print-formats", companyId, documentType];
  const [editingFormat, setEditingFormat] = useState<PrintFormatView | null>(null);
  const [sampleDataText, setSampleDataText] = useState("{}");
  const [previewError, setPreviewError] = useState<string | null>(null);

  const {
    data: printFormats,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: () => listPrintFormats(companyId, documentType),
  });

  const form = useForm<PrintFormatFormValues>({
    resolver: zodResolver(printFormatSchema),
    defaultValues: { ...emptyFormValues, templateContent: SAMPLE_TEMPLATE },
  });

  async function save(values: PrintFormatFormValues): Promise<PrintFormatView> {
    if (editingFormat) {
      await renamePrintFormat(editingFormat.id, values.name);
      const updated = await updatePrintFormatTemplate(editingFormat.id, values.templateContent);
      return values.makeDefault && !updated.isDefault ? setDefaultPrintFormat(updated.id) : updated;
    }
    return createPrintFormat(companyId, {
      documentType,
      name: values.name,
      templateContent: values.templateContent,
      makeDefault: values.makeDefault,
    });
  }

  const saveMutation = useMutation({
    mutationFn: save,
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey });
      setEditingFormat(saved);
      form.reset(toFormValues(saved));
    },
  });

  const previewMutation = useMutation({
    mutationFn: async (values: PrintFormatFormValues) => {
      setPreviewError(null);
      let model: Record<string, unknown>;
      try {
        model = sampleDataText.trim() === "" ? {} : (JSON.parse(sampleDataText) as Record<string, unknown>);
      } catch {
        throw new Error("Sample data must be valid JSON.");
      }
      const saved = await save(values);
      const pdf = await renderPrintFormat(saved.id, model);
      return { saved, pdf };
    },
    onSuccess: ({ saved, pdf }) => {
      queryClient.invalidateQueries({ queryKey });
      setEditingFormat(saved);
      form.reset(toFormValues(saved));
      openBlobInNewTab(pdf);
    },
    onError: (error) => {
      setPreviewError(error instanceof ApiError ? error.message : error instanceof Error ? error.message : "Could not render a preview.");
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: PrintFormatView) => (target.disabled ? enablePrintFormat(target.id) : disablePrintFormat(target.id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  const setDefaultMutation = useMutation({
    mutationFn: (target: PrintFormatView) => setDefaultPrintFormat(target.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEditing(target: PrintFormatView) {
    setEditingFormat(target);
    form.reset(toFormValues(target));
  }

  function startCreating() {
    setEditingFormat(null);
    form.reset({ ...emptyFormValues, templateContent: SAMPLE_TEMPLATE });
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Print formats — {documentType}</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading print formats…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load print formats.
          </p>
        )}
        {printFormats && printFormats.length === 0 && (
          <p className="text-sm text-muted-foreground">No print formats configured yet for this document type.</p>
        )}
        {printFormats && printFormats.length > 0 && (
          <ul className="flex flex-col gap-2">
            {printFormats.map((item) => (
              <li
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex items-center gap-2">
                  <span className="font-medium">{item.name}</span>
                  {item.isDefault && <Badge>Default</Badge>}
                  {item.disabled && <Badge variant="outline">Disabled</Badge>}
                </div>
                <div className="flex items-center gap-2">
                  <Button size="sm" variant="ghost" onClick={() => startEditing(item)}>
                    Edit
                  </Button>
                  {!item.isDefault && !item.disabled && (
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={setDefaultMutation.isPending}
                      onClick={() => setDefaultMutation.mutate(item)}
                    >
                      Set default
                    </Button>
                  )}
                  <DisableToggleButton
                    disabled={item.disabled}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(item)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">{editingFormat ? `Edit ${editingFormat.name}` : "New print format"}</h3>
        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="flex flex-col gap-3">
            <ValidatedTextField control={form.control} name="name" label="Name" placeholder="e.g. Standard" />
            <ValidatedTextareaField
              control={form.control}
              name="templateContent"
              label="Template (Thymeleaf XHTML)"
              rows={12}
              monospace
            />
            <FormField
              control={form.control}
              name="makeDefault"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center gap-2 space-y-0">
                  <FormControl>
                    <input
                      type="checkbox"
                      className="size-4"
                      checked={field.value}
                      onChange={(event) => field.onChange(event.target.checked)}
                    />
                  </FormControl>
                  <FormLabel className="!mt-0">Make this the default for {documentType}</FormLabel>
                </FormItem>
              )}
            />

            <div className="flex flex-col gap-1">
              <label htmlFor="sample-data" className="text-sm font-medium">
                Sample data (JSON) — used for preview only
              </label>
              <textarea
                id="sample-data"
                rows={4}
                className="font-mono text-sm rounded-md border bg-transparent px-3 py-2 shadow-xs outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]"
                value={sampleDataText}
                onChange={(event) => setSampleDataText(event.target.value)}
              />
            </div>

            {(saveMutation.isError || previewError) && (
              <p role="alert" className="text-sm text-destructive">
                {previewError ??
                  (saveMutation.error instanceof ApiError ? saveMutation.error.message : "Could not save the print format.")}
              </p>
            )}

            <div className="flex gap-2">
              <Button type="submit" disabled={saveMutation.isPending} className="self-start">
                {saveMutation.isPending ? "Saving…" : editingFormat ? "Save changes" : "Create"}
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={previewMutation.isPending}
                onClick={form.handleSubmit((values) => previewMutation.mutate(values))}
              >
                {previewMutation.isPending ? "Rendering…" : "Save & preview PDF"}
              </Button>
              {editingFormat && (
                <Button type="button" variant="ghost" onClick={startCreating}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </Form>
      </section>
    </div>
  );
}
