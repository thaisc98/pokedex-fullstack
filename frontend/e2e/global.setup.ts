import { existsSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { request, type Page } from "@playwright/test";
import {
  accounts,
  ADMIN_STATE,
  AUTH_DIR,
  US03_MARKER,
  US03_PASSWORD,
  us03Email,
  USER_STATE,
  VIEWPORT_PROJECTS,
} from "./support/env";
import { expect, test as setup } from "./support/fixtures";

/**
 * Signs the seeded demo USER and ADMIN in once through the real login page and saves their sessions
 * (cookies incl. the `pokedex_signed_in` hint). A saved session is reused while it is younger than
 * 50 minutes (tokens live 60) and `GET /auth/me` still accepts it, so repeated runs don't spend the
 * per-IP+email login budget (security.md §4.1). Also makes sure the US03 journey accounts exist.
 */
const MAX_AGE_MS = 50 * 60 * 1000;

// The journey-account step reads the USER session saved by the first step.
setup.describe.configure({ mode: "serial" });

setup.beforeAll(() => {
  mkdirSync(AUTH_DIR, { recursive: true });
});

setup("sign in the demo USER", async ({ page, baseURL }) => {
  await ensureSession(page, baseURL!, USER_STATE, accounts.user);
});

setup("sign in the demo ADMIN", async ({ page, baseURL }) => {
  await ensureSession(page, baseURL!, ADMIN_STATE, accounts.admin);
});

setup("register the US03 journey accounts", async ({ baseURL }) => {
  const api = await request.newContext({ baseURL });
  try {
    // A reset database gets a new demo user id: then the marker is stale and we register again.
    const userApi = await request.newContext({ baseURL, storageState: USER_STATE });
    const me = (await (await userApi.get("/api/v1/auth/me")).json()) as { id: string };
    await userApi.dispose();
    const marker = existsSync(US03_MARKER) ? (JSON.parse(readFileSync(US03_MARKER, "utf8")) as { demoUserId?: string }) : {};
    if (marker.demoUserId === me.id) return;

    for (const project of VIEWPORT_PROJECTS) {
      await api.get("/api/v1/auth/csrf");
      const token = (await api.storageState()).cookies.find((c) => c.name === "XSRF-TOKEN")?.value ?? "";
      const response = await api.post("/api/v1/auth/register", {
        headers: { "X-XSRF-TOKEN": token },
        data: { email: us03Email(project), password: US03_PASSWORD, displayName: `E2E ${project}` },
      });
      // 409: it exists from an earlier run on this database.
      expect([201, 409], `register ${us03Email(project)}: ${await response.text()}`).toContain(response.status());
    }
    writeFileSync(US03_MARKER, JSON.stringify({ demoUserId: me.id }));
  } finally {
    await api.dispose();
  }
});

async function ensureSession(
  page: Page,
  baseURL: string,
  statePath: string,
  account: { email: string; password: string },
) {
  if (existsSync(statePath) && Date.now() - statSync(statePath).mtimeMs < MAX_AGE_MS) {
    const saved = await request.newContext({ baseURL, storageState: statePath });
    const me = await saved.get("/api/v1/auth/me");
    await saved.dispose();
    if (me.status() === 200) return;
  }
  await page.goto("/login");
  await page.getByLabel("Email").fill(account.email);
  await page.getByLabel("Password").fill(account.password);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).not.toHaveURL(/\/login/);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await page.context().storageState({ path: statePath });
}
