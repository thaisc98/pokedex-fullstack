import { expect, expectAccessible, test } from "./support/fixtures";

/**
 * US01 (docs/ui-ux.md §11 rows 1–2, §6.3 grid): cards per row by viewport, card content,
 * keyboard pagination, Back returns to page 1 at the same scroll position.
 */
const COLUMNS: Record<string, number> = { "mobile-375": 2, "tablet-768": 3, "desktop-1280": 5 };

test.describe("US01 catalog", () => {
  test("shows the grid with the viewport's columns and complete cards", async ({ page }, testInfo) => {
    await page.goto("/pokemon");

    await expect(page.getByRole("heading", { level: 1, name: "Pokémon" })).toBeVisible();
    // Direct children only: each card also contains an abilities list.
    const cards = page.getByRole("main").getByRole("list").first().locator(":scope > li");
    await expect(cards).toHaveCount(20);

    // Columns = cards sharing the first card's row.
    const tops = await cards.evaluateAll((items) => items.map((item) => Math.round(item.getBoundingClientRect().top)));
    expect(tops.filter((top) => top === tops[0]).length, "cards in the first row").toBe(COLUMNS[testInfo.project.name]);

    // Sprite, number, name, category, mass and abilities (Bulbasaur, the first entry).
    const first = cards.first();
    // The sprite is decorative (alt=""), so it has no img role; check the element itself.
    await expect(first.locator("img")).toBeVisible();
    await expect(first).toContainText("#001");
    await expect(first).toContainText("Bulbasaur");
    await expect(first).toContainText("Seed Pokémon");
    await expect(first).toContainText("6.9 kg");
    await expect(first).toContainText(/Overgrow/i);
    await expectAccessible(page);
  });

  test("paginates with the keyboard and Back returns to page 1 at the same scroll position", async ({ page }) => {
    await page.goto("/pokemon");
    const next = page.getByLabel("Next page");
    await next.scrollIntoViewIfNeeded();
    const scrollBefore = await page.evaluate(() => window.scrollY);
    expect(scrollBefore, "the pagination is below the fold").toBeGreaterThan(0);

    await next.focus();
    await page.keyboard.press("Enter");

    await expect(page).toHaveURL(/\/pokemon\?page=2$/);
    await expect(page.getByText(/Showing 21–40 of/)).toBeVisible();
    await expectAccessible(page);

    await page.goBack();

    await expect(page).toHaveURL(/\/pokemon$/);
    await expect(page.getByText(/Showing 1–20 of/)).toBeVisible();
    // "Same scroll position" = back where the user was, not at the top. Allow a few pixels for
    // font/layout rounding differences between machines (CI rendered 664 px vs 668 px before).
    const tolerance = 16;
    expect(scrollBefore, "the test needs a scroll position well away from the top").toBeGreaterThan(tolerance * 4);
    await expect
      .poll(() => page.evaluate(() => window.scrollY), { message: "scroll position restored" })
      .toBeGreaterThanOrEqual(scrollBefore - tolerance);
    expect(await page.evaluate(() => window.scrollY)).toBeLessThanOrEqual(scrollBefore + tolerance);
  });
});
