import { US03_PASSWORD, us03Email, USER_STATE } from "./support/env";
import { expect, expectAccessible, ownedPokedexId, test, uniqueTag } from "./support/fixtures";

/** US03 (docs/ui-ux.md §11 rows 5–6, §4 US03 flow). */
test.describe("US03 add to collection", () => {
  test("signed out: Add to collection → log in → back on the same Pokémon", async ({ page }, testInfo) => {
    await page.goto("/pokemon/pikachu");
    await expect(page.getByRole("heading", { level: 1, name: "Pikachu" })).toBeVisible();

    await page.getByRole("button", { name: "Add to collection" }).click();

    await expect(page).toHaveURL(/\/login\?from=%2Fpokemon%2Fpikachu$/);
    await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
    await expectAccessible(page);
    // One account per viewport project: one login per email per run (security.md §4.1 rate limit).
    await page.getByLabel("Email").fill(us03Email(testInfo.project.name));
    await page.getByLabel("Password").fill(US03_PASSWORD);
    await page.getByRole("button", { name: "Log in" }).click();

    await expect(page).toHaveURL(/\/pokemon\/pikachu$/);
    await expect(page.getByRole("heading", { level: 1, name: "Pikachu" })).toBeVisible();
    await expect(page.getByRole("button", { name: "Add to collection" })).toBeEnabled();
  });

  test.describe("signed in", () => {
    test.use({ storageState: USER_STATE });

    test("re-sync keeps the custom fields and says so in a toast", async ({ page, api }, testInfo) => {
      const id = ownedPokedexId("resync", testInfo.project.name);
      const tag = uniqueTag("kept");
      const before = await api.sync(id);
      const custom = {
        localizedNames: [{ lang: "ja", value: "テスト" }],
        geo: { region: "Kanto", habitat: "e2e-meadow", coordinates: null },
        tags: [tag],
      };
      const updated = await api.update(id, custom, before.version);

      await page.goto(`/pokemon/${id}`);
      await page.getByRole("button", { name: "Add to collection" }).click();

      const toast = page.getByText("Refreshed from PokeAPI");
      await expect(toast).toBeVisible();
      await expect(page.getByText("Your custom fields were kept.")).toBeVisible();
      await expect(page.getByRole("button", { name: /In collection · Refresh/ })).toBeVisible();
      await expectAccessible(page);

      const after = await api.get(id);
      expect(after.custom, "BR-05: custom attributes untouched by the re-sync").toEqual(updated.custom);
      await page.goto(`/collection/${id}`);
      await expect(page.getByText(tag)).toBeVisible();
      await expect(page.getByText("e2e-meadow")).toBeVisible();
    });
  });
});
