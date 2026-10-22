import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright acceptance scenarios (docs/ui-ux.md §11, §12). Owned by the test engineer.
 *
 * Runs against a running stack, by default the Docker Compose stack through the nginx edge proxy
 * (production build): `docker compose up -d --build --wait`, then `npm run test:e2e`.
 * Point it elsewhere with E2E_BASE_URL.
 *
 * - Three viewport projects (ui-ux.md §6.3 breakpoints): 375×812, 768×1024, 1280×800.
 * - A `setup` project logs the seeded demo accounts in once and saves their sessions, so the
 *   per-IP+email login limit (security.md §4.1: 5/min) is never hit by the suite.
 * - No retries: a flaky test must fail, not be hidden.
 */
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  workers: process.env.CI ? 2 : 4,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : [["list"]],
  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "setup", testMatch: /global\.setup\.ts/ },
    {
      name: "mobile-375",
      use: { ...devices["Desktop Chrome"], viewport: { width: 375, height: 812 } },
      dependencies: ["setup"],
    },
    {
      name: "tablet-768",
      use: { ...devices["Desktop Chrome"], viewport: { width: 768, height: 1024 } },
      dependencies: ["setup"],
    },
    {
      name: "desktop-1280",
      use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } },
      dependencies: ["setup"],
    },
  ],
});
