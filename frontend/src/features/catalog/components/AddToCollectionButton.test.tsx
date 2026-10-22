import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "@/mocks/node";
import { API, renderWithStore, trainer, withCsrfCookie } from "@/test-utils/render";
import { AddToCollectionButton } from "./AddToCollectionButton";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  usePathname: () => "/pokemon/pikachu",
}));

const toast = vi.hoisted(() => ({ success: vi.fn(), close: vi.fn() }));
vi.mock("@heroui/react", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@heroui/react")>()),
  toast,
}));

const record = {
  id: 25,
  snapshot: { id: 25, name: "pikachu", sprite: "", category: "Mouse Pokémon", mass: 6, abilities: [], stats: [] },
  custom: { localizedNames: [], tags: [] },
  version: 0,
  syncedAt: "2026-10-21T00:00:00Z",
  updatedAt: "2026-10-21T00:00:00Z",
};

beforeEach(() => {
  push.mockClear();
  toast.success.mockClear();
  withCsrfCookie();
});

describe("AddToCollectionButton", () => {
  it("should_send_a_visitor_to_login_and_back_to_this_pokemon", async () => {
    const { user } = renderWithStore(<AddToCollectionButton pokedexId={25} name="Pikachu" />, { signedInAs: null });

    await user.click(await screen.findByRole("button", { name: "Add to collection" }));

    expect(push).toHaveBeenCalledWith("/login?from=%2Fpokemon%2Fpikachu");
  });

  it.each([
    [201, "Pikachu added to your collection"],
    [200, "Refreshed from PokeAPI"],
  ])("should_toast_on_a_%s_sync", async (status, message) => {
    server.use(http.post(`${API}/collection`, () => HttpResponse.json(record, { status })));
    const { user } = renderWithStore(<AddToCollectionButton pokedexId={25} name="Pikachu" />, {
      signedInAs: trainer,
    });

    await user.click(await screen.findByRole("button", { name: "Add to collection" }));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith(message, expect.anything()));
    expect(await screen.findByRole("button", { name: "In collection · Refresh" })).toBeInTheDocument();
  });
});
