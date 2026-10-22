import AxeBuilder from "@axe-core/playwright";
import { test as base, expect, type APIRequestContext, type BrowserContext, type Page } from "@playwright/test";

/**
 * Shared fixtures for every spec (docs/ui-ux.md §11 "All", §12):
 *
 * - **Console guard** (automatic): any console warning or error, or uncaught page error, in any
 *   page of the test's browser context fails the test. The ONLY exceptions are HTTP errors a test
 *   declares up front with `consoleGuard.allowHttpError(status, urlPattern)` for its error journey
 *   (bad login 401, stale-version 409). Each declared exception must actually happen, so the
 *   allow-list can never hide a journey that silently stopped reaching its error.
 * - **axe** (`expectAccessible(page)`): 0 serious/critical WCAG 2.x A/AA violations on each page
 *   a spec visits.
 * - **api**: the same-origin API (through the edge proxy) with the test's own cookies, used to
 *   arrange data the way the UI would (CSRF header included).
 */

type AllowedHttpError = { status: number; url: RegExp; seen: number };

export type ConsoleGuard = {
  allowHttpError(status: number, url: RegExp): void;
};

export type Api = {
  /** POST /collection {pokedexId}; returns the CollectedPokemonDto (201 or 200). */
  sync(pokedexId: number): Promise<CollectedPokemon>;
  get(pokedexId: number): Promise<CollectedPokemon>;
  /** PUT /collection/{id} with the full custom attributes; returns the updated record. */
  update(pokedexId: number, custom: CustomAttributes, version: number): Promise<CollectedPokemon>;
};

export type CustomAttributes = {
  localizedNames: { lang: string; value: string }[];
  geo: { region?: string; habitat?: string; coordinates?: { latitude: number; longitude: number } | null } | null;
  tags: string[];
};
export type CollectedPokemon = {
  id: number;
  version: number;
  snapshot: { name: string };
  custom: CustomAttributes;
};

const STATUS_IN_TEXT = /status of (\d{3})/;

export const test = base.extend<{ consoleGuard: ConsoleGuard; api: Api }>({
  consoleGuard: [
    async ({ context }, provide, testInfo) => {
      const allowed: AllowedHttpError[] = [];
      const problems: string[] = [];
      watch(context, allowed, problems);

      await provide({ allowHttpError: (status, url) => allowed.push({ status, url, seen: 0 }) });

      // Only judge tests that otherwise passed; a failed test already reports its own error.
      if (testInfo.status !== testInfo.expectedStatus) return;
      expect(problems, "console warnings/errors (ui-ux.md §11: none on any journey)").toEqual([]);
      for (const entry of allowed) {
        expect(entry.seen, `expected HTTP ${entry.status} for ${entry.url} did not happen`).toBeGreaterThan(0);
      }
    },
    { auto: true },
  ],

  api: async ({ page }, provide) => {
    await provide(apiFor(page.request));
  },
});

export { expect };

function watch(context: BrowserContext, allowed: AllowedHttpError[], problems: string[]) {
  context.on("console", (msg) => {
    const type = msg.type();
    if (type !== "error" && type !== "warning") return;
    const text = msg.text();
    const url = msg.location().url;
    const status = Number(STATUS_IN_TEXT.exec(text)?.[1]);
    const match = allowed.find((a) => a.status === status && a.url.test(url));
    if (match) {
      match.seen += 1;
      return;
    }
    problems.push(`[${type}] ${text}${url ? ` (${url})` : ""}`);
  });
  context.on("weberror", (error) => problems.push(`[pageerror] ${error.error().message}`));
}

/** 0 serious/critical axe violations (WCAG 2.0/2.1/2.2 A + AA) on the page as it is now. */
export async function expectAccessible(page: Page) {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
    .analyze();
  const serious = results.violations
    .filter((v) => v.impact === "serious" || v.impact === "critical")
    .map((v) => `${v.id} (${v.impact}): ${v.help} → ${v.nodes.map((n) => n.target.join(" ")).slice(0, 5).join(" | ")}`);
  expect(serious, `axe serious/critical violations on ${page.url()}`).toEqual([]);
}

function apiFor(request: APIRequestContext): Api {
  const csrf = async () => {
    const response = await request.get("/api/v1/auth/csrf");
    expect(response.status(), "GET /auth/csrf").toBe(200);
    const body = (await response.json()) as { headerName: string };
    const cookies = (await request.storageState()).cookies;
    const token = cookies.find((c) => c.name === "XSRF-TOKEN")?.value;
    if (!token) throw new Error("no XSRF-TOKEN cookie after GET /auth/csrf");
    return { [body.headerName]: token };
  };
  return {
    async sync(pokedexId) {
      const response = await request.post("/api/v1/collection", {
        headers: await csrf(),
        data: { pokedexId },
      });
      expect([200, 201], `sync ${pokedexId}: ${await response.text()}`).toContain(response.status());
      return (await response.json()) as CollectedPokemon;
    },
    async get(pokedexId) {
      const response = await request.get(`/api/v1/collection/${pokedexId}`);
      expect(response.status(), `GET /collection/${pokedexId}`).toBe(200);
      return (await response.json()) as CollectedPokemon;
    },
    async update(pokedexId, custom, version) {
      const response = await request.put(`/api/v1/collection/${pokedexId}`, {
        headers: await csrf(),
        data: { customAttributes: custom, version },
      });
      expect(response.status(), `PUT /collection/${pokedexId}: ${await response.text()}`).toBe(200);
      return (await response.json()) as CollectedPokemon;
    },
  };
}

/**
 * A Pokédex id for data-changing tests: outside the demo seed (1, 4, 7, 25, 133, 143) and spread
 * over 152–1000, so parallel tests and repeated runs practically never share a record. The record
 * may already exist from an earlier run; every test works either way (sync is an upsert).
 */
export function freshPokedexId() {
  return 152 + Math.floor(Math.random() * 849);
}

/** A unique, valid tag (`^[a-z0-9-]{1,30}$`). */
export function uniqueTag(prefix: string) {
  return `${prefix}-${Date.now().toString(36)}${Math.floor(Math.random() * 1e4).toString(36)}`.slice(0, 30);
}
