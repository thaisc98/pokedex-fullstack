import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "@/mocks/node";
import type { CollectedPokemon } from "@/shared/api/types";
import { API, renderWithStore, trainer, withCsrfCookie } from "@/test-utils/render";
import { EditForm } from "./EditForm";

const item: CollectedPokemon = {
  id: 25,
  snapshot: { id: 25, name: "pikachu", sprite: "", category: "Mouse Pokémon", mass: 6, abilities: [], stats: [] },
  custom: { localizedNames: [], geo: { region: "Kanto" }, tags: [] },
  version: 3,
  syncedAt: "2026-10-21T00:00:00Z",
  updatedAt: "2026-10-21T00:00:00Z",
};

function renderForm() {
  const props = {
    onDone: vi.fn(),
    onCancel: vi.fn(),
    onReloadLatest: vi.fn(() => Promise.resolve()),
    onRemoved: vi.fn(),
  };
  const rendered = renderWithStore(<EditForm item={item} {...props} />, { signedInAs: trainer });
  return { ...rendered, props };
}

beforeEach(() => withCsrfCookie());

describe("EditForm", () => {
  it("should_focus_the_error_summary_and_mark_the_field_for_latitude_91", async () => {
    // No PUT handler: client-side validation must stop the request (MSW fails on unhandled requests).
    const { user } = renderForm();

    await user.type(document.getElementById("edit-latitude")!, "91");
    await user.type(document.getElementById("edit-longitude")!, "10");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    const summary = await screen.findByText("There is 1 problem with this form");
    await waitFor(() => expect(summary.closest("[tabindex='-1']")).toHaveFocus());
    expect(screen.getByRole("link", { name: "Latitude must be between -90 and 90" })).toHaveAttribute(
      "href",
      "#edit-latitude",
    );
    expect(document.getElementById("edit-latitude")).toHaveAttribute("aria-invalid", "true");
  });

  it("should_open_the_conflict_dialog_on_409_and_reload_the_latest_version", async () => {
    let sentVersion: unknown;
    server.use(
      http.put(`${API}/collection/25`, async ({ request }) => {
        sentVersion = ((await request.json()) as { version: number }).version;
        return HttpResponse.json(
          { type: "/problems/version-conflict", title: "Conflict", status: 409 },
          { status: 409 },
        );
      }),
    );
    const { user, props } = renderForm();

    await user.click(screen.getByRole("button", { name: "Save changes" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent("Someone else updated this record");
    expect(sentVersion).toBe(3);

    await user.click(screen.getByRole("button", { name: "Reload latest" }));
    expect(props.onReloadLatest).toHaveBeenCalledTimes(1);
    expect(props.onDone).not.toHaveBeenCalled();
  });
});
