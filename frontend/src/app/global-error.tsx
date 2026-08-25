"use client";

import { useEffect } from "react";

// Catches errors thrown by the root layout itself (Providers, fonts, etc.),
// so it must render its own <html>/<body> — it replaces the whole tree,
// including src/app/layout.tsx, when triggered.
export default function GlobalError({
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
    <html lang="en">
      <body>
        <div
          style={{
            display: "flex",
            minHeight: "100dvh",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            gap: "0.75rem",
            padding: "1rem",
            textAlign: "center",
            fontFamily: "sans-serif",
          }}
        >
          <h1>Something went wrong</h1>
          <p>The application failed to load. You can try again.</p>
          <button onClick={reset}>Try again</button>
        </div>
      </body>
    </html>
  );
}
