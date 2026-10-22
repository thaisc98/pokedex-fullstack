import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      // security.md §5: dangerouslySetInnerHTML is banned.
      "react/no-danger": "error",
      // Zod is configured jitless in one module (no `new Function` probe under our CSP).
      "no-restricted-imports": [
        "error",
        { paths: [{ name: "zod", message: 'Import { z } from "@/shared/lib/zod" (jitless config).' }] },
      ],
    },
  },
  {
    files: ["src/shared/lib/zod.ts"],
    rules: { "no-restricted-imports": "off" },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // Generated from docs/api/openapi.yaml by `npm run gen:api`.
    "src/shared/api/schema.d.ts",
    "coverage/**",
  ]),
]);

export default eslintConfig;
