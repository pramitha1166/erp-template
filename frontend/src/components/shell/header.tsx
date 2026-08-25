"use client";

import { Menu } from "lucide-react";

import { Button } from "@/components/ui/button";
import { CompanySwitcher } from "./company-switcher";
import { SessionMenu } from "./session-menu";

interface HeaderProps {
  onToggleNav: () => void;
}

export function Header({ onToggleNav }: HeaderProps) {
  return (
    <header className="sticky top-0 z-50 flex h-14 items-center gap-3 border-b bg-background px-3 sm:px-4">
      <Button
        variant="ghost"
        size="icon"
        className="lg:hidden"
        aria-label="Toggle navigation"
        onClick={onToggleNav}
      >
        <Menu />
      </Button>
      {/* BRD-2: generic placeholder — brand name/logo come from Brand
          config once Epic 0.8/F0.8 theming lands. */}
      <span className="truncate text-sm font-semibold">ERP Platform</span>
      <div className="ml-auto flex items-center gap-1 text-sm text-muted-foreground">
        <CompanySwitcher />
        <SessionMenu />
      </div>
    </header>
  );
}
