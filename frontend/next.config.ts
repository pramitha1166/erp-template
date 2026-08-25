import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Standalone output keeps the Docker runtime image to a minimal
  // node_modules subset (NFR-D1 — single docker compose deployment).
  output: "standalone",
};

export default nextConfig;
