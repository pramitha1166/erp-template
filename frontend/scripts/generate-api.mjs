#!/usr/bin/env node
// F0.0.2: regenerates src/lib/api/schema.d.ts from the backend's published
// OpenAPI spec. Run `mvn -Popenapi verify -DskipTests` in backend/ first
// (produces backend/target/openapi.yaml), or point OPENAPI_SPEC at a
// running backend instead, e.g.:
//   OPENAPI_SPEC=http://localhost:8080/v3/api-docs.yaml npm run generate:api
import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";

const spec =
  process.env.OPENAPI_SPEC ??
  path.resolve(process.cwd(), "../backend/target/openapi.yaml");

const isUrl = /^https?:\/\//.test(spec);

if (!isUrl && !existsSync(spec)) {
  console.error(
    `OpenAPI spec not found at ${spec}.\n` +
      `Run "mvn -Popenapi verify -DskipTests" in backend/ first, or set ` +
      `OPENAPI_SPEC to a reachable spec URL (e.g. ` +
      `http://localhost:8080/v3/api-docs.yaml).`,
  );
  process.exit(1);
}

const out = path.resolve(process.cwd(), "src/lib/api/schema.d.ts");

execFileSync("npx", ["--yes", "openapi-typescript", spec, "-o", out], {
  stdio: "inherit",
});
