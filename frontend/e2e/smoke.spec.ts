import { USER_STATE } from "./support/env";
import { expect, expectAccessible, test } from "./support/fixtures";

/**
 * Smoke of every page (docs/ui-ux.md §3.1): it renders its h1, the console stays clean and axe
 * finds no serious/critical violations. Journeys live in the other specs.
 */
test.describe("Smoke: public pages", () => {
  test("/ redirects to the catalog", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveURL(/\/pokemon$/);
    await expect(page.getByRole("heading", { level: 1, name: "Pokémon" })).toBeVisible();
  });

  test("detail page", async ({ page }) => {
    await page.goto("/pokemon/pikachu");
    await expect(page.getByRole("heading", { level: 1, name: "Pikachu" })).toBeVisible();
    await expect(page.getByRole("img", { name: "Pikachu official artwork" })).toBeVisible();
    await expectAccessible(page);
  });

  test("log in page", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
    await expectAccessible(page);
  });

  test("register page", async ({ page }) => {
    await page.goto("/register");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await expect(page.getByLabel("Display name")).toBeVisible();
    await expectAccessible(page);
  });

  test("signed out, /collection redirects to log in", async ({ page }) => {
    await page.goto("/collection");
    await expect(page).toHaveURL(/\/login\?from=%2Fcollection$/);
  });

  test("unknown route shows the 404 page", async ({ page, consoleGuard }) => {
    consoleGuard.allowHttpError(404, /\/no-such-page$/);
    const response = await page.goto("/no-such-page");
    expect(response?.status()).toBe(404);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await expectAccessible(page);
  });
});

test.describe("Smoke: signed-in pages", () => {
  test.use({ storageState: USER_STATE });

  test("collection list", async ({ page }) => {
    await page.goto("/collection");
    await expect(page.getByRole("heading", { level: 1, name: "My collection" })).toBeVisible();
    await expect(page.getByText(/\d+ records?/)).toBeVisible();
    await expectAccessible(page);
  });

  test("collected Pokémon read view", async ({ page }) => {
    await page.goto("/collection/25");
    await expect(page.getByRole("heading", { level: 1, name: "Pikachu" })).toBeVisible();
    await expect(page.getByRole("heading", { level: 2, name: "Our notes" })).toBeVisible();
    await expectAccessible(page);
  });
});
