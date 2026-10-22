import { ADMIN_STATE, USER_STATE } from "./support/env";
import { expect, expectAccessible, freshPokedexId, test } from "./support/fixtures";

/** Auth row of docs/ui-ux.md §11 (BR-10) plus the bad-login error journey (security.md §4.1). */
test.describe("Auth", () => {
  test.describe("as USER", () => {
    test.use({ storageState: USER_STATE });

    test("does not show the Delete action", async ({ page }) => {
      await page.goto("/collection/25"); // Pikachu, from the demo seed

      await expect(page.getByRole("heading", { level: 1, name: "Pikachu" })).toBeVisible();
      await expect(page.getByRole("button", { name: "Edit" })).toBeVisible();
      await expect(page.getByRole("button", { name: "Delete" })).toHaveCount(0);
      await expectAccessible(page);
    });
  });

  test.describe("as ADMIN", () => {
    test.use({ storageState: ADMIN_STATE });

    test("delete asks for confirmation and the record disappears", async ({ page, api }) => {
      const id = freshPokedexId();
      const record = await api.sync(id);
      const name = record.snapshot.name;

      await page.goto(`/collection/${id}`);
      await page.getByRole("button", { name: "Delete" }).click();

      const dialog = page.getByRole("alertdialog");
      await expect(dialog.getByRole("heading", { name: /^Delete .+ from the collection\?$/ })).toBeVisible();
      await expectAccessible(page);
      await dialog.getByRole("button", { name: "Delete" }).click();

      await expect(page).toHaveURL(/\/collection$/);
      await page.goto(`/collection?q=${encodeURIComponent(name)}`);
      await expect(page.getByRole("heading", { name: "No matching Pokémon" })).toBeVisible();
    });
  });

  test("a wrong password gets one generic message", async ({ page, consoleGuard }) => {
    // The 401 is the journey. A never-registered email has its own rate-limit bucket.
    consoleGuard.allowHttpError(401, /\/api\/v1\/auth\/login$/);
    const email = `nobody-${Date.now()}@example.com`;

    await page.goto("/login");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password").fill("Wrong-Password-123");
    await page.getByRole("button", { name: "Log in" }).click();

    const message = page.getByRole("alert");
    await expect(message).toBeVisible();
    await expect(message).toBeFocused();
    await expect(page).toHaveURL(/\/login$/);
    await expectAccessible(page);
  });
});
