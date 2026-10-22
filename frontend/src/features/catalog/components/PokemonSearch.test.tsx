import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "@/mocks/node";
import { API, renderWithStore } from "@/test-utils/render";
import { exactMatchHref, normalizeSearch } from "../lib/search";
import { PokemonSearch } from "./PokemonSearch";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

/** MSW handler matching the agreed `GET /api/v1/pokemon/search?q=&limit=10` contract. */
function searchHandler(calls: string[]) {
  return http.get(`${API}/pokemon/search`, ({ request }) => {
    const url = new URL(request.url);
    const q = url.searchParams.get("q") ?? "";
    calls.push(q);
    const all = [
      { id: 25, name: "pikachu" },
      { id: 26, name: "raichu" },
      { id: 172, name: "pichu" },
      { id: 122, name: "mr-mime" },
    ];
    return HttpResponse.json({ results: all.filter((p) => p.name.includes(q)).slice(0, Number(url.searchParams.get("limit"))) });
  });
}

beforeEach(() => push.mockClear());

describe("normalizeSearch / exactMatchHref", () => {
  it("should_trim_lowercase_and_hyphenate", () => {
    expect(normalizeSearch("  Mr Mime ")).toBe("mr-mime");
    expect(normalizeSearch("pika;drop")).toBeNull();
    expect(normalizeSearch("   ")).toBeNull();
  });

  it("should_go_to_a_number_or_an_exact_name_only", () => {
    const results = [{ id: 25, name: "pikachu" }];
    expect(exactMatchHref("25", [])).toBe("/pokemon/25");
    expect(exactMatchHref("Pikachu", results)).toBe("/pokemon/25");
    expect(exactMatchHref("pika", results)).toBeNull();
  });
});

describe("PokemonSearch", () => {
  it("should_debounce_typing_into_one_request_and_show_numbered_suggestions", async () => {
    const calls: string[] = [];
    server.use(searchHandler(calls));
    const { user } = renderWithStore(<PokemonSearch />);

    await user.type(screen.getByRole("combobox", { name: "Find a Pokémon" }), "pich");

    expect(await screen.findByRole("option", { name: "#172 Pichu" })).toBeInTheDocument();
    expect(calls).toEqual(["pich"]);
  });

  it("should_open_the_pokemon_picked_with_the_keyboard", async () => {
    server.use(searchHandler([]));
    const { user } = renderWithStore(<PokemonSearch />);
    const input = screen.getByRole("combobox", { name: "Find a Pokémon" });

    await user.type(input, "chu");
    await screen.findByRole("option", { name: "#025 Pikachu" });
    await user.keyboard("{ArrowDown}{Enter}");

    await waitFor(() => expect(push).toHaveBeenCalledWith("/pokemon/25"));
  });

  it("should_go_to_an_exact_name_on_enter", async () => {
    server.use(searchHandler([]));
    const { user } = renderWithStore(<PokemonSearch />);
    const input = screen.getByRole("combobox", { name: "Find a Pokémon" });

    await user.type(input, "Raichu");
    await screen.findByRole("option", { name: "#026 Raichu" });
    await user.keyboard("{Enter}");

    expect(push).toHaveBeenCalledWith("/pokemon/26");
  });

  it("should_say_when_nothing_matches", async () => {
    server.use(searchHandler([]));
    const { user } = renderWithStore(<PokemonSearch />);

    await user.type(screen.getByRole("combobox", { name: "Find a Pokémon" }), "zzz");

    expect(await screen.findByText("No Pokémon match")).toBeInTheDocument();
  });

  it("should_explain_a_503_inline", async () => {
    server.use(
      http.get(`${API}/pokemon/search`, () =>
        HttpResponse.json(
          { type: "/problems/upstream-unavailable", title: "Unavailable", status: 503 },
          { status: 503 },
        ),
      ),
    );
    const { user } = renderWithStore(<PokemonSearch />);

    await user.type(screen.getByRole("combobox", { name: "Find a Pokémon" }), "pika");

    expect(await screen.findByText(/Search is unavailable right now\. Please try again/)).toHaveAttribute(
      "role",
      "status",
    );
  });
});
