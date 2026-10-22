import { expect, expectAccessible, test } from "./support/fixtures";

/** US02 (docs/ui-ux.md §11 rows 3–4): Eevee's 8 branches as links; friendly not-found page. */
const EEVEELUTIONS = [
  ["Vaporeon", 134],
  ["Jolteon", 135],
  ["Flareon", 136],
  ["Espeon", 196],
  ["Umbreon", 197],
  ["Leafeon", 470],
  ["Glaceon", 471],
  ["Sylveon", 700],
] as const;

test.describe("US02 detail", () => {
  test("shows all 8 Eevee branches, each stage a link", async ({ page }) => {
    await page.goto("/pokemon/eevee");

    await expect(page.getByRole("heading", { level: 1, name: "Eevee" })).toBeVisible();
    const chain = page.getByRole("list", { name: "Evolution chain" });
    await expect(chain.getByRole("link")).toHaveCount(1 + EEVEELUTIONS.length);
    await expect(chain.getByRole("link", { name: /Eevee/ })).toHaveAttribute("href", "/pokemon/133");
    for (const [name, id] of EEVEELUTIONS) {
      await expect(chain.getByRole("link", { name: new RegExp(name) })).toHaveAttribute("href", `/pokemon/${id}`);
    }
    await expectAccessible(page);
  });

  test("shows a friendly not-found page with a link back to the catalog", async ({ page, consoleGuard }) => {
    // The document itself is the 404 (Next.js notFound()); Chromium logs that one response.
    consoleGuard.allowHttpError(404, /\/pokemon\/does-not-exist$/);

    const response = await page.goto("/pokemon/does-not-exist");

    expect(response?.status()).toBe(404);
    await expect(page.getByRole("heading", { level: 1, name: "We couldn’t find that Pokémon" })).toBeVisible();
    await expectAccessible(page);
    await page.getByRole("link", { name: "Back to catalog" }).click();
    await expect(page).toHaveURL(/\/pokemon$/);
    await expect(page.getByRole("heading", { level: 1, name: "Pokémon" })).toBeVisible();
  });
});
