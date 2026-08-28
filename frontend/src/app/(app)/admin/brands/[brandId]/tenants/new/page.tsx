"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm, type FieldPath } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { ApiError } from "@/lib/api/http";
import { onboardTenant } from "@/lib/api/admin-api";

const wizardSchema = z.object({
  tenantName: z.string().min(1, "Tenant name is required"),
  legalName: z.string().min(1, "Legal name is required"),
  registrationNo: z.string().optional(),
  vatNo: z.string().optional(),
  address: z.string().optional(),
  baseCurrency: z
    .string()
    .min(3, "Enter a 3-letter currency code")
    .max(3, "Enter a 3-letter currency code")
    .transform((value) => value.toUpperCase()),
  fiscalYearStartMonth: z.string().min(1, "Select a month"),
  adminEmail: z.string().min(1, "Admin email is required").email("Enter a valid email"),
  entitlementCodes: z.string().optional(),
});

type WizardValues = z.infer<typeof wizardSchema>;

const MONTH_OPTIONS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
].map((label, index) => ({ value: String(index + 1), label }));

interface Step {
  title: string;
  description: string;
  fields: FieldPath<WizardValues>[];
}

const STEPS: Step[] = [
  {
    title: "Company details",
    description: "The Tenant's first Company (MDM-1).",
    fields: ["tenantName", "legalName", "registrationNo", "vatNo", "address"],
  },
  {
    title: "Fiscal year & currency",
    description: "Sets the default accounting period seeded at onboarding.",
    fields: ["baseCurrency", "fiscalYearStartMonth"],
  },
  {
    title: "Initial admin user",
    description: "This person is provisioned as the Tenant Administrator and emailed a temporary password.",
    fields: ["adminEmail"],
  },
  {
    title: "Plan & entitlements",
    description: "Feature codes to enable immediately, bounded by this Brand's own entitlements. Include MOD-LK to seed a Sri Lanka–localised chart of accounts.",
    fields: ["entitlementCodes"],
  },
  {
    title: "Review & create",
    description: "Branches, users beyond the initial admin, and the rest of the setup checklist are configured after the tenant is created.",
    fields: [],
  },
];

interface NewTenantPageProps {
  params: Promise<{ brandId: string }>;
}

function NewTenantWizard({ brandId }: { brandId: string }) {
  const router = useRouter();
  const [step, setStep] = useState(0);

  const form = useForm<WizardValues>({
    resolver: zodResolver(wizardSchema),
    defaultValues: {
      tenantName: "",
      legalName: "",
      registrationNo: "",
      vatNo: "",
      address: "",
      baseCurrency: "LKR",
      fiscalYearStartMonth: "1",
      adminEmail: "",
      entitlementCodes: "",
    },
  });

  const mutation = useMutation({
    mutationFn: (values: WizardValues) =>
      onboardTenant(brandId, {
        tenantName: values.tenantName,
        company: {
          legalName: values.legalName,
          registrationNo: values.registrationNo || undefined,
          vatNo: values.vatNo || undefined,
          address: values.address || undefined,
          baseCurrency: values.baseCurrency,
          fiscalYearStartMonth: Number(values.fiscalYearStartMonth),
        },
        adminEmail: values.adminEmail,
        initialEntitlementFeatureCodes: values.entitlementCodes
          ? values.entitlementCodes.split(/[\s,]+/).map((code) => code.trim().toUpperCase()).filter(Boolean)
          : undefined,
      }),
    onSuccess: (tenant) => {
      router.push(`/admin/brands/${brandId}/tenants/${tenant.id}`);
    },
  });

  const values = form.getValues();
  const current = STEPS[step];
  const isReview = step === STEPS.length - 1;

  async function handleNext() {
    const valid = await form.trigger(current.fields);
    if (valid) {
      setStep((s) => Math.min(s + 1, STEPS.length - 1));
    }
  }

  function handleBack() {
    setStep((s) => Math.max(s - 1, 0));
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <Link href={`/admin/brands/${brandId}`} className="text-xs text-muted-foreground hover:underline">
          ← Brand console
        </Link>
        <h1 className="text-xl font-semibold">Onboard a tenant</h1>
        <p className="text-sm text-muted-foreground">
          Zero-to-first-invoice starts here: company, fiscal year, initial admin, and entitlements in one flow.
        </p>
      </div>

      <ol className="flex flex-wrap gap-2 text-xs">
        {STEPS.map((s, index) => (
          <li
            key={s.title}
            className={
              "rounded-full border px-2.5 py-1 " +
              (index === step
                ? "border-primary bg-primary text-primary-foreground"
                : index < step
                  ? "border-primary text-primary"
                  : "text-muted-foreground")
            }
          >
            {index + 1}. {s.title}
          </li>
        ))}
      </ol>

      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((v) => mutation.mutate(v))}
          className="flex flex-col gap-4 rounded-lg border p-4"
        >
          <div className="flex flex-col gap-1">
            <h2 className="text-sm font-semibold">{current.title}</h2>
            <p className="text-xs text-muted-foreground">{current.description}</p>
          </div>

          {step === 0 && (
            <div className="flex flex-col gap-3">
              <ValidatedTextField control={form.control} name="tenantName" label="Tenant name" />
              <ValidatedTextField control={form.control} name="legalName" label="Company legal name" />
              <ValidatedTextField control={form.control} name="registrationNo" label="Registration no." />
              <ValidatedTextField control={form.control} name="vatNo" label="VAT no." />
              <ValidatedTextField control={form.control} name="address" label="Address" />
            </div>
          )}

          {step === 1 && (
            <div className="flex flex-col gap-3">
              <ValidatedTextField control={form.control} name="baseCurrency" label="Base currency" placeholder="LKR" />
              <ValidatedSelectField
                control={form.control}
                name="fiscalYearStartMonth"
                label="Fiscal year start month"
                options={MONTH_OPTIONS}
              />
            </div>
          )}

          {step === 2 && (
            <div className="flex flex-col gap-3">
              <ValidatedTextField control={form.control} name="adminEmail" label="Admin email" type="email" />
            </div>
          )}

          {step === 3 && (
            <div className="flex flex-col gap-3">
              <ValidatedTextField
                control={form.control}
                name="entitlementCodes"
                label="Feature codes (comma or space separated)"
                placeholder="MOD-LK"
              />
            </div>
          )}

          {isReview && (
            <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-xs text-muted-foreground">Tenant name</dt>
                <dd>{values.tenantName || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Legal name</dt>
                <dd>{values.legalName || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Base currency</dt>
                <dd>{values.baseCurrency || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Fiscal year starts</dt>
                <dd>{MONTH_OPTIONS[Number(values.fiscalYearStartMonth) - 1]?.label ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Initial admin</dt>
                <dd>{values.adminEmail || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Entitlements</dt>
                <dd>{values.entitlementCodes || "None"}</dd>
              </div>
            </dl>
          )}

          {mutation.isError && (
            <p role="alert" className="text-sm text-destructive">
              {mutation.error instanceof ApiError ? mutation.error.message : "Could not onboard the tenant."}
            </p>
          )}

          <div className="flex justify-between">
            <Button type="button" variant="outline" onClick={handleBack} disabled={step === 0}>
              Back
            </Button>
            {isReview ? (
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Creating…" : "Create tenant"}
              </Button>
            ) : (
              <Button type="button" onClick={handleNext}>
                Next
              </Button>
            )}
          </div>
        </form>
      </Form>
    </div>
  );
}

export default function NewTenantPage({ params }: NewTenantPageProps) {
  const { brandId } = use(params);
  return <NewTenantWizard brandId={brandId} />;
}
