"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Form } from "@/components/ui/form";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { addContact, listContacts, updatePartner, type PartnerView } from "@/lib/api/masterdata-partner-api";

const detailSchema = z.object({
  name: z.string().min(1, "Name is required"),
  taxRegistrationNo: z.string().optional(),
  creditLimit: z.string().regex(/^\d*\.?\d*$/, "Must be a number"),
  creditTermsDays: z.string().regex(/^\d+$/, "Must be a whole number"),
  defaultAccountId: z.string().optional(),
  bankName: z.string().optional(),
  bankBranch: z.string().optional(),
  bankAccountNo: z.string().optional(),
  bankSwiftCode: z.string().optional(),
});
type DetailFormValues = z.infer<typeof detailSchema>;

const contactSchema = z.object({
  name: z.string().min(1, "Name is required"),
  designation: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email("Enter a valid email").or(z.literal("")).optional(),
});
type ContactFormValues = z.infer<typeof contactSchema>;

function toFormValues(partner: PartnerView): DetailFormValues {
  return {
    name: partner.name,
    taxRegistrationNo: partner.taxRegistrationNo ?? "",
    creditLimit: String(partner.creditLimit),
    creditTermsDays: String(partner.creditTermsDays),
    defaultAccountId: partner.defaultAccountId ?? "",
    bankName: partner.bankName ?? "",
    bankBranch: partner.bankBranch ?? "",
    bankAccountNo: partner.bankAccountNo ?? "",
    bankSwiftCode: partner.bankSwiftCode ?? "",
  };
}

export interface BusinessPartnerDetailDialogProps {
  companyId: string;
  partner: PartnerView;
  onClose: () => void;
}

/** F0.6.4 / MDM-5: a partner's own detail form (credit terms, default account, bank details) plus its contacts. */
export function BusinessPartnerDetailDialog({ companyId, partner, onClose }: BusinessPartnerDetailDialogProps) {
  const queryClient = useQueryClient();
  const partnersQueryKey = ["masterdata", "business-partners", companyId];
  const contactsQueryKey = ["masterdata", "business-partners", partner.id, "contacts", companyId];

  const { data: contacts } = useQuery({
    queryKey: contactsQueryKey,
    queryFn: () => listContacts(partner.id, companyId),
  });

  const detailForm = useForm<DetailFormValues>({ resolver: zodResolver(detailSchema), defaultValues: toFormValues(partner) });
  const contactForm = useForm<ContactFormValues>({
    resolver: zodResolver(contactSchema),
    defaultValues: { name: "", designation: "", phone: "", email: "" },
  });

  const detailMutation = useMutation({
    mutationFn: (values: DetailFormValues) =>
      updatePartner(partner.id, companyId, {
        name: values.name,
        taxRegistrationNo: values.taxRegistrationNo || undefined,
        creditLimit: Number(values.creditLimit || "0"),
        creditTermsDays: Number(values.creditTermsDays || "0"),
        defaultAccountId: values.defaultAccountId || undefined,
        bankName: values.bankName || undefined,
        bankBranch: values.bankBranch || undefined,
        bankAccountNo: values.bankAccountNo || undefined,
        bankSwiftCode: values.bankSwiftCode || undefined,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: partnersQueryKey }),
  });

  const contactMutation = useMutation({
    mutationFn: (values: ContactFormValues) =>
      addContact(partner.id, companyId, {
        name: values.name,
        designation: values.designation || undefined,
        phone: values.phone || undefined,
        email: values.email || undefined,
        primaryContact: (contacts ?? []).length === 0,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: contactsQueryKey });
      contactForm.reset({ name: "", designation: "", phone: "", email: "" });
    },
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>
            {partner.code} — {partner.name}
          </DialogTitle>
        </DialogHeader>

        <Form {...detailForm}>
          <form onSubmit={detailForm.handleSubmit((values) => detailMutation.mutate(values))} className="flex flex-col gap-3">
            <ValidatedTextField control={detailForm.control} name="name" label="Name" />
            <ValidatedTextField control={detailForm.control} name="taxRegistrationNo" label="Tax registration no." />
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <ValidatedTextField control={detailForm.control} name="creditLimit" label="Credit limit" />
              <ValidatedTextField control={detailForm.control} name="creditTermsDays" label="Credit terms (days)" type="number" />
            </div>
            <ValidatedTextField control={detailForm.control} name="defaultAccountId" label="Default account ID" />
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <ValidatedTextField control={detailForm.control} name="bankName" label="Bank name" />
              <ValidatedTextField control={detailForm.control} name="bankBranch" label="Bank branch" />
              <ValidatedTextField control={detailForm.control} name="bankAccountNo" label="Bank account no." />
              <ValidatedTextField control={detailForm.control} name="bankSwiftCode" label="SWIFT code" />
            </div>

            {detailMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {detailMutation.error instanceof ApiError ? detailMutation.error.message : "Could not save these details."}
              </p>
            )}

            <Button type="submit" disabled={detailMutation.isPending} className="self-start">
              {detailMutation.isPending ? "Saving…" : "Save details"}
            </Button>
          </form>
        </Form>

        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="text-sm font-semibold">Contacts</h3>
          {contacts && contacts.length === 0 && <p className="text-sm text-muted-foreground">No contacts yet.</p>}
          {contacts && contacts.length > 0 && (
            <ul className="flex flex-col gap-2">
              {contacts.map((contact) => (
                <li key={contact.id} className="rounded-md border px-3 py-2 text-sm">
                  <span className="font-medium">{contact.name}</span>
                  {contact.primaryContact && <span className="ml-1.5 text-xs text-muted-foreground">(primary)</span>}
                  <div className="text-xs text-muted-foreground">
                    {[contact.designation, contact.phone, contact.email].filter(Boolean).join(" · ")}
                  </div>
                </li>
              ))}
            </ul>
          )}

          <Form {...contactForm}>
            <form
              onSubmit={contactForm.handleSubmit((values) => contactMutation.mutate(values))}
              className="flex flex-col gap-3"
            >
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <ValidatedTextField control={contactForm.control} name="name" label="Contact name" />
                <ValidatedTextField control={contactForm.control} name="designation" label="Designation" />
                <ValidatedTextField control={contactForm.control} name="phone" label="Phone" />
                <ValidatedTextField control={contactForm.control} name="email" label="Email" type="email" />
              </div>

              {contactMutation.isError && (
                <p role="alert" className="text-sm text-destructive">
                  {contactMutation.error instanceof ApiError ? contactMutation.error.message : "Could not add this contact."}
                </p>
              )}

              <Button type="submit" variant="outline" disabled={contactMutation.isPending} className="self-start">
                {contactMutation.isPending ? "Adding…" : "Add contact"}
              </Button>
            </form>
          </Form>
        </div>
      </DialogContent>
    </Dialog>
  );
}
