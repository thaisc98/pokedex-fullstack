import { USER_STATE } from "./support/env";
import { expect, expectAccessible, freshPokedexId, test, uniqueTag } from "./support/fixtures";

/** US04 (docs/ui-ux.md §11 rows 7–8, §4 US04 flow). */
test.use({ storageState: USER_STATE });

test.describe("US04 edit custom attributes", () => {
  test("latitude 91 → the error summary takes focus and the field shows the error", async ({ page, api }) => {
    const id = freshPokedexId();
    await api.sync(id);

    await page.goto(`/collection/${id}?edit=1`);
    await expect(page.getByRole("heading", { level: 2, name: /^Edit / })).toBeVisible();
    await page.getByLabel("Latitude").fill("91");
    await page.getByLabel("Longitude").fill("10");
    await page.getByRole("button", { name: "Save changes" }).click();

    const summary = page.getByText("There is 1 problem with this form");
    await expect(summary).toBeVisible();
    await expect(page.locator(":focus")).toContainText("There is 1 problem with this form");
    await expect(page.getByRole("link", { name: "Latitude must be between -90 and 90" })).toBeVisible();
    await expect(page.getByLabel("Latitude")).toHaveAttribute("aria-invalid", "true");
    await expectAccessible(page);
  });

  test("stale version → conflict dialog → Reload latest shows the current data", async ({ page, api, consoleGuard }) => {
    const id = freshPokedexId();
    const record = await api.sync(id);
    // The 409 is the journey: the browser logs that one failed PUT.
    consoleGuard.allowHttpError(409, new RegExp(`/api/v1/collection/${id}$`));

    await page.goto(`/collection/${id}?edit=1`);
    await expect(page.getByText(`Version ${record.version}`)).toBeVisible();

    // Someone else saves first, in a second page (same team dataset, security.md §3.1).
    const other = await page.context().newPage();
    const theirRegion = uniqueTag("theirs");
    await other.goto(`/collection/${id}?edit=1`);
    await other.getByLabel("Region").fill(theirRegion);
    await other.getByRole("button", { name: "Save changes" }).click();
    await expect(other.getByText("Saved")).toBeVisible();
    await other.close();

    await page.getByLabel("Region").fill("mine");
    await page.getByRole("button", { name: "Save changes" }).click();

    const dialog = page.getByRole("alertdialog", { name: "Someone else updated this record" });
    await expect(dialog).toBeVisible();
    await expectAccessible(page);
    await dialog.getByRole("button", { name: "Reload latest" }).click();

    await expect(dialog).toBeHidden();
    await expect(page.getByLabel("Region")).toHaveValue(theirRegion);
    await expect(page.getByText(`Version ${record.version + 1}`)).toBeVisible();
  });
});
