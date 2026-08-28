"use client";

import { useState } from "react";

import { ImpersonationBanner } from "@/components/admin/impersonation-banner";
import { Header } from "./header";
import { Nav } from "./nav";

/**
 * F0.0.5: nav frame + header, responsive from 360px (NFR-U1). The sidebar
 * is an off-canvas drawer below the `lg` breakpoint and a static column
 * above it.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const [navOpen, setNavOpen] = useState(false);

  return (
    <div className="flex min-h-dvh flex-col">
      <ImpersonationBanner />
      <Header onToggleNav={() => setNavOpen((open) => !open)} />
      <div className="flex flex-1">
        <Nav open={navOpen} onClose={() => setNavOpen(false)} />
        <main className="min-w-0 flex-1 p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
