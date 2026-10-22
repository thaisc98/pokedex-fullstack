import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "src"),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      include: ["src/**"],
      exclude: [
        // Tests and test helpers are not the code under test.
        "src/**/*.test.{ts,tsx}",
        "src/mocks/**",
        // Generated from docs/api/openapi.yaml, and other type-only modules (no runtime code).
        "src/**/*.d.ts",
        "src/shared/api/types.ts",
        "src/features/catalog/types.ts",
        // Next.js route files (pages, layouts, loading/error/not-found) and proxy.ts run inside
        // the Next.js server/runtime (async Server Components, headers(), redirects). Vitest +
        // jsdom can't render them; the Playwright suite (frontend/e2e) covers every route.
        "src/app/**",
        "src/proxy.ts",
        // server-only fetch client used by those Server Components; also covered end to end.
        "src/server/**",
      ],
      reporter: ["text-summary", "text", "html"],
      reportsDirectory: "./coverage",
    },
  },
});
