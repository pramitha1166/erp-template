import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";
import "@testing-library/jest-dom/vitest";

// Testing Library's own auto-cleanup only registers when it finds a global
// `afterEach` (Jest's default); Vitest doesn't put one on globalThis unless
// `test.globals` is enabled, so wire it up explicitly instead.
afterEach(() => {
  cleanup();
});
