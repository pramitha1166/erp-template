import { DelegationPanel } from "@/components/workflow/delegation-panel";

/** F0.4.5 / WF-5: self-service approval delegation, alongside the account's other self-service security screens. */
export default function DelegationsPage() {
  return (
    <div className="flex max-w-md flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-xl font-semibold">Approval delegation</h1>
        <p className="text-sm text-muted-foreground">
          Hand off your pending approvals to someone else for a fixed date range.
        </p>
      </div>
      <DelegationPanel />
    </div>
  );
}
