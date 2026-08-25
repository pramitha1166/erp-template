"use client";

import { useEffect } from "react";

import { Button } from "@/components/ui/button";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-col items-start gap-3 py-16">
      <h2 className="text-lg font-semibold">Something went wrong</h2>
      <p className="text-sm text-muted-foreground">
        This section couldn&apos;t be displayed. You can try again, or go
        back to the dashboard.
      </p>
      <Button onClick={reset}>Try again</Button>
    </div>
  );
}
